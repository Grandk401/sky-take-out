package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/*
    地址簿实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddressBook implements Serializable {
    private static final long serialVersionUID = 1L;
    //主键策略：数据库自增策略
    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private Long userId; // 用户ID;关联user表;
    private String consignee; // 收货人姓名
    private String phone; // 收货人手机号
    private String detail; // 详细地址
    private Integer isDefault; // 是否默认地址（0：否；1：是）
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
    private String createUser; // 创建人
    private String updateUser; // 更新人
    private Integer isDeleted; // 删除标志（0：未删除；1：已删除）
}
