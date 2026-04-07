package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rural.ecommerce.entity.Address;
import com.rural.ecommerce.mapper.AddressMapper;
import com.rural.ecommerce.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressMapper addressMapper;

    @Override
    public List<Address> getUserAddresses(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .eq(Address::getDeleted, 0)
               .orderByDesc(Address::getIsDefault)
               .orderByDesc(Address::getCreateTime);
        return addressMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public Address addAddress(Long userId, Address address) {
        address.setUserId(userId);
        
        // 如果是第一个地址或设为默认，则清除其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            addressMapper.clearDefaultByUserId(userId);
        } else {
            // 检查是否已有地址，如果没有则设为默认
            List<Address> existingAddresses = getUserAddresses(userId);
            if (existingAddresses.isEmpty()) {
                address.setIsDefault(1);
            } else {
                address.setIsDefault(0);
            }
        }
        
        addressMapper.insert(address);
        return address;
    }

    @Override
    @Transactional
    public Address updateAddress(Long userId, Long addressId, Address address) {
        // 验证地址是否属于当前用户
        Address existingAddress = getAddressByIdAndUserId(addressId, userId);
        if (existingAddress == null) {
            throw new RuntimeException("地址不存在");
        }
        
        address.setId(addressId);
        address.setUserId(userId);
        
        // 如果设为默认，清除其他默认地址
        if (address.getIsDefault() != null && address.getIsDefault() == 1 && 
            (existingAddress.getIsDefault() == null || existingAddress.getIsDefault() == 0)) {
            addressMapper.clearDefaultByUserId(userId);
        }
        
        addressMapper.updateById(address);
        return addressMapper.selectById(addressId);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        // 验证地址是否属于当前用户
        Address existingAddress = getAddressByIdAndUserId(addressId, userId);
        if (existingAddress == null) {
            throw new RuntimeException("地址不存在");
        }
        
        addressMapper.deleteById(addressId);
        
        // 如果删除的是默认地址，将最新的地址设为默认
        if (existingAddress.getIsDefault() != null && existingAddress.getIsDefault() == 1) {
            List<Address> remainingAddresses = getUserAddresses(userId);
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                newDefault.setIsDefault(1);
                addressMapper.updateById(newDefault);
            }
        }
    }

    @Override
    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        // 验证地址是否属于当前用户
        Address existingAddress = getAddressByIdAndUserId(addressId, userId);
        if (existingAddress == null) {
            throw new RuntimeException("地址不存在");
        }
        
        // 清除所有默认地址
        addressMapper.clearDefaultByUserId(userId);
        
        // 设置当前地址为默认
        existingAddress.setIsDefault(1);
        addressMapper.updateById(existingAddress);
    }

    @Override
    public Address getDefaultAddress(Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getUserId, userId)
               .eq(Address::getIsDefault, 1)
               .eq(Address::getDeleted, 0);
        return addressMapper.selectOne(wrapper);
    }

    private Address getAddressByIdAndUserId(Long addressId, Long userId) {
        LambdaQueryWrapper<Address> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Address::getId, addressId)
               .eq(Address::getUserId, userId)
               .eq(Address::getDeleted, 0);
        return addressMapper.selectOne(wrapper);
    }
}
