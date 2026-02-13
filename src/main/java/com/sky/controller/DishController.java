package com.sky.controller;

import com.sky.common.ResponseResult;
import com.sky.entity.Dish;
import com.sky.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜品控制器
 * 提供前端接口调用的增删改查接口
 */
@RestController
@RequestMapping("/dish")
public class DishController {
    private static final Logger logger = LoggerFactory.getLogger(DishController.class);

    private final DishService dishService;

    /**
     * 构造器注入
     */
    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    /**
     * 新增菜品：POST请求，接收前端传的菜品数据
     */
    @PostMapping
    public ResponseResult<Void> save(@RequestBody Dish dish) {
        logger.info("开始新增菜品：{}", dish.getName());
        try {
            dishService.save(dish);
            logger.info("新增菜品成功：{}", dish.getName());
            return ResponseResult.success();
        } catch (RuntimeException e) {
            logger.error("新增菜品失败：{}", e.getMessage(), e);
            return ResponseResult.error("新增失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID删除菜品：DELETE请求，路径传ID
     */
    @DeleteMapping("/{id}")
    public ResponseResult<Void> deleteById(@PathVariable Long id) {
        logger.info("开始删除菜品，ID：{}", id);
        try {
            dishService.deleteById(id);
            logger.info("删除菜品成功，ID：{}", id);
            return ResponseResult.success();
        } catch (RuntimeException e) {
            logger.error("删除菜品失败，ID：{}，原因：{}", id, e.getMessage(), e);
            return ResponseResult.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询菜品：GET请求，路径传ID
     */
    @GetMapping("/{id}")
    public ResponseResult<Dish> getById(@PathVariable Long id) {
        logger.info("开始查询菜品，ID：{}", id);
        try {
            Dish dish = dishService.getById(id);
            logger.info("查询菜品成功，ID：{}", id);
            return ResponseResult.success(dish);
        } catch (RuntimeException e) {
            logger.error("查询菜品失败，ID：{}，原因：{}", id, e.getMessage(), e);
            return ResponseResult.error("查询失败：" + e.getMessage());
        }
    }

    /**
     * 修改菜品：PUT请求，接收前端传的修改后数据
     */
    @PutMapping
    public ResponseResult<Void> update(@RequestBody Dish dish) {
        logger.info("开始修改菜品，ID：{}", dish.getId());
        try {
            dishService.update(dish);
            logger.info("修改菜品成功，ID：{}", dish.getId());
            return ResponseResult.success();
        } catch (RuntimeException e) {
            logger.error("修改菜品失败，ID：{}，原因：{}", dish.getId(), e.getMessage(), e);
            return ResponseResult.error("修改失败：" + e.getMessage());
        }
    }

    /**
     * 根据分类ID+状态查询菜品列表：GET请求，参数传categoryId和status
     */
    @GetMapping("/list")
    public ResponseResult<List<Dish>> listByCategoryIdAndStatus(
            @RequestParam Long categoryId,
            @RequestParam Integer status
    ) {
        logger.info("开始查询菜品列表，分类ID：{}，状态：{}", categoryId, status);
        try {
            List<Dish> dishes = dishService.listByCategoryIdAndStatus(categoryId, status);
            logger.info("查询菜品列表成功，分类ID：{}，状态：{}，数量：{}", categoryId, status, dishes.size());
            return ResponseResult.success(dishes);
        } catch (RuntimeException e) {
            logger.error("查询菜品列表失败，分类ID：{}，状态：{}，原因：{}", categoryId, status, e.getMessage(), e);
            return ResponseResult.error("查询失败：" + e.getMessage());
        }
    }
}