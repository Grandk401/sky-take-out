package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisCacheConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.CategoryDTO;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 分类业务层
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 新增分类
     * @param categoryDTO
     */
    public void save(CategoryDTO categoryDTO) {
        Category category = new Category();
        //属性拷贝
        BeanUtils.copyProperties(categoryDTO, category);

        //分类状态默认为禁用状态0
        category.setStatus(StatusConstant.DISABLE);

        categoryMapper.insert(category);
        // 写操作完成后自动重建分类缓存白名单
        rebuildCategoryCache();
    }

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    public PageResult pageQuery(CategoryPageQueryDTO categoryPageQueryDTO) {
        PageHelper.startPage(categoryPageQueryDTO.getPage(),categoryPageQueryDTO.getPageSize());
        //下一条sql进行分页，自动加入limit关键字分页
        Page<Category> page = categoryMapper.pageQuery(categoryPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 根据id删除分类
     * @param id
     */
    public void deleteById(Long id) {
        //查询当前分类是否关联了菜品，如果关联了就抛出业务异常
        Integer count = dishMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        }

        //查询当前分类是否关联了套餐，如果关联了就抛出业务异常
        count = setmealMapper.countByCategoryId(id);
        if(count > 0){
            //当前分类下有菜品，不能删除
            throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        }

        //删除分类数据
        categoryMapper.deleteById(id);
        // 写操作完成后自动重建分类缓存白名单
        rebuildCategoryCache();
    }

    /**
     * 修改分类
     * @param categoryDTO
     */
    public void update(CategoryDTO categoryDTO) {
        Category category = new Category();
        BeanUtils.copyProperties(categoryDTO,category);

        categoryMapper.update(category);
        // 写操作完成后自动重建分类缓存白名单
        rebuildCategoryCache();
    }

    /**
     * 启用、禁用分类
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Category category = Category.builder()
                .id(id)
                .status(status)
                .build();
        categoryMapper.update(category);
        // 写操作完成后自动重建分类缓存白名单
        rebuildCategoryCache();
    }

    /**
     * 根据类型查询分类
     * @param type
     * @return
     */
    public List<Category> list(Integer type) {
        return categoryMapper.list(type);
    }

    /**
     * 管理端写操作后主动重建分类缓存白名单，消除缓存失效间隙
     */
    @Override
    public void rebuildCategoryCache() {
        log.info("【缓存重建】管理端写操作完成，主动重建分类缓存白名单");
        long start = System.currentTimeMillis();
        getOrCreateCategoryWhitelist();
        log.info("【缓存重建】完成，耗时 {}ms", System.currentTimeMillis() - start);
    }

    /**
     * 获取或创建分类白名单缓存（缓存不存在/过期时自动查库写入）
     * @return 分类列表
     */
    private List<Category> getOrCreateCategoryWhitelist() {
        List<Category> categories = (List<Category>) redisTemplate.opsForValue().get(RedisCacheConstant.CATEGORY_ALL_KEY);

        if (categories == null || categories.isEmpty()) {
            log.info("分类白名单 {} 未命中，触发预热...", RedisCacheConstant.CATEGORY_ALL_KEY);
            long start = System.currentTimeMillis();
            categories = categoryMapper.list(null);
            long cost = System.currentTimeMillis() - start;

            if (categories != null && !categories.isEmpty()) {
                redisTemplate.opsForValue().set(RedisCacheConstant.CATEGORY_ALL_KEY, categories, RedisCacheConstant.CATEGORY_TTL_SECONDS, TimeUnit.SECONDS);
                log.info("分类白名单预热完成，共加载 {} 个分类，耗时 {}ms，已写入Redis(TTL=24h)", categories.size(), cost);
            } else {
                log.warn("分类白名单预热后仍为空（数据库无分类数据）");
            }
        }

        return categories;
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        // 从Redis分类白名单中读取所有分类（缓存不存在时自动触发预热）
        List<Category> categories = getOrCreateCategoryWhitelist();
        // 判断分类是否存在于白名单中
        return categories.stream().anyMatch(c -> c.getId().equals(categoryId));
    }
}
