package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.Address;
import com.rural.ecommerce.service.AddressService;
import com.rural.ecommerce.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final JwtUtil jwtUtil;

    /**
     * 从请求中获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        String jwt = token.replace("Bearer ", "");
        return jwtUtil.getUserId(jwt);
    }

    /**
     * 获取当前用户的收货地址列表
     */
    @GetMapping("/list")
    public Result<List<Address>> getUserAddresses(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        List<Address> addresses = addressService.getUserAddresses(userId);
        return Result.success(addresses);
    }

    /**
     * 添加收货地址
     */
    @PostMapping("/add")
    public Result<Address> addAddress(@RequestBody Address address, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        // 参数校验
        if (address.getReceiverName() == null || address.getReceiverName().trim().isEmpty()) {
            return Result.error("收货人姓名不能为空");
        }
        if (address.getReceiverPhone() == null || address.getReceiverPhone().trim().isEmpty()) {
            return Result.error("收货人电话不能为空");
        }
        if (address.getDetailAddress() == null || address.getDetailAddress().trim().isEmpty()) {
            return Result.error("详细地址不能为空");
        }
        
        Address savedAddress = addressService.addAddress(userId, address);
        return Result.success(savedAddress);
    }

    /**
     * 更新收货地址
     */
    @PutMapping("/update/{id}")
    public Result<Address> updateAddress(@PathVariable Long id, @RequestBody Address address, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            Address updatedAddress = addressService.updateAddress(userId, id, address);
            return Result.success(updatedAddress);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除收货地址
     */
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            addressService.deleteAddress(userId, id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 设置默认地址
     */
    @PutMapping("/setDefault/{id}")
    public Result<Void> setDefaultAddress(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        
        try {
            addressService.setDefaultAddress(userId, id);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取默认地址
     */
    @GetMapping("/default")
    public Result<Address> getDefaultAddress(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.error("请先登录");
        }
        Address address = addressService.getDefaultAddress(userId);
        return Result.success(address);
    }
}
