package com.sky.enumeration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 订单状态枚举 - 基于事件驱动的轻量级状态机实现
 * <p>
 * 定义了订单的所有状态及合法的状态转换规则，
 * 通过 {@link #canTransitionTo(OrderTransitionEvent, OrderStatusEnum)} 方法校验状态转换的合法性。
 * </p>
 * <p>
 * 本实现引入了"转换事件（Event）"维度，
 * 同一个目标状态可以由不同的事件触发，每种事件有独立的前置状态约束。
 * 解决了"商家拒单只能从待接单触发，而用户取消可以从待付款/待接单触发"这类业务语义差异问题。
 * </p>
 *
 * <pre>
 * 状态流转图：
 * 待付款(1) ──[用户支付]──> 待接单(2) ──[商家接单]──> 已接单(3) ──[开始派送]──> 派送中(4) ──[完成]──> 已完成(5)
 *    │                          │                         │
 *    │──[用户取消]──> 已取消(6)   │──[用户取消]──────┐       │
 *    │                          │──[商家拒单]────────┤
 *    │                          │──[商家取消]────────┼──> 已取消(6)
 *    │                                                   │
 *    └──────────────────────────[商家取消]────────────────┘
 * </pre>
 */
public enum OrderStatusEnum {

    /**
     * 待付款 - 初始状态，用户已下单但未支付
     */
    PENDING_PAYMENT(1, "待付款"),

    /**
     * 待接单 - 用户已支付，等待商家接单
     */
    TO_BE_CONFIRMED(2, "待接单"),

    /**
     * 已接单 - 商家已接单
     */
    CONFIRMED(3, "已接单"),

    /**
     * 派送中 - 配送员正在配送
     */
    DELIVERY_IN_PROGRESS(4, "派送中"),

    /**
     * 已完成 - 终态，订单已完成
     */
    COMPLETED(5, "已完成"),

    /**
     * 已取消 - 终态，订单已取消（可能由多种原因触发）
     */
    CANCELLED(6, "已取消");

    /**
     * 状态编码
     */
    private final Integer code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 事件驱动的转换规则表：Event → 目标状态
     * <p>
     * Key: 触发转换的事件（如 USER_PAY、MERCHANT_REJECT）
     * Value: 转换后的目标状态
     * </p>
     */
    private Map<OrderTransitionEvent, OrderStatusEnum> transitionMap;

    static {
        // ====== 待付款 (1) ======
        // 允许的事件：用户支付 → 待接单、用户取消 → 已取消
        Map<OrderTransitionEvent, OrderStatusEnum> pendingPaymentTransitions = new HashMap<>();
        pendingPaymentTransitions.put(OrderTransitionEvent.USER_PAY, TO_BE_CONFIRMED);
        pendingPaymentTransitions.put(OrderTransitionEvent.USER_CANCEL, CANCELLED);
        PENDING_PAYMENT.transitionMap = Collections.unmodifiableMap(pendingPaymentTransitions);

        // ====== 待接单 (2) ======
        // 允许的事件：商家接单→已接单、商家拒单→已取消、商家取消→已取消、用户取消→已取消
        Map<OrderTransitionEvent, OrderStatusEnum> toBeConfirmedTransitions = new HashMap<>();
        toBeConfirmedTransitions.put(OrderTransitionEvent.MERCHANT_ACCEPT, CONFIRMED);
        toBeConfirmedTransitions.put(OrderTransitionEvent.MERCHANT_REJECT, CANCELLED);   // ← 拒单专属路径
        toBeConfirmedTransitions.put(OrderTransitionEvent.MERCHANT_CANCEL, CANCELLED);
        toBeConfirmedTransitions.put(OrderTransitionEvent.USER_CANCEL, CANCELLED);
        TO_BE_CONFIRMED.transitionMap = Collections.unmodifiableMap(toBeConfirmedTransitions);

        // ====== 已接单 (3) ======
        // 允许的事件：开始派送 → 派送中、商家取消 → 已取消
        Map<OrderTransitionEvent, OrderStatusEnum> confirmedTransitions = new HashMap<>();
        confirmedTransitions.put(OrderTransitionEvent.DELIVERY_START, DELIVERY_IN_PROGRESS);
        confirmedTransitions.put(OrderTransitionEvent.MERCHANT_CANCEL, CANCELLED);
        CONFIRMED.transitionMap = Collections.unmodifiableMap(confirmedTransitions);

        // ====== 派送中 (4) ======
        // 允许的事件：完成 → 已完成
        Map<OrderTransitionEvent, OrderStatusEnum> deliveryTransitions = new HashMap<>();
        deliveryTransitions.put(OrderTransitionEvent.COMPLETE, COMPLETED);
        DELIVERY_IN_PROGRESS.transitionMap = Collections.unmodifiableMap(deliveryTransitions);

        // ====== 已完成 (5) - 终态 ======
        COMPLETED.transitionMap = Collections.emptyMap();

        // ====== 已取消 (6) - 终态 ======
        CANCELLED.transitionMap = Collections.emptyMap();
    }

    OrderStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 获取状态编码
     * @return 状态编码
     */
    public Integer getCode() {
        return code;
    }

    /**
     * 获取状态描述
     * @return 状态描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 获取当前状态下所有允许触发的转换事件集合
     *
     * @return 允许触发的事件集合的不可修改视图
     */
    public Set<OrderTransitionEvent> getAllowedEvents() {
        return Collections.unmodifiableSet(transitionMap.keySet());
    }

    /**
     * 是否为终态（已完成或已取消）
     * @return true表示终态（不允许任何转换），false表示非终态
     */
    public boolean isTerminal() {
        return transitionMap.isEmpty();
    }

    /**
     * 校验在指定事件下是否可以转换到目标状态
     * <p>
     * 核心方法。根据预定义的【事件→目标状态】映射规则，
     * 判断当前状态在给定事件的触发下能否转换到指定的目标状态。
     * </p>
     *
     * @param event  触发转换的事件（如 MERCHANT_REJECT 表示商家拒单）
     * @param target 期望的目标状态
     * @return true表示可以转换，false表示不能转换
     */
    public boolean canTransitionTo(OrderTransitionEvent event, OrderStatusEnum target) {
        if (event == null || target == null) {
            return false;
        }
        if (this.isTerminal()) {
            return false;
        }
        OrderStatusEnum actualTarget = this.transitionMap.get(event);
        return target.equals(actualTarget);
    }

    /**
     * 根据状态编码获取对应的枚举值
     *
     * @param code 状态编码
     * @return 对应的枚举值，如果未找到则返回null
     */
    public static OrderStatusEnum getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatusEnum status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
