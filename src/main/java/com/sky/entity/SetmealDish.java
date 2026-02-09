package com.sky.entity;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
    套餐菜品关联实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetmealDish implements Serializable {
    private static final long serialVersionUID = 1L;
    //主键策略：数据库自增策略
    @TableId(type = IdType.AUTO)
    private Long id;                    // 关联id;
    private Long setmealId;             // 套餐id;关联setmeal表;
    private Long dishId;                // 菜品id;关联dish表;
    private Integer copies;             // 菜品份数;
    private LocalDateTime createTime;   // 创建时间;
    private LocalDateTime updateTime;   // 更新时间;
    private Long createUser;            // 创建人id;关联user表;
    private Long updateUser;            // 修改人id;关联user表;
    private Integer isDeleted;          // 逻辑删除;0-未删 1-已删;
}
