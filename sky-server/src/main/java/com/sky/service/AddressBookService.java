package com.sky.service;


import com.sky.entity.AddressBook;

import java.util.List;

public interface AddressBookService {

    /**
     * 新增收货地址
     * @param addressBook
     */
    void addAddressBook(AddressBook addressBook);

    /**
     * 根据地址id查询地址
     * @param id
     * @return
     */
    AddressBook getAddressBookById(Long id);

    /**
     * 修改地址
     * @param addressBook
     */
    void updateAddressBook(AddressBook addressBook);

    /**
     * 删除收货地址
     * @param id
     */
    void deleteAddressBook(Long id);

    /**
     * 查询当前用户的收货地址列表
     * @param currentId
     * @return
     */
    List<AddressBook> listByUserId(Long currentId);

    /**
     * 设置默认收货地址
     * @param addressBook
     */
    void setDefault(AddressBook addressBook);

    /**
     * 获取默认收货地址
     * @param currentId
     * @return
     */
    AddressBook getDefault(Long currentId);
}
