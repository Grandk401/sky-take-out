package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.entity.AddressBook;
import com.sky.result.Result;
import com.sky.service.AddressBookService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * 收货地址管理
 */
@RestController
@RequestMapping("/user/addressBook")
@Api(tags = "收货地址管理")
public class AddressBookController {

    @Autowired
    private AddressBookService addressBookService;

    /**
     * 新增收货地址
     * @param addressBook
     * @return
     */
    @PostMapping
    @ApiOperation(value = "新增收货地址")
    public Result addAddressBook(@RequestBody AddressBook addressBook) {
        addressBook.setUserId(BaseContext.getCurrentId());
        addressBookService.addAddressBook(addressBook);
        return Result.success();
    }

    /**
     * 根据地址id查询地址
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    @ApiOperation(value = "根据地址id查询地址")
    public Result<AddressBook> getAddressBookById(@PathVariable Long id) {
        AddressBook addressBook = addressBookService.getAddressBookById(id);
        return Result.success(addressBook);
    }

    /**
     * 修改收货地址
     * @param addressBook
     * @return
     */
    @PutMapping
    @ApiOperation(value = "修改收货地址")
    public Result updateAddressBook(@RequestBody AddressBook addressBook) {
        addressBookService.updateAddressBook(addressBook);
        return Result.success();
    }

    /**
     * 删除收货地址
     * @param id
     * @return
     */
    @DeleteMapping
    @ApiOperation(value = "删除收货地址")
    public Result deleteAddressBook(@RequestParam Long id) {
        addressBookService.deleteAddressBook(id);
        return Result.success();
    }

    /**
     * 查询当前用户的收货地址列表
     * @return
     */
    @GetMapping("/list")
    @ApiOperation(value = "查询当前用户的收货地址列表")
    public Result<List<AddressBook>> list() {
        List<AddressBook> list = addressBookService.listByUserId(BaseContext.getCurrentId());
        return Result.success(list);
    }

    /**
     * 设置默认收货地址
     * @param addressBook
     * @return
     */
    @PutMapping("/default")
    @ApiOperation(value = "设置默认收货地址")
    public Result setDefault(@RequestBody AddressBook addressBook) {
        addressBookService.setDefault(addressBook);
        return Result.success();
    }

    /**
     * 获取默认收货地址
     * @return
     */
    @GetMapping("/default")
    @ApiOperation(value = "获取默认收货地址")
    public Result<AddressBook> getDefault() {
        AddressBook addressBook = addressBookService.getDefault(BaseContext.getCurrentId());
        if(addressBook != null){
            return Result.success(addressBook);
        }else {
            return Result.error("没有默认收货地址");
        }
    }
}
