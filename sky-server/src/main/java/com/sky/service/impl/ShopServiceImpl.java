package com.sky.service.impl;
import com.sky.constant.RedisCacheConstant;
import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    //注入RedisTemplate对象
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 查询店铺状态
     * @return
     */
    @Override
    public Integer getStatus() {
        //从Redis中查询店铺状态
        Integer status = (Integer) redisTemplate.opsForValue().get(RedisCacheConstant.SHOP_STATUS_KEY);
        if (status == null) {
            status = 1; // 默认营业中
            //默认营业中，写入Redis，有效期24小时
            redisTemplate.opsForValue().set(RedisCacheConstant.SHOP_STATUS_KEY, status, RedisCacheConstant.SHOP_STATUS_TTL_SECONDS, TimeUnit.SECONDS);
        }
        return status;
    }

    /**
     * 设置店铺状态
     * @param status
     */
    @Override
    public void setStatus(Integer status) {
        //设置店铺状态到Redis中，有效期24小时
        redisTemplate.opsForValue().set(RedisCacheConstant.SHOP_STATUS_KEY, status, RedisCacheConstant.SHOP_STATUS_TTL_SECONDS, TimeUnit.SECONDS);
    }

}
