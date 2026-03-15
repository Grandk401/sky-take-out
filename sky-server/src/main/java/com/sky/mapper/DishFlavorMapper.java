package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {
    /**
     * 批量插入菜品口味
     * @param flavors 菜品口味列表
     */
    void insertBatch(List<DishFlavor> flavors);
    /**
     * 根据菜品id删除菜品口味
     * @param dishIds 菜品id列表
     */
    void deleteByDishIds(List<Long> dishIds);
}
