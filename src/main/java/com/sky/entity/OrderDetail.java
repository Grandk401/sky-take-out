package com.sky.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/*
    订单详情实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("order_detail")
public class OrderDetail implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id; // 主键ID
    private Long orderId; // 订单ID;关联orders表;
    private Long dishId; // 菜品ID
    private String name; // 菜品名称
    private String image; // 菜品图片
    private Integer number; // 数量
    private BigDecimal amount; // 金额
}
