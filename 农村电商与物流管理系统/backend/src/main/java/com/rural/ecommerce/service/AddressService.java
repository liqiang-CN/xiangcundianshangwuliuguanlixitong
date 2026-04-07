package com.rural.ecommerce.service;

import com.rural.ecommerce.entity.Address;

import java.util.List;

public interface AddressService {

    /**
     * 获取用户的收货地址列表
     */
    List<Address> getUserAddresses(Long userId);

    /**
     * 添加收货地址
     */
    Address addAddress(Long userId, Address address);

    /**
     * 更新收货地址
     */
    Address updateAddress(Long userId, Long addressId, Address address);

    /**
     * 删除收货地址
     */
    void deleteAddress(Long userId, Long addressId);

    /**
     * 设置默认地址
     */
    void setDefaultAddress(Long userId, Long addressId);

    /**
     * 获取默认地址
     */
    Address getDefaultAddress(Long userId);
}
