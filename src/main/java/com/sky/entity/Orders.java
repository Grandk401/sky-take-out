package com.sky.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/*
 * 订单实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@TableName("orders")
public class Orders implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;                    // 订单id;
    private String number;              // 订单号;
    private Integer status;             // 订单状态;
    private Long userId;                // 用户id;关联user表;
    private Long addressBookId;         // 地址簿id;关联address_book表;
    private BigDecimal amount;          // 订单金额;
    private String remark;              // 订单备注;
    private String consignee;           // 收货人;
    private String phone;               // 手机号;
    private String address;             // 收货地址;
    private LocalDateTime createTime;   // 创建时间;
    private LocalDateTime updateTime;   // 更新时间;
    private Long createUser;            // 创建人id;关联user表;
    private Long updateUser;            // 修改人id;关联user表;
    @TableField("is_deleted")
    private Integer isDeleted;          // 逻辑删除;0-未删 1-已删;
}
