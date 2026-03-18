package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;


@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Slf4j
@Api(tags = "店铺相关接口")
public class ShopController {
    //注入RedisTemplate对象
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    //设置店铺状态
    @PutMapping("/{status}")
    @ApiOperation("设置店铺状态")
    public Result setShopStatus(@PathVariable(value = "status") Integer status) {
        log.info("设置店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        //设置店铺状态到Redis中
        redisTemplate.opsForValue().set("shop_status", status);
        return Result.success();
    }

    //查询店铺状态
    @GetMapping("/status")
    @ApiOperation("查询店铺状态")
    public Result<Integer> getShopStatus() {
        log.info("查询店铺状态");
        //从Redis中查询店铺状态
        Integer status = (Integer) redisTemplate.opsForValue().get("shop_status");
        log.info("查询店铺状态为：{}", status == 1 ? "营业中" : "打烊中");
        return Result.success(status);
    }

}
