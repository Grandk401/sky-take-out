package com.sky.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 菜品实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("dish")
public class Dish implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id; // 主键ID
    private String name; // 菜品名称
    private Long categoryId; // 分类ID
    private BigDecimal price; // 菜品价格
    private String image; // 菜品图片
    private String description; // 菜品描述
    private Integer status; // 状态（0：停售；1：起售）
    private Integer sort; // 菜品排序权重
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Long createUser; // 创建人ID
    private Long updateUser; // 更新人ID
    @TableField("is_deleted")
    @TableLogic(value = "0", delval = "1")
    private Integer isDeleted; // 删除状态（0：未删除；1：已删除）
}
