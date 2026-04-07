package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

    /**
     * 获取两个用户之间的聊天记录
     */
    @Select("SELECT * FROM chat_message WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) OR (sender_id = #{targetId} AND receiver_id = #{userId}) ORDER BY create_time ASC LIMIT #{offset}, #{limit}")
    List<ChatMessage> selectChatHistory(@Param("userId") Long userId, @Param("targetId") Long targetId, @Param("offset") int offset, @Param("limit") int limit);

    /**
     * 获取两个用户之间的最后一条消息
     */
    @Select("SELECT * FROM chat_message WHERE (sender_id = #{userId} AND receiver_id = #{targetId}) OR (sender_id = #{targetId} AND receiver_id = #{userId}) ORDER BY create_time DESC LIMIT 1")
    ChatMessage selectLastMessage(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 标记消息为已读
     */
    @Update("UPDATE chat_message SET is_read = 1 WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND is_read = 0")
    int markAsRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);

    /**
     * 获取用户未读消息总数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE receiver_id = #{userId} AND is_read = 0")
    int selectUnreadCount(@Param("userId") Long userId);

    /**
     * 获取两个用户之间的未读消息数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE sender_id = #{senderId} AND receiver_id = #{receiverId} AND is_read = 0")
    int selectUnreadCountBetweenUsers(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
