package com.sky.mapper;

import com.sky.entity.AddressBook;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AddressBookMapper {

    /**
     * 新增收货地址
     * @param addressBook
     */
    void insert(AddressBook addressBook);

    /**
     * 根据地址id查询地址
     * @param addressBook
     * @return
     */
    @Select("select * from address_book where id = #{id} and user_id = #{userId}")
    AddressBook selectAddressBookById(AddressBook addressBook);

    /**
     * 修改地址
     * @param addressBook
     */
    void updateById(AddressBook addressBook);

    /**
     * 删除收货地址
     * @param addressBook
     */
    @Delete("delete from address_book where id = #{id} and user_id = #{userId}")
    void deleteById(AddressBook addressBook);

    /**
     * 查询当前用户的收货地址列表
     * @param currentId
     * @return
     */
    @Select("select * from address_book where user_id = #{currentId} order by is_default desc")
    List<AddressBook> selectList(Long currentId);

    /**
     * 设置当前用户全部收货地址为0
     * @param userId
     */
    @Update("update address_book set is_default = 0 where user_id = #{userId}")
    void updateAllToNonDefault(Long userId);

    /**
     * 查询当前用户的默认收货地址
     * @param addressBook
     * @return
     */
    @Select("select * from address_book where user_id = #{userId} and is_default = 1 limit 1")
    AddressBook selectDefaultAddressBook(AddressBook addressBook);

    /**
     * 查询当前用户的其他收货地址
     * @param userId
     * @param excludeId
     * @return
     */
    @Select("select * from address_book where user_id = #{userId} and id != #{excludeId} order by id desc limit 1")
    AddressBook selectOtherAddresses(@Param("userId") Long userId, @Param("excludeId") Long excludeId);
}
