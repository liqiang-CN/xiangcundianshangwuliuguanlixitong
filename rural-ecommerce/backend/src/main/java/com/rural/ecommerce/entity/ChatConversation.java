package com.rural.ecommerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    private Integer targetType;

    private Long lastMessageId;

    private LocalDateTime lastMessageTime;

    private Integer unreadCount;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
