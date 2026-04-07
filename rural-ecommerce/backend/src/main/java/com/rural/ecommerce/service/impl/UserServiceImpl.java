package com.rural.ecommerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.mapper.UserMapper;
import com.rural.ecommerce.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    
    @Override
    public User login(String username, String password, Integer userType) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username)
               .eq(User::getUserType, userType)
               .eq(User::getDeleted, 0);
        
        User user = getOne(wrapper);
        if (user == null) {
            return null;
        }
        
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!encryptedPassword.equals(user.getPassword())) {
            return null;
        }
        
        return user;
    }
    
    @Override
    public User register(User user) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, user.getUsername());
        if (count(wrapper) > 0) {
            throw new RuntimeException("用户名已存在");
        }
        
        user.setPassword(DigestUtils.md5DigestAsHex(user.getPassword().getBytes()));
        user.setStatus(1);
        save(user);
        return user;
    }
    
    @Override
    public User getUserByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return getOne(wrapper);
    }
    
    @Override
    public List<User> getFarmers() {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUserType, 2)
               .eq(User::getStatus, 1)
               .eq(User::getDeleted, 0)
               .isNotNull(User::getLocation)
               .isNotNull(User::getDescription)
               .ne(User::getLocation, "")
               .ne(User::getDescription, "")
               .orderByDesc(User::getRating);
        return list(wrapper);
    }

    @Override
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        String encryptedPassword = DigestUtils.md5DigestAsHex(rawPassword.getBytes());
        return encryptedPassword.equals(encodedPassword);
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        User user = getById(userId);
        if (user == null) {
            return false;
        }
        user.setPassword(DigestUtils.md5DigestAsHex(newPassword.getBytes()));
        return updateById(user);
    }
}
