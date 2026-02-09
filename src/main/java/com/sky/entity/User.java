package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
 * 用户实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    //主键策略：数据库自增策略
    @TableId(type = IdType.AUTO)
    private Long id;                    // 用户id;
    private String name;                // 姓名;
    private String phone;               // 手机号;
    private LocalDateTime createTime;   // 创建时间;
    private LocalDateTime updateTime;   // 更新时间;
    private Long createUser;            // 创建人id;
    private Long updateUser;            // 修改人id;
    private Integer isDeleted;          // 逻辑删除;0-未删 1-已删;
}
