package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.mapper.DishMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.WorkspaceService;
import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WorkspaceServiceImpl implements WorkspaceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    public BusinessDataVO getBusinessData(LocalDate begin, LocalDate end) {
        /**
         * 营业额：当日已完成订单的总金额
         * 有效订单：当日已完成订单的数量
         * 订单完成率：有效订单数 / 总订单数
         * 平均客单价：营业额 / 有效订单数
         * 新增用户：当日新增用户的数量
         */

        Map map = new HashMap();
        map.put("begin", begin);
        map.put("end", end.plusDays(1));

        //查询总订单数
        Integer totalOrderCount = orderMapper.countByMap(map);

        //一次SQL同时返回营业额和有效订单数
        map.put("status", Orders.COMPLETED);
        Map<String, Object> businessData = orderMapper.sumAndCountByMap(map);
        Double turnover = ((Number) businessData.get("turnover")).doubleValue();
        Integer validOrderCount = ((Number) businessData.get("validOrderCount")).intValue();

        Double unitPrice = 0.0;

        Double orderCompletionRate = 0.0;
        if(totalOrderCount != 0 && validOrderCount != 0){
            //订单完成率
            orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
            //平均客单价
            unitPrice = turnover / validOrderCount;
        }

        Map userMap = new HashMap();
        userMap.put("begin", begin);
        userMap.put("end", end.plusDays(1));
        Integer newUsers = userMapper.countByMap(userMap);

        return BusinessDataVO.builder()
                .turnover(turnover)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .unitPrice(unitPrice)
                .newUsers(newUsers)
                .build();
    }

    /**
     * 批量查询时间区间内每天的营业数据
     * 复用已有的 GROUP BY 批量查询方法，在内存中按天组装并计算派生字段
     */
    public Map<LocalDate, BusinessDataVO> getBusinessDataList(LocalDate begin, LocalDate end) {
        // 查询每天的总订单数 + 有效订单数
        List<Map<String, Object>> orderDailyList = orderMapper.countByDateRange(
                begin, end.plusDays(1), Orders.COMPLETED);
        Map<String, Integer> totalOrderMap = new HashMap<>();
        Map<String, Integer> validOrderMap = new HashMap<>();
        for (Map<String, Object> item : orderDailyList) {
            String date = item.get("date").toString();
            totalOrderMap.put(date, ((Number) item.get("totalOrderCount")).intValue());
            validOrderMap.put(date, ((Number) item.get("validOrderCount")).intValue());
        }

        // 查询每天的营业额
        List<Map<String, Object>> turnoverList = orderMapper.sumTurnoverByDateRange(
                begin, end.plusDays(1), Orders.COMPLETED);
        Map<String, Double> turnoverMap = new HashMap<>();
        for (Map<String, Object> item : turnoverList) {
            turnoverMap.put(item.get("date").toString(), ((Number) item.get("turnover")).doubleValue());
        }

        // 查询每天的新增用户数
        List<Map<String, Object>> userList = userMapper.countByDateRange(begin, end.plusDays(1));
        Map<String, Integer> newUserMap = new HashMap<>();
        for (Map<String, Object> item : userList) {
            newUserMap.put(item.get("date").toString(), ((Number) item.get("newUserCount")).intValue());
        }

        // 按日期顺序组装结果，并在内存中计算派生字段
        Map<LocalDate, BusinessDataVO> result = new LinkedHashMap<>();
        LocalDate temp = begin;
        while (!temp.isAfter(end)) {
            String dateStr = temp.toString();
            int totalOrderCount = totalOrderMap.getOrDefault(dateStr, 0);
            int validOrderCount = validOrderMap.getOrDefault(dateStr, 0);
            double turnover = turnoverMap.getOrDefault(dateStr, 0.0);
            int newUsers = newUserMap.getOrDefault(dateStr, 0);

            double unitPrice = 0.0;
            double orderCompletionRate = 0.0;
            if (totalOrderCount != 0 && validOrderCount != 0) {
                orderCompletionRate = (double) validOrderCount / totalOrderCount;
                unitPrice = turnover / validOrderCount;
            }

            result.put(temp, BusinessDataVO.builder()
                    .turnover(turnover)
                    .validOrderCount(validOrderCount)
                    .orderCompletionRate(orderCompletionRate)
                    .unitPrice(unitPrice)
                    .newUsers(newUsers)
                    .build());

            temp = temp.plusDays(1);
        }
        return result;
    }


    /**
     * 查询订单管理数据
     *
     * @return
     */
    public OrderOverViewVO getOrderOverView() {
        // 一次聚合SQL返回全部状态订单数
        Map map = new HashMap();
        map.put("begin", LocalDate.now());
        Map<String, Object> overViewData = orderMapper.countOverViewByMap(map);

        return OrderOverViewVO.builder()
                .waitingOrders(((Number) overViewData.get("waitingOrders")).intValue())
                .deliveredOrders(((Number) overViewData.get("deliveredOrders")).intValue())
                .completedOrders(((Number) overViewData.get("completedOrders")).intValue())
                .cancelledOrders(((Number) overViewData.get("cancelledOrders")).intValue())
                .allOrders(((Number) overViewData.get("allOrders")).intValue())
                .build();
    }

    /**
     * 查询菜品总览
     *
     * @return
     */
    public DishOverViewVO getDishOverView() {
        // 一次聚合SQL返回启售/停售菜品数量
        Map<String, Object> overViewData = dishMapper.countOverViewByMap();

        return DishOverViewVO.builder()
                .sold(((Number) overViewData.get("sold")).intValue())
                .discontinued(((Number) overViewData.get("discontinued")).intValue())
                .build();
    }

    /**
     * 查询套餐总览
     *
     * @return
     */
    public SetmealOverViewVO getSetmealOverView() {
        // 一次聚合SQL返回启售/停售套餐数量
        Map<String, Object> overViewData = setmealMapper.countOverViewByMap();

        return SetmealOverViewVO.builder()
                .sold(((Number) overViewData.get("sold")).intValue())
                .discontinued(((Number) overViewData.get("discontinued")).intValue())
                .build();
    }
}
