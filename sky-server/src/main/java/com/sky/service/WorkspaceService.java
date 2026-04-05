package com.sky.service;

import com.sky.vo.BusinessDataVO;
import com.sky.vo.DishOverViewVO;
import com.sky.vo.OrderOverViewVO;
import com.sky.vo.SetmealOverViewVO;
import java.time.LocalDate;
import java.util.Map;

public interface WorkspaceService {

    /**
     * 根据时间段统计营业数据
     * @param begin
     * @param end
     * @return
     */
    BusinessDataVO getBusinessData(LocalDate begin, LocalDate end);

    /**
     * 批量查询时间区间内每天的营业数据（用于报表导出明细，避免N+1问题）
     * @param begin 开始日期（含）
     * @param end 结束日期（含）
     * @return key=日期, value=当天营业数据
     */
    Map<LocalDate, BusinessDataVO> getBusinessDataList(LocalDate begin, LocalDate end);

    /**
     * 查询订单管理数据
     * @return
     */
    OrderOverViewVO getOrderOverView();

    /**
     * 查询菜品总览
     * @return
     */
    DishOverViewVO getDishOverView();

    /**
     * 查询套餐总览
     * @return
     */
    SetmealOverViewVO getSetmealOverView();

}
