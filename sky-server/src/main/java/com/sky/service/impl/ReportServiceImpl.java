package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.UserMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.BusinessDataVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;

/**
 * 报表业务实现
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @Override
    public TurnoverReportVO turnoverStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new OrderBusinessException("起止时间不能为空");
        }
        if (end.isBefore(begin)) {
            throw new OrderBusinessException("结束时间不能早于开始时间");
        }

        // 获取时间区间内的所有日期
        List<LocalDate> dateList = new ArrayList<>();
        LocalDate temp = begin;
        while (!temp.isAfter(end)) {
            dateList.add(temp);
            temp = temp.plusDays(1);
        }

        // 查询时间区间内的所有订单
        List<Map<String, Object>> turnoverList = orderMapper.sumTurnoverByDateRange(
                begin, end.plusDays(1), Orders.COMPLETED);
        Map<String, BigDecimal> turnoverMap = turnoverList.stream().collect(Collectors.toMap(
                item -> item.get("date").toString(),
                item -> new BigDecimal(item.get("turnover").toString())
        ));
        log.info("营业额统计查询到 {} 条日期记录", turnoverList.size());

        //按照dateList中的日期顺序返回数据，保证数据顺序一致；如果某天没有数据，则返回0
        List<BigDecimal> turnoverData = new ArrayList<>();
        dateList.forEach(date -> {
            turnoverData.add(turnoverMap.getOrDefault(date.toString(), BigDecimal.ZERO));
        });

        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder().
                dateList(StringUtils.join(dateList, ",")).
                turnoverList(StringUtils.join(turnoverData, ",")).
                build();
        return turnoverReportVO;
    }

    /**
     * 用户统计
     */
    @Override
    public UserReportVO userStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new OrderBusinessException("起止时间不能为空");
        }
        if (end.isBefore(begin)) {
            throw new OrderBusinessException("结束时间不能早于开始时间");
        }

        // 构造日期列表
        List<String> dateStrList = new ArrayList<>();
        LocalDate temp = begin;
        while (!temp.isAfter(end)) {
            dateStrList.add(temp.toString());
            temp = temp.plusDays(1);
        }

        // 查询每日新增用户
        List<Map<String, Object>> userList = userMapper.countByDateRange(
                begin, end.plusDays(1));
        Map<String, Integer> newUserMap = userList.stream().collect(Collectors.toMap(
                item -> item.get("date").toString(),
                item -> ((Number) item.get("newUserCount")).intValue()
        ));
        log.info("用户统计查询到 {} 条日期记录", userList.size());

        // 按日期顺序组装，同时累计计算用户总量
        List<Integer> newUserListData = new ArrayList<>();
        List<Integer> totalUserListData = new ArrayList<>();
        int totalCount = 0;   // 累计器，用于计算用户总量

        for (String date : dateStrList) {
            int newUserCount = newUserMap.getOrDefault(date, 0);
            totalCount += newUserCount;              // 累加：截止今天的总用户数
            newUserListData.add(newUserCount);      // 当天新增
            totalUserListData.add(totalCount); // 截止今天累计总量
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateStrList, ","))
                .newUserList(StringUtils.join(newUserListData, ","))       // ← 改掉
                .totalUserList(StringUtils.join(totalUserListData, ","))   // ← 改掉
                .build();
    }

    /**
     * 订单统计
     */
    @Override
    public OrderReportVO orderStatistics(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new OrderBusinessException("起止时间不能为空");
        }
        if (end.isBefore(begin)) {
            throw new OrderBusinessException("结束时间不能早于开始时间");
        }

        // 构造日期列表
        List<String> dateStrList = new ArrayList<>();
        LocalDate temp = begin;
        while (!temp.isAfter(end)) {
            dateStrList.add(temp.toString());
            temp = temp.plusDays(1);
        }

        // 单次SQL查询，同时获取每日订单总数和有效订单数
        List<Map<String, Object>> orderList = orderMapper.countByDateRange(
                begin, end.plusDays(1), Orders.COMPLETED);
        Map<String, Integer> totalOrderMap = orderList.stream().collect(Collectors.toMap(
                item -> item.get("date").toString(),
                item -> ((Number) item.get("totalOrderCount")).intValue()
        ));
        Map<String, Integer> validOrderMap = orderList.stream().collect(Collectors.toMap(
                item -> item.get("date").toString(),
                item -> ((Number) item.get("validOrderCount")).intValue()
        ));
        log.info("订单统计查询到 {} 条日期记录", orderList.size());

        // 按日期顺序组装数据，同时累计计算总和
        List<Integer> totalOrderData = new ArrayList<>();
        List<Integer> validOrderData = new ArrayList<>();
        int totalOrderSum = 0;
        int validOrderSum = 0;

        for (String date : dateStrList) {
            int total = totalOrderMap.getOrDefault(date, 0);
            int valid = validOrderMap.getOrDefault(date, 0);
            totalOrderData.add(total);
            validOrderData.add(valid);
            totalOrderSum += total;
            validOrderSum += valid;
        }

        // 订单完成率（返回小数比例0~1，前端ECharts负责转百分比，勿*100）
        double rate = totalOrderSum > 0 ? (validOrderSum * 1.0 / totalOrderSum) : 0.0;

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateStrList, ","))
                .orderCountList(StringUtils.join(totalOrderData, ","))
                .validOrderCountList(StringUtils.join(validOrderData, ","))
                .totalOrderCount(totalOrderSum)
                .validOrderCount(validOrderSum)
                .orderCompletionRate(rate)
                .build();
    }

    /**
     * 销量排名Top10
     * @param begin
     * @param end
     * @return
     */
    @Override
    public SalesTop10ReportVO getSalesTop10(LocalDate begin, LocalDate end) {
        if (begin == null || end == null) {
            throw new OrderBusinessException("起止时间不能为空");
        }
        if (end.isBefore(begin)) {
            throw new OrderBusinessException("结束时间不能早于开始时间");
        }

        // 查询销量排名Top10数据（SQL已按销量降序+limit 10）
        List<Map<String, Object>> salesList = orderMapper.getSalesTop10(begin, end.plusDays(1));
        log.info("销量Top10查询到 {} 条记录", salesList.size());

        // 提取商品名称列表和销量列表
        List<String> nameList = new ArrayList<>();
        List<Integer> numberList = new ArrayList<>();
        for (Map<String, Object> item : salesList) {
            nameList.add(item.get("name").toString());
            numberList.add(((Number) item.get("number")).intValue());
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(nameList, ","))
                .numberList(StringUtils.join(numberList, ","))
                .build();
    }


    /**
     * 导出运营数据报表
     * @param response
     */
    public void exportBusinessData(HttpServletResponse response) {
        // 查询数据库，获取营业数据---查询最近30天的运营数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);

        // 查询概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(dateBegin, dateEnd);

        // 通过POI将数据写入到Excel文件中
        InputStream in = this.getClass().getClassLoader().getResourceAsStream("template/运营数据报表模板.xlsx");

        try {
            // 基于模板文件创建一个新的Excel文件
            XSSFWorkbook excel = new XSSFWorkbook(in);

            // 获取表格文件的Sheet页
            XSSFSheet sheet = excel.getSheet("Sheet1");

            // 填充数据--时间
            getOrCreateCell(sheet.getRow(1), 1).setCellValue("时间：" + dateBegin + "至" + dateEnd);

            // 获得第4行，填充概览数据--营业额、订单完成率、新增用户数
            XSSFRow row = sheet.getRow(3);
            getOrCreateCell(row, 2).setCellValue(businessDataVO.getTurnover());
            getOrCreateCell(row, 4).setCellValue(businessDataVO.getOrderCompletionRate());
            getOrCreateCell(row, 6).setCellValue(businessDataVO.getNewUsers().doubleValue());

            // 获得第5行，填充概览数据--有效订单数和单价
            row = sheet.getRow(4);
            getOrCreateCell(row, 2).setCellValue(businessDataVO.getValidOrderCount().doubleValue());
            getOrCreateCell(row, 4).setCellValue(businessDataVO.getUnitPrice());

            // 填充明细数据
            Map<LocalDate, BusinessDataVO> businessDataMap = workspaceService.getBusinessDataList(dateBegin, dateEnd);
            for (int i = 0; i < 30; i++) {
                LocalDate date = dateBegin.plusDays(i);
                // 从Map中直接取当天数据
                BusinessDataVO businessData = businessDataMap.get(date);

                // 获得某一行
                row = sheet.getRow(7 + i);
                getOrCreateCell(row, 1).setCellValue(date.toString());
                getOrCreateCell(row, 2).setCellValue(businessData.getTurnover());
                getOrCreateCell(row, 3).setCellValue(businessData.getValidOrderCount().doubleValue());
                getOrCreateCell(row, 4).setCellValue(businessData.getOrderCompletionRate());
                getOrCreateCell(row, 5).setCellValue(businessData.getUnitPrice());
                getOrCreateCell(row, 6).setCellValue(businessData.getNewUsers().doubleValue());
            }

            // 通过输出流将Excel文件下载到客户端浏览器
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String filename = URLEncoder.encode("运营数据报表.xlsx", "UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filename);
            
            ServletOutputStream out = response.getOutputStream();
            excel.write(out);

            // 关闭资源
            out.close();
            excel.close();
            in.close();
        } catch (IOException e) {
            log.error("导出营业数据失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取或创建单元格，防止NPE
     */
    private Cell getOrCreateCell(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        return cell;
    }
}
