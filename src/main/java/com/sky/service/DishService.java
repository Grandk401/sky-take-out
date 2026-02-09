package com.sky.service;

import com.sky.entity.Dish;

import java.util.List;

public interface DishService {
    //新增菜品
    void save(Dish dish);
    //根据id删除
    void deleteById(Long id);
    //根据id修改
    void update(Dish dish);
    //根据id查询
    Dish getById(Long id);
    //根据分类id查询菜品
    List<Dish> listByCategoryIdAndStatus(Long categoryid, Integer status);
}
