package com.sky.service.impl;

import com.sky.entity.Orders;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.vo.TurnoverReportVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表业务实现
 */
@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;

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
        String dateStr = StringUtils.join(dateList, ",");

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
        String turnoverDataStr = StringUtils.join(turnoverData, ",");

        TurnoverReportVO turnoverReportVO = TurnoverReportVO.builder().
                dateList(dateStr).
                turnoverList(turnoverDataStr).
                build();
        return turnoverReportVO;
    }
}
