package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.mapper.UserMapper;
import com.sky.result.PageResult;
import com.sky.service.AddressBookService;
import com.sky.service.OrderService;
import com.sky.service.ShoppingCartService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private WeChatPayUtil weChatPayUtil;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Override
    @Transactional
    public OrderSubmitVO submit(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常（地址簿或者购物车为空）
        AddressBook addressBook = addressBookService.getAddressBookById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }
        List<ShoppingCart> shoppingCarts = shoppingCartService.showShoppingCart();
        if (shoppingCarts == null || shoppingCarts.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }
        //向订单表插入数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setUserId(BaseContext.getCurrentId());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setOrderTime(LocalDateTime.now());
        orders.setNumber(String.valueOf(System.currentTimeMillis()));//使用时间戳作为订单号
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName()
                + addressBook.getCityName()
                + addressBook.getDistrictName()
                + addressBook.getDetail());
        orderMapper.insert(orders);
        //向订单明细表插入数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart shoppingCart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(shoppingCart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置订单明细关联的订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);
        //清空该用户的购物车
        shoppingCartService.clear();
        //封装VO并返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();

        return orderSubmitVO;
    }


    /**
     * 分页查询历史订单
     * @param page
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public PageResult pageQuery4User(int page, int pageSize, Integer status){
        // 分页查询订单
        PageHelper.startPage(page, pageSize);
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setStatus(status);
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        Page<Orders> pageOrders = orderMapper.pageQuery(ordersPageQueryDTO);

        if(pageOrders == null || pageOrders.getTotal() == 0){
            return new PageResult(0, new ArrayList());
        }

        // 批量查询本页所有订单的明细
        List<Long> orderIds = new ArrayList<>();
        for (Orders orders : pageOrders) {
            orderIds.add(orders.getId());
        }
        List<OrderDetail> allDetails = orderDetailMapper.getByOrderIds(orderIds);

        // 在内存中按 orderId 分组
        Map<Long, List<OrderDetail>> detailMap = allDetails.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        // 组装 VO
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : pageOrders) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            orderVO.setOrderDetailList(detailMap.getOrDefault(orders.getId(), new ArrayList<>()));
            orderVOList.add(orderVO);
        }

        return new PageResult(pageOrders.getTotal(), orderVOList);
    }

    /**
     * 根据订单id查询订单
     * @param id
     * @return
     */
    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        //查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }

    /**
     * 用户取消订单
     * @param id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id) throws Exception  {
        Orders ordersDB = orderMapper.getById(id);
        //判断订单是否存在
        if(ordersDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //验证订单是否属于当前用户
        if(!ordersDB.getUserId().equals(BaseContext.getCurrentId())){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_YOUR);
        }
        //判断订单状态，只允许取消待付款和待接单状态的订单 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if(ordersDB.getStatus() > Orders.TO_BE_CONFIRMED){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orders = new Orders();
        orders.setId(ordersDB.getId());
        //若订单支付状态为已支付，调用微信退款接口
        if(ordersDB.getPayStatus().equals(Orders.PAID)){
//            weChatPayUtil.refund(
//                    ordersDB.getNumber(),//商户订单号
//                    ordersDB.getNumber(),//退款订单号
//                    new BigDecimal(0.01),//退款金额，单位 元
//                    new BigDecimal(0.01));//原订单金额，单位 元
            //修改订单状态为已退款
            orders.setPayStatus(Orders.REFUND);
        }
        //修改订单状态为已取消, 并更新取消时间、取消原因
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason("用户取消订单");
        orderMapper.update(orders);
    }

    /**
     * 用户再来一单
     * @param id
     */
    @Override
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }

    /**
     * 订单搜索
     * @param ordersPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        //查询订单
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);
        if (page == null || page.isEmpty()) {
            return new PageResult(0, new ArrayList<>());
        }
        //收集订单ID进行批量查询
        List<Long> orderIds = new ArrayList<>();
        for (Orders orders : page) {
            orderIds.add(orders.getId());
        }

        //批量查询订单明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderIds(orderIds);

        //将订单明细按订单ID分组
        Map<Long, List<OrderDetail>> orderDetailMap = orderDetailList.stream()
                .collect(Collectors.groupingBy(OrderDetail::getOrderId));

        //构建VO对象集合
        List<OrderVO> orderVOList = new ArrayList<>();
        for (Orders orders : page) {
            OrderVO orderVO = new OrderVO();
            BeanUtils.copyProperties(orders, orderVO);
            //从Map中获取对应的订单明细
            orderVO.setOrderDetailList(orderDetailMap.getOrDefault(orders.getId(), new ArrayList<>()));
            orderVOList.add(orderVO);
        }

        //将VO对象中的订单菜品信息转换为字符串，添加到VO对象中
        orderVOList.forEach(x -> {
            x.setOrderDishes(x.getOrderDetailList().stream().map(y -> y.getName() + " * " + y.getNumber()).collect(Collectors.joining("; ")));
        });

        //用集合封装VO返回
        return new PageResult(page.getTotal(), orderVOList);
    }

    /**
     * 统计订单状态数量
     * @return
     */
    @Override
    public OrderStatisticsVO statistics() {
        //根据订单状态查询订单数量
        Integer TO_BE_CONFIRMED = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer CONFIRMED = orderMapper.countStatus(Orders.CONFIRMED);
        Integer DELIVERY_IN_PROGRESS = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);

        //封装VO对象
        OrderStatisticsVO vo = new OrderStatisticsVO();
        vo.setToBeConfirmed(TO_BE_CONFIRMED);
        vo.setConfirmed(CONFIRMED);
        vo.setDeliveryInProgress(DELIVERY_IN_PROGRESS);
        return vo;
    }

    /**
     * 商家接单
     * @param ordersConfirmDTO
     */
    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //更新订单状态为已接单
        Orders ordersDB = orderMapper.getById(ordersConfirmDTO.getId());
        //非空校验
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //校验订单状态是否为待接单
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException("订单状态不是待接单");
        }
        //更新订单状态，只更新状态字段
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CONFIRMED)
                .build();

        orderMapper.update(orders);
    }

    /**
     * 商家拒单
     * @param ordersRejectionDTO
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        //从数据库中查询订单状态
        Orders ordersDB = orderMapper.getById(ordersRejectionDTO.getId());
        //非空校验
        if (ordersDB == null) {
            throw new OrderBusinessException("订单不存在");
        }
        //校验订单状态是否为待接单
        if (!Orders.TO_BE_CONFIRMED.equals(ordersDB.getStatus())) {
            throw new OrderBusinessException("订单状态不是待接单");
        }
        //如果已支付，调用微信退款接口，并更新支付状态为已退款，订单状态为已取消
        if (Orders.PAID.equals(ordersDB.getPayStatus())) {
//            //调用微信退款接口
//            String refundResult = weChatPayUtil.refund(
//                    ordersDB.getNumber(), //商户订单号
//                    ordersDB.getNumber(), //退款订单号
//                    new BigDecimal(0.01), //订单金额，单位 元
//                    new BigDecimal(0.01));//退款金额，单位 元
              log.info("商家拒单，订单号：{}", ordersDB.getNumber());
        }

        //更新订单状态，只更新必须的字段
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.REFUND)
                .cancelReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    /**
     * 商家取消订单
     * @param ordersCancelDTO
     */
    @Override
    public void adminCancel(OrdersCancelDTO ordersCancelDTO) throws Exception {
       //从数据库中查询订单状态
       Orders ordersDB = orderMapper.getById(ordersCancelDTO.getId());
       //非空校验
       if (ordersDB == null) {
           throw new OrderBusinessException("订单不存在");
       }

        //构造订单实体类
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
       //如果订单已支付，调用微信退款接口
       if (Orders.PAID.equals(ordersDB.getPayStatus())) {
//           //调用微信退款接口
//           String refundResult = weChatPayUtil.refund(
//                   ordersDB.getNumber(), //商户订单号
//                   ordersDB.getNumber(), //退款订单号
//                   new BigDecimal(0.01), //订单金额，单位 元
//                   new BigDecimal(0.01));//退款金额，单位 元
           //设置支付状态为已退款
           orders.setPayStatus(Orders.REFUND);
           log.info("商家取消订单，订单号：{}", ordersDB.getNumber());
       }
       //更新订单状态，只更新必须的字段
       orderMapper.update(orders);
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

//        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");
        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString( "package"));

        //为替代微信支付成功后的数据库订单状态更新， 多定义一个方法进行修改
        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付
        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新
        LocalDateTime check_out_time = LocalDateTime.now();

        //获取订单号码
        String orderNumber = ordersPaymentDTO.getOrderNumber();

        log.info("调用updateStatus，用于替换微信支付更新数据库状态的问题");
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderNumber);

        return vo;
//        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
//        vo.setPackageStr(jsonObject.getString("package"));
//
//        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }
}
