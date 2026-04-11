package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisCacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     *
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        log.info("新增菜品:{}", dishDTO);
        // 新增一个菜品
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.insert(dish);
        //新增菜品口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dish.getId());// 新增菜品口味时，需要获取菜品id
            });
            dishFlavorMapper.insertBatch(flavors);
        }
        // 清空Redis中菜品列表缓存
        redisTemplate.delete(RedisCacheConstant.buildDishKey(dishDTO.getCategoryId()));
    }

    /**
     * 分页查询菜品
     *
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        log.info("分页查询菜品:{}", dishPageQueryDTO);
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 删除菜品
     *
     * @param ids
     */
    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        log.info("删除菜品:{}", ids);
        //起售中的菜品不能被删除
        List<Dish> dishList = dishMapper.selectByIds(ids);
        for (Dish dish : dishList) {
            if (dish.getStatus().equals(StatusConstant.ENABLE)) {
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //被套餐关联的菜品不能被删除
        List<Long> setmealIds = setmealDishMapper.selectSetmealIdsByDishIds(ids);
        if (setmealIds != null && setmealIds.size() > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //通过ids查询出菜品所属分类
        List<Long> categoryIds = dishMapper.selectCategoryIds(ids);
        //删除菜品关联的口味
        dishFlavorMapper.deleteByDishIds(ids);
        //删除菜品
        dishMapper.deleteByIds(ids);
        // 清空Redis中菜品列表缓存
        for (Long categoryId : categoryIds) {
            redisTemplate.delete(RedisCacheConstant.buildDishKey(categoryId));
        }
    }

    /**
     * 根据id查询菜品
     *
     * @param id 菜品id
     * @return
     */
    @Override
    public DishVO selectById(Long id) {
        log.info("根据id查询菜品:{}", id);
        // 查询菜品基本信息
        Dish dish = dishMapper.selectById(id);
        // 查询菜品口味信息
        List<DishFlavor> flavors = dishFlavorMapper.selectByDishId(id);
        // 封装VO对象
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }
    /**
     * 更新菜品
     * @param dishDTO
     */
    @Override
    @Transactional
    public void updateWithFlavor(DishDTO dishDTO) {
        log.info("更新菜品:{}", dishDTO);
        // 查原分类ID
        Dish oldDish = dishMapper.selectById(dishDTO.getId());
        Long oldCategoryId = oldDish.getCategoryId();
        Long newCategoryId = dishDTO.getCategoryId();
        // 更新菜品基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dishMapper.update(dish);
        log.info("更新菜品口味，菜品id:{}", dishDTO.getId());
        // 删除原有的口味
        Long dishId = dishDTO.getId();
        dishFlavorMapper.deleteByDishId(dishId);
        log.info("删除菜品原有口味，菜品id:{}", dishId);
        // 更新菜品口味信息
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            // 新增新的口味
            flavors.forEach(dishFlavor -> {
                dishFlavor.setDishId(dishDTO.getId());// 更新菜品口味时，需要获取菜品id
            });
            dishFlavorMapper.insertBatch(flavors);
            log.info("新增菜品口味，菜品id:{},口味数量:{}", dishId, flavors.size());
        }else {
            log.info("菜品无口味新增或修改");
        }
        // 清除Redis缓存
        // 原分类必清（菜品信息变了）
        redisTemplate.delete(RedisCacheConstant.buildDishKey(oldCategoryId));

        // 如果分类变了，新分类也要清
        if (newCategoryId != null && !newCategoryId.equals(oldCategoryId)) {
            redisTemplate.delete(RedisCacheConstant.buildDishKey(newCategoryId));
        }
    }
    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    public List<Dish> list(Long categoryId) {
        Dish dish = Dish.builder()
                .categoryId(categoryId)
                .status(StatusConstant.ENABLE)
                .build();
        return dishMapper.list(dish);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        List<Dish> dishList = dishMapper.list(dish);

        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }

        return dishVOList;
    }

    /**
     * 根据分类id查询菜品
     * @param categoryId
     * @return
     */
    @Override
    public List<DishVO> listWithFlavorByCategory(Long categoryId) {
        // 缓存穿透防护：先校验categoryId是否存在于分类白名单
        if (!categoryService.existsByCategoryId(categoryId)) {
            log.warn("缓存穿透拦截：非法 categoryId={}, 直接返回空列表", categoryId);
            return new ArrayList<>();
        }

        String key = RedisCacheConstant.buildDishKey(categoryId);

        // 先查询Redis中是否存在
        List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            // 缓存命中，直接返回
            return list;
        }

        // 缓存未命中，查询数据库
        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE); // 只查询起售中的菜品
        list = listWithFlavor(dish);

        // 存入Redis缓存（基础5小时 + 随机0~2小时，预防可能的缓存雪崩问题）
        if (list != null && list.size() > 0) {
            long ttl = RedisCacheConstant.DISH_TTL_BASE_SECONDS + (long) (Math.random() * RedisCacheConstant.DISH_TTL_JITTER_SECONDS);
            redisTemplate.opsForValue().set(key, list, ttl, TimeUnit.SECONDS);
        } else {
            // 查询结果为空时缓存空值，防止缓存穿透（短过期时间）
            redisTemplate.opsForValue().set(key, new ArrayList<>(), RedisCacheConstant.DISH_NULL_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return list;
    }

    /**
     * 菜品起售、停售
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        // 查询菜品原信息（获取分类ID用于清除缓存）
        Dish dish = dishMapper.selectById(id);
        Long categoryId = dish.getCategoryId();

        // 构建更新对象，只更新状态
        Dish updateDish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(updateDish);

        // 清除Redis缓存（该分类下菜品状态变了，必须清缓存）
        String key = RedisCacheConstant.buildDishKey(categoryId);
        redisTemplate.delete(key);

        log.info("菜品状态修改成功：id={}, status={}, 清除分类{}缓存", id, status, categoryId);
    }
}
