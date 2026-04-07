package com.rural.ecommerce.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.UserMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMessageRepository extends BaseMapper<UserMessage> {
    
    @Select("SELECT * FROM user_message WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<UserMessage> findByUserId(Long userId);
    
    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND is_read = 0")
    int countUnreadByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND is_read = 0 AND type = #{type}")
    int countUnreadByType(@Param("userId") Long userId, @Param("type") Integer type);
    
    @Update("UPDATE user_message SET is_read = 1 WHERE user_id = #{userId} AND is_read = 0")
    int markAllAsRead(Long userId);
}
