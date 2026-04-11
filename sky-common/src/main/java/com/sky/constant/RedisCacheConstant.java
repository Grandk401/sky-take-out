package com.sky.constant;

/**
 * Redis缓存Key统一管理常量类
 * 所有缓存Key的命名规则、前缀、过期时间均在此集中定义
 *
 * Key命名规范：sky:{业务域}:{标识}
 */
public class RedisCacheConstant {

    // ==================== Key 前缀/完整Key 定义 ====================

    /** 店铺状态缓存Key */
    public static final String SHOP_STATUS_KEY = "sky:shop:status";

    /** 分类白名单缓存Key（全部分类） */
    public static final String CATEGORY_ALL_KEY = "sky:category:all";

    /** 菜品列表缓存Key前缀 */
    public static final String DISH_KEY_PREFIX = "sky:dish:";

    /** 套餐列表缓存Key前缀 */
    public static final String SETMEAL_KEY_PREFIX = "sky:setmeal:";

    // ==================== TTL 过期时间常量（单位：秒） ====================

    /** 店铺状态缓存有效期：24小时 */
    public static final long SHOP_STATUS_TTL_SECONDS = 24 * 60 * 60;

    /** 分类白名单缓存有效期：24小时 */
    public static final long CATEGORY_TTL_SECONDS = 24 * 60 * 60;

    /** 菜品列表缓存基础有效期：5小时 */
    public static final long DISH_TTL_BASE_SECONDS = 5 * 60 * 60;

    /** 菜品列表缓存随机抖动上限：2小时——用于预防缓存雪崩 */
    public static final long DISH_TTL_JITTER_SECONDS = 2 * 60 * 60;

    /** 菜品空值缓存有效期：5分钟——防止缓存穿透 */
    public static final long DISH_NULL_TTL_SECONDS = 5 * 60;

    /** 套餐列表缓存基础有效期：5小时 */
    public static final long SETMEAL_TTL_BASE_SECONDS = 5 * 60 * 60;

    /** 套餐列表缓存随机抖动上限：2小时——用于预防缓存雪崩 */
    public static final long SETMEAL_TTL_JITTER_SECONDS = 2 * 60 * 60;

    /** 套餐空值缓存有效期：5分钟——防止缓存穿透 */
    public static final long SETMEAL_NULL_TTL_SECONDS = 5 * 60;

    // ==================== Key 构建工具方法 ====================

    /**
     * 构建菜品列表缓存Key
     * @param categoryId 分类id
     * @return 完整的Redis Key，如 sky:dish:10
     */
    public static String buildDishKey(Long categoryId) {
        return DISH_KEY_PREFIX + categoryId;
    }

    /**
     * 构建套餐列表缓存Key
     * @param categoryId 分类id
     * @return 完整的Redis Key，如 sky:setmeal:10
     */
    public static String buildSetmealKey(Long categoryId) {
        return SETMEAL_KEY_PREFIX + categoryId;
    }
}
