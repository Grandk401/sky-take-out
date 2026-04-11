package com.sky.service;


public interface ShopService {

    /**
     * 查询店铺状态
     * @return
     */
    Integer getStatus();

    /**
     * 设置店铺状态
     * @param status
     */
    void setStatus(Integer status);

}
