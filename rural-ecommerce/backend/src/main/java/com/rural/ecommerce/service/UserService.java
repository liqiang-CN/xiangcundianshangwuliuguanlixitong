package com.rural.ecommerce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.rural.ecommerce.entity.User;

import java.util.List;

public interface UserService extends IService<User> {

    User login(String username, String password, Integer userType);

    User register(User user);

    User getUserByUsername(String username);

    /**
     * 获取所有农户用户
     */
    List<User> getFarmers();

    /**
     * 验证密码
     */
    boolean verifyPassword(String rawPassword, String encodedPassword);

    /**
     * 更新密码
     */
    boolean updatePassword(Long userId, String newPassword);
}
