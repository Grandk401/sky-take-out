package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 分类实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Category implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id; // 主键ID
    private Integer type; // 分类类型（1：菜品分类；2：套餐分类）
    private String name; // 分类名称
    private Integer sort; // 分类排序
    private Integer status; // 状态（0：禁用；1：启用）
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private Long createUser; // 创建人ID
    private Long updateUser; // 更新人ID
    private Integer isDeleted; // 删除状态（0：未删除；1：已删除）
}
