package com.sky.enumeration;

/**
 * 订单状态转换事件 - 区分不同的状态转换触发来源
 * <p>
 * 同一个目标状态可能由不同的事件触发，而每种事件对前置状态的要求不同。
 * 例如："已取消"可以由"用户取消"、"商家拒单"、"商家取消"三种事件触发，
 * 但"商家拒单"只允许从"待接单"状态触发，而"用户取消"允许从"待付款"和"待接单"触发。
 * </p>
 *
 * <pre>
 * 事件与触发者的对应关系：
 * - USER_PAY           → 用户（支付订单）
 * - USER_CANCEL        → 用户（取消订单）
 * - MERCHANT_ACCEPT    → 商家（接单）
 * - MERCHANT_REJECT    → 商家（拒单，仅限待接单状态）
 * - MERCHANT_CANCEL    → 商家（取消订单）
 * - DELIVERY_START     → 配送员/商家（开始派送）
 * - COMPLETE           → 配送员/用户（确认完成）
 * </pre>
 */
public enum OrderTransitionEvent {

    /**
     * 用户支付
     */
    USER_PAY("用户支付"),

    /**
     * 用户取消订单
     */
    USER_CANCEL("用户取消"),

    /**
     * 商家接单
     */
    MERCHANT_ACCEPT("商家接单"),

    /**
     * 商家拒单（仅限待接单状态）
     */
    MERCHANT_REJECT("商家拒单"),

    /**
     * 商家取消订单
     */
    MERCHANT_CANCEL("商家取消"),

    /**
     * 开始派送
     */
    DELIVERY_START("开始派送"),

    /**
     * 完成订单
     */
    COMPLETE("完成订单");

    private final String desc;

    OrderTransitionEvent(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
