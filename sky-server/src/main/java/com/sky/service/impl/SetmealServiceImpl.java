package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisCacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 套餐业务实现
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 新增套餐，同时需要保存套餐和菜品的关联关系
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //向套餐表插入数据
        setmealMapper.insert(setmeal);

        //获取生成的套餐id
        Long setmealId = setmeal.getId();

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });

        //保存套餐和菜品的关联关系
        setmealDishMapper.insertBatch(setmealDishes);

        // 清空Redis中套餐列表缓存
        redisTemplate.delete(RedisCacheConstant.buildSetmealKey(setmealDTO.getCategoryId()));
    }

    /**
     * 分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        int pageNum = setmealPageQueryDTO.getPage();
        int pageSize = setmealPageQueryDTO.getPageSize();

        PageHelper.startPage(pageNum, pageSize);
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 1. 校验：起售中的套餐不能删除
        ids.forEach(id -> {
            Setmeal setmeal = setmealMapper.getById(id);
            if(StatusConstant.ENABLE == setmeal.getStatus()){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        });

        // 2. 先查询涉及的所有分类ID（必须在删除之前查，否则数据已不存在）
        List<Long> categoryIds = setmealMapper.selectCategoryIds(ids);

        // 3. 删除数据库记录
        if (!ids.isEmpty()){
            setmealMapper.deleteBatchByIds(ids);
            setmealDishMapper.deleteBatchBySetmealIds(ids);
        }

        // 4. 精准清理涉及分类的Redis缓存
        for (Long categoryId : categoryIds) {
            redisTemplate.delete(RedisCacheConstant.buildSetmealKey(categoryId));
        }
    }

    /**
     * 根据id查询套餐和套餐菜品关系
     *
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);

        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);

        return setmealVO;
    }

    /**
     * 修改套餐
     *
     * @param setmealDTO
     */
    @Transactional
    public void update(SetmealDTO setmealDTO) {
        // 查原分类ID，用于后续清除旧分类的缓存
        Setmeal oldSetmeal = setmealMapper.getById(setmealDTO.getId());
        Long oldCategoryId = oldSetmeal.getCategoryId();
        Long newCategoryId = setmealDTO.getCategoryId();

        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);

        //1、修改套餐表，执行update
        setmealMapper.update(setmeal);

        //套餐id
        Long setmealId = setmealDTO.getId();

        //2、删除套餐和菜品的关联关系，操作setmeal_dish表，执行delete
        setmealDishMapper.deleteBySetmealId(setmealId);

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        //3、重新插入套餐和菜品的关联关系，操作setmeal_dish表，执行insert
        setmealDishMapper.insertBatch(setmealDishes);

        // 清除Redis缓存：原分类必清（套餐信息变了）
        redisTemplate.delete(RedisCacheConstant.buildSetmealKey(oldCategoryId));

        // 如果分类变了，新分类也要清
        if (newCategoryId != null && !newCategoryId.equals(oldCategoryId)) {
            redisTemplate.delete(RedisCacheConstant.buildSetmealKey(newCategoryId));
        }
    }

    /**
     * 套餐起售、停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        //起售套餐时，判断套餐内是否有停售菜品，有停售菜品提示"套餐内包含未启售菜品，无法启售"
        if(status == StatusConstant.ENABLE){
            //select a.* from dish a left join setmeal_dish b on a.id = b.dish_id where b.setmeal_id = ?
            List<Dish> dishList = dishMapper.getBySetmealId(id);
            if(dishList != null && dishList.size() > 0){
                dishList.forEach(dish -> {
                    if(StatusConstant.DISABLE == dish.getStatus()){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }

        Setmeal setmeal = Setmeal.builder()
                .id(id)
                .status(status)
                .build();
        setmealMapper.update(setmeal);

        // 精准清理该套餐所属分类的缓存
        String key = RedisCacheConstant.buildSetmealKey(setmealMapper.getById(id).getCategoryId());
        redisTemplate.delete(key);
    }
    /**
     * 根据分类id查询套餐（用户端调用）
     * @param setmeal
     * @return
     */
    public List<Setmeal> list(Setmeal setmeal) {
        // 缓存穿透防护：校验categoryId是否存在于分类白名单
        if (setmeal.getCategoryId() != null && !categoryService.existsByCategoryId(setmeal.getCategoryId())) {
            log.warn("缓存穿透拦截：非法 categoryId={}，直接返回空列表", setmeal.getCategoryId());
            return new ArrayList<>();
        }
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }

    /**
     * 根据分类id查询套餐（带Redis缓存，用户端调用）
     *
     * @param categoryId 分类id
     * @return 套餐列表
     */
    @Override
    public List<Setmeal> listByCategoryWithCache(Long categoryId) {
        // 第1层：缓存穿透防护——校验categoryId是否存在于分类白名单
        if (!categoryService.existsByCategoryId(categoryId)) {
            log.warn("缓存穿透拦截：非法 categoryId={}，直接返回空列表", categoryId);
            return new ArrayList<>();
        }

        String key = RedisCacheConstant.buildSetmealKey(categoryId);

        // 第2层：先查询Redis中是否存在
        List<Setmeal> list = (List<Setmeal>) redisTemplate.opsForValue().get(key);
        if (list != null) {
            // 缓存命中，直接返回
            return list;
        }

        // 第3层：缓存未命中，查询数据库
        Setmeal setmeal = new Setmeal();
        setmeal.setCategoryId(categoryId);
        setmeal.setStatus(StatusConstant.ENABLE); // 只查询起售中的套餐
        list = setmealMapper.list(setmeal);

        // 存入Redis缓存（基础5小时 + 随机0~2小时，预防可能的缓存雪崩问题）
        if (list != null && list.size() > 0) {
            long ttl = RedisCacheConstant.SETMEAL_TTL_BASE_SECONDS + (long) (Math.random() * RedisCacheConstant.SETMEAL_TTL_JITTER_SECONDS);
            redisTemplate.opsForValue().set(key, list, ttl, TimeUnit.SECONDS);
        } else {
            // 查询结果为空时缓存空值，防止缓存穿透（短过期时间）
            redisTemplate.opsForValue().set(key, new ArrayList<>(), RedisCacheConstant.SETMEAL_NULL_TTL_SECONDS, TimeUnit.SECONDS);
        }

        return list;
    }
}