package com.sky.service;

import com.sky.entity.Dish;

import java.util.List;

/**
 * 菜品服务接口
 */
public interface DishService {
    /**
     * 新增菜品
     * @param dish 菜品信息
     */
    void save(Dish dish);

    /**
     * 根据ID删除菜品
     * @param id 菜品ID
     */
    void deleteById(Long id);

    /**
     * 根据ID修改菜品
     * @param dish 菜品信息
     */
    void update(Dish dish);

    /**
     * 根据ID查询菜品
     * @param id 菜品ID
     * @return 菜品信息
     */
    Dish getById(Long id);

    /**
     * 根据分类ID和状态查询菜品列表
     * @param categoryId 分类ID
     * @param status 状态（0：停售；1：起售）
     * @return 菜品列表
     */
    List<Dish> listByCategoryIdAndStatus(Long categoryId, Integer status);
}
