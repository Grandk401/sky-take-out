package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper {
    /**
     * 根据openid查询用户信息
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenId(String openid);

    /**
     * 插入用户信息
     */
    void insert(User user);

    /**
     * 根据id查询用户信息
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 按日期范围分组统计每日新增用户
     */
    List<Map<String, Object>> countByDateRange(LocalDate begin, LocalDate end);

    /**
     * 根据动态条件统计用户数量
     * @param map 包含 begin/end 等条件
     * @return 用户数
     */
    Integer countByMap(Map map);

}
