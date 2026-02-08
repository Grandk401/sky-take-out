package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
    套餐实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Setmeal implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;                    // 套餐id;
    private String name;                // 套餐名称;
    private Long categoryId;            // 分类id;关联category表;
    private BigDecimal price;           // 套餐价格;
    private String image;               // 套餐图片;
    private String description;         // 套餐描述;
    private Integer status;             // 套餐状态;0-停用 1-启用;
    private Integer sort;               // 排序;
    private LocalDateTime createTime;   // 创建时间;
    private LocalDateTime updateTime;   // 更新时间;
    private Long createUser;            // 创建人id;关联user表;
    private Long updateUser;            // 修改人id;关联user表;
    private Integer isDeleted;          // 逻辑删除;0-未删 1-已删;
}
