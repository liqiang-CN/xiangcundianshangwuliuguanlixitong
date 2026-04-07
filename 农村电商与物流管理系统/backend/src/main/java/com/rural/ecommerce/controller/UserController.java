package com.rural.ecommerce.controller;

import com.rural.ecommerce.common.Result;
import com.rural.ecommerce.entity.User;
import com.rural.ecommerce.service.UserService;
import com.rural.ecommerce.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> params) {
        try {
            String username = params.get("username");
            String password = params.get("password");
            String userTypeStr = params.get("userType");
            
            if (username == null || username.isEmpty()) {
                return Result.error("用户名不能为空");
            }
            if (password == null || password.isEmpty()) {
                return Result.error("密码不能为空");
            }
            if (userTypeStr == null || userTypeStr.isEmpty()) {
                return Result.error("用户类型不能为空");
            }
            
            Integer userType = Integer.valueOf(userTypeStr);
            
            User user = userService.login(username, password, userType);
            if (user == null) {
                return Result.error("用户名或密码错误");
            }
            
            // 检查用户状态，如果为0（禁用）则阻止登录
            if (user.getStatus() != null && user.getStatus() == 0) {
                return Result.error("账号已被禁用，请联系管理员");
            }
            
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());
            
            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("userInfo", user);
            
            return Result.success(result);
        } catch (NumberFormatException e) {
            return Result.error("用户类型格式错误");
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登录失败：" + e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public Result<User> register(@RequestBody User user) {
        try {
            User newUser = userService.register(user);
            return Result.success(newUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
    
    @GetMapping("/info")
    public Result<User> getUserInfo(@RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(jwt);
        User user = userService.getById(userId);
        return Result.success(user);
    }
    
    @PutMapping("/info")
    public Result<User> updateUserInfo(@RequestBody User user, @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(jwt);
        
        // 确保只能修改自己的信息
        if (!userId.equals(user.getId())) {
            return Result.error("无权修改其他用户信息");
        }
        
        User existingUser = userService.getById(userId);
        if (existingUser == null) {
            return Result.error("用户不存在");
        }
        
        // 只更新允许的字段
        existingUser.setNickname(user.getNickname());
        existingUser.setEmail(user.getEmail());
        existingUser.setAvatar(user.getAvatar());
        existingUser.setPhone(user.getPhone());
        // 店铺相关字段
        existingUser.setShopName(user.getShopName());
        existingUser.setLocation(user.getLocation());
        existingUser.setDescription(user.getDescription());
        existingUser.setTags(user.getTags());
        existingUser.setMainCategory(user.getMainCategory());
        existingUser.setBackground(user.getBackground());
        
        boolean success = userService.updateById(existingUser);
        if (success) {
            return Result.success(existingUser);
        }
        return Result.error("更新失败");
    }

    @PutMapping("/password")
    public Result<String> updatePassword(@RequestBody Map<String, String> params, @RequestHeader("Authorization") String token) {
        String jwt = token.replace("Bearer ", "");
        Long userId = jwtUtil.getUserId(jwt);

        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");

        if (oldPassword == null || oldPassword.isEmpty()) {
            return Result.error("原密码不能为空");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            return Result.error("新密码不能为空");
        }
        if (newPassword.length() < 6) {
            return Result.error("新密码长度不能少于6位");
        }

        User user = userService.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // 验证原密码
        if (!userService.verifyPassword(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }

        // 更新密码
        boolean success = userService.updatePassword(userId, newPassword);
        if (success) {
            return Result.success("密码修改成功");
        }
        return Result.error("密码修改失败");
    }
}
