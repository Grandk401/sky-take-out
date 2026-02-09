package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 购物车实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShoppingCart implements Serializable {
    private static final long serialVersionUID = 1L;
    //主键策略：数据库自增策略
    @TableId(type = IdType.AUTO)
    private Long id;                    // 购物车id;
    private Long userId;                // 用户id;关联user表;
    private Long dishId;                // 菜品id;关联dish表;
    private String name;                // 名称;
    private String image;               // 图片;
    private Integer number;             // 数量;
    private BigDecimal amount;          // 金额;
    private LocalDateTime createTime;   // 创建时间;
    private LocalDateTime updateTime;   // 更新时间;
    private Long createUser;            // 创建人id;关联user表;
    private Long updateUser;            // 修改人id;关联user表;
    private Integer isDeleted;          // 逻辑删除;0-未删 1-已删;
}
