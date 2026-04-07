package com.rural.ecommerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rural.ecommerce.entity.ChatConversation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {

    /**
     * 获取用户的所有会话
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} ORDER BY last_message_time DESC")
    List<ChatConversation> selectByUserId(@Param("userId") Long userId);

    /**
     * 获取用户与特定目标的会话
     */
    @Select("SELECT * FROM chat_conversation WHERE user_id = #{userId} AND target_id = #{targetId}")
    ChatConversation selectByUserIdAndTargetId(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 获取用户与特定目标的会话（别名方法）
     */
    default ChatConversation selectByUserAndTarget(Long userId, Long targetId) {
        return selectByUserIdAndTargetId(userId, targetId);
    }

    /**
     * 获取用户总未读消息数
     */
    @Select("SELECT COALESCE(SUM(unread_count), 0) FROM chat_conversation WHERE user_id = #{userId}")
    int selectTotalUnreadCount(@Param("userId") Long userId);

    /**
     * 重置会话未读数
     */
    @Update("UPDATE chat_conversation SET unread_count = 0 WHERE user_id = #{userId} AND target_id = #{targetId}")
    int resetUnreadCount(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 清除未读数（别名方法，与resetUnreadCount功能相同）
     */
    default int clearUnreadCount(Long userId, Long targetId) {
        return resetUnreadCount(userId, targetId);
    }

    /**
     * 增加未读数
     */
    @Update("UPDATE chat_conversation SET unread_count = unread_count + 1 WHERE user_id = #{userId} AND target_id = #{targetId}")
    int incrementUnreadCount(@Param("userId") Long userId, @Param("targetId") Long targetId);

    /**
     * 标记所有会话为已读
     */
    @Update("UPDATE chat_conversation SET unread_count = 0 WHERE user_id = #{userId}")
    int markAllAsRead(@Param("userId") Long userId);

    /**
     * 删除会话
     */
    @Delete("DELETE FROM chat_conversation WHERE user_id = #{userId} AND target_id = #{targetId}")
    int deleteByUserIdAndTargetId(@Param("userId") Long userId, @Param("targetId") Long targetId);
}
