package com.sky.controller;

import com.sky.entity.Dish;
import com.sky.service.DishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/*
    菜品控制器
    提供前端接口调用的增删改查接口
 */
@RestController
@RequestMapping("/dish")
public class DishController {
    @Autowired
    private DishService dishService;

    // 新增菜品：POST请求，接收前端传的菜品数据
    @PostMapping
    public String save(@RequestBody Dish dish) {
        try {
            dishService.save(dish);
            return "新增菜品成功";
        } catch (RuntimeException e) {
            // 捕获Service层的异常，返回友好提示
            return "新增失败：" + e.getMessage();
        }
    }

    // 根据ID删除菜品：DELETE请求，路径传ID
    @DeleteMapping("/{id}")
    public String deleteById(@PathVariable Long id) {
        try {
            dishService.deleteById(id);
            return "删除菜品成功";
        } catch (RuntimeException e) {
            return "删除失败：" + e.getMessage();
        }
    }

    // 根据ID查询菜品：GET请求，路径传ID
    @GetMapping("/{id}")
    public Dish getById(@PathVariable Long id) {
        try {
            return dishService.getById(id);
        } catch (RuntimeException e) {
            // 实际项目中建议返回统一响应对象，这里简化
            throw new RuntimeException("查询失败：" + e.getMessage());
        }
    }

    // 修改菜品：PUT请求，接收前端传的修改后数据
    @PutMapping
    public String update(@RequestBody Dish dish) {
        try {
            dishService.update(dish);
            return "修改菜品成功";
        } catch (RuntimeException e) {
            return "修改失败：" + e.getMessage();
        }
    }

    // 根据分类ID+状态查询菜品列表：GET请求，参数传categoryId和status
    @GetMapping("/list")
    public List<Dish> listByCategoryIdAndStatus(
            @RequestParam Long categoryId,
            @RequestParam Integer status
    ) {
        try {
            return dishService.listByCategoryIdAndStatus(categoryId, status);
        } catch (RuntimeException e) {
            throw new RuntimeException("查询列表失败：" + e.getMessage());
        }
    }
}
