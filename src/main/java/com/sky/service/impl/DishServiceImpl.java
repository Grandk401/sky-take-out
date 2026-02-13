package com.sky.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sky.entity.Dish;
import com.sky.mapper.DishMapper;
import com.sky.service.DishService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * 菜品服务实现类
 */
@Service
public class DishServiceImpl implements DishService {
    /**
     * 日志记录器
     */
    private static final Logger logger = LoggerFactory.getLogger(DishServiceImpl.class);

    private final DishMapper dishMapper;

    /**
     * 构造器注入
     */
    public DishServiceImpl(DishMapper dishMapper) {
        this.dishMapper = dishMapper;
    }

    /**
     * 新增菜品
     */
    @Override
    public void save(Dish dish) {
        logger.info("开始新增菜品：{}", dish.getName());

        // 菜品名称非空检验
        if (!StringUtils.hasText(dish.getName())) {
            throw new RuntimeException("菜品名称不能为空");
        }

        // 菜品价格非空检验
        if (dish.getPrice() == null || dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("菜品价格不能为空且必须大于0");
        }

        // 菜品分类非空检验
        if (dish.getCategoryId() == null) {
            throw new RuntimeException("菜品分类不能为空");
        }

        // 菜品图片非空检验
        if (!StringUtils.hasText(dish.getImage())) {
            throw new RuntimeException("菜品图片不能为空");
        }

        // 状态检验
        if (dish.getStatus() == null) {
            dish.setStatus(1); // 默认起售
        } else if (dish.getStatus() != 0 && dish.getStatus() != 1){
            throw new RuntimeException("状态只能为0或1");
        }

        // 排序字段处理
        if (dish.getSort() == null) {
            dish.setSort(0); // 默认排序
        }

        dishMapper.insert(dish);
        logger.info("新增菜品成功：{}", dish.getName());
    }

    /**
     * 根据ID删除菜品
     */
    @Override
    public void deleteById(Long id) {
        logger.info("开始删除菜品，ID：{}", id);

        // ID合法性校验
        if (id == null || id <= 0) {
            throw new RuntimeException("ID不能为空或者负数(必须为正整数)");
        }

        // 校验菜品是否存在
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }

        dishMapper.deleteById(id);
        logger.info("删除菜品成功，ID：{}", id);
    }

    /**
     * 根据ID查询菜品
     */
    @Override
    public Dish getById(Long id) {
        logger.info("开始查询菜品，ID：{}", id);

        // ID合法性校验
        if (id == null || id <= 0) {
            throw new RuntimeException("ID不能为空或者负数(必须为正整数)");
        }

        // 校验菜品是否存在
        Dish dish = dishMapper.selectById(id);
        if (dish == null) {
            throw new RuntimeException("菜品不存在");
        }

        logger.info("查询菜品成功，ID：{}", id);
        return dish;
    }

    /**
     * 修改菜品
     */
    @Override
    public void update(Dish dish) {
        logger.info("开始修改菜品，ID：{}", dish.getId());

        // ID合法性校验
        if (dish.getId() == null || dish.getId() <= 0) {
            throw new RuntimeException("ID必须为正整数");
        }

        // 校验菜品是否存在
        Dish existingDish = dishMapper.selectById(dish.getId());
        if (existingDish == null) {
            throw new RuntimeException("菜品不存在");
        }

        // 价格校验（若传了价格）
        if (dish.getPrice() != null && dish.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("菜品价格必须大于0");
        }

        // 名称校验（若传了名称）
        if (dish.getName() != null && !StringUtils.hasText(dish.getName())) {
            throw new RuntimeException("菜品名称不能为空");
        }

        // 图片校验（若传了图片）
        if (dish.getImage() != null && !StringUtils.hasText(dish.getImage())) {
            throw new RuntimeException("菜品图片不能为空");
        }

        // 状态校验（若传了状态）
        if (dish.getStatus() != null && dish.getStatus() != 0 && dish.getStatus() != 1) {
            throw new RuntimeException("状态只能为0或1");
        }

        dishMapper.updateById(dish);
        logger.info("修改菜品成功，ID：{}", dish.getId());
    }

    /**
     * 根据分类ID和状态查询菜品列表
     */
    @Override
    public List<Dish> listByCategoryIdAndStatus(Long categoryId, Integer status) {
        logger.info("开始查询菜品列表，分类ID：{}，状态：{}", categoryId, status);

        // 分类ID合法性校验
        if (categoryId == null || categoryId <= 0) {
            throw new RuntimeException("分类ID不能为空或者负数(必须为正整数)");
        }

        // 状态校验
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("状态只能为0或1");
        }

        // 根据分类ID和状态查询菜品列表
        List<Dish> dishList = dishMapper.selectList(new LambdaQueryWrapper<Dish>()
                .eq(Dish::getCategoryId, categoryId)
                .eq(Dish::getStatus, status));

        logger.info("查询菜品列表成功，分类ID：{}，状态：{}，数量：{}", categoryId, status, dishList.size());
        return dishList;
    }
}