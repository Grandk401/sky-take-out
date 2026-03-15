package com.sky.controller.admin;

import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishServcie;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品管理
 */
@RestController
@RequestMapping("/admin/dish")
@Api(tags = "菜品管理")
@Slf4j
public class DishController {

    @Autowired
    private DishServcie dishService;
    /**
     * 新增菜品
     * @param dishDTO
     * @return
     */
    @ApiOperation("新增菜品")
    @PostMapping
    public Result<?> save(@RequestBody DishDTO dishDTO){
        log.info("新增菜品：{}", dishDTO);
        // 调用service层新增菜品方法
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }
    /**
     * 分页查询菜品
     * @param dishPageQueryDTO
     * @return
     */
    @GetMapping("/page")
    @ApiOperation("分页查询菜品")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO){
        log.info("分页查询菜品：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }
    /**
     * 删除菜品
     * @param ids 菜品id列表
     * @return
     */
    @DeleteMapping
    @ApiOperation("删除菜品")
    public Result delete(@RequestParam("ids") List<Long> ids){
        log.info("删除菜品：{}", ids);
        // 调用service层删除菜品方法
        dishService.deleteBatch(ids);
        return Result.success();
    }
}
