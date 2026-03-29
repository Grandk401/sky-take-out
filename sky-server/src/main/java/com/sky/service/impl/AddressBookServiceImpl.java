package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.exception.AddressBookBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class AddressBookServiceImpl implements AddressBookService {

    @Autowired
    private AddressBookMapper addressBookMapper;

    /**
     * 新增收货地址
     * @param addressBook
     */
    @Override
    @Transactional
    public void addAddressBook(AddressBook addressBook) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();
        addressBook.setUserId(userId);
        //设置默认值
        if(addressBook.getIsDefault() == null){
            addressBook.setIsDefault(0);
        }else if(addressBook.getIsDefault() == 1){
            // 如果设置为默认地址1，则更新其他地址为0
            addressBookMapper.updateAllToNonDefault(addressBook.getUserId());// 把所有地址的is_default设置为0
        }
        //插入新地址
        addressBookMapper.insert(addressBook);
    }

    /**
     * 根据地址id查询地址
     * @param id
     * @return
     */
    @Override
    public AddressBook getAddressBookById(Long id) {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBook.setId(id);
        return addressBookMapper.selectAddressBookById(addressBook);
    }

    /**
     * 修改地址
     * @param addressBook
     */
    @Override
    public void updateAddressBook(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 删除收货地址
     * @param id
     */
    @Override
    @Transactional
    public void deleteAddressBook(Long id) {
        Long userId = BaseContext.getCurrentId();
        AddressBook addressBook = getAddressBookById(id);

        if(addressBook == null){
            throw new AddressBookBusinessException("收货地址不存在");
        }
        // 如果要删除的是默认地址，可以设置另一个地址为默认
        if(addressBook.getIsDefault() == 1){
            // 查找用户的其他地址
            AddressBook otherAddress = addressBookMapper.selectOtherAddresses(userId, id);
            if(otherAddress != null){
                // 设置所得地址为默认
                otherAddress.setIsDefault(1);
                addressBookMapper.updateById(otherAddress);
            }
        }
        //执行删除
        addressBookMapper.deleteById(addressBook);
    }

    /**
     * 查询当前用户的收货地址列表
     * @param currentId
     * @return
     */
    @Override
    public List<AddressBook> listByUserId(Long currentId) {
        List<AddressBook> list = addressBookMapper.selectList(currentId);
        return list;
    }

    /**
     * 设置默认收货地址
     * @param addressBook
     */
    @Override
    @Transactional
    public void setDefault(AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        // 先把所有地址的is_default设置为0
        addressBookMapper.updateAllToNonDefault(addressBook.getUserId());
        // 再把当前地址的is_default设置为1
        addressBook.setIsDefault(1);
        addressBookMapper.updateById(addressBook);
    }

    /**
     * 获取默认收货地址
     * @param currentId
     * @return
     */
    @Override
    public AddressBook getDefault(Long currentId) {
        AddressBook addressBook = new AddressBook();
        addressBook.setUserId(currentId);
        addressBook.setIsDefault(1);
        return addressBookMapper.selectDefaultAddressBook(addressBook);
    }
}
