package com.rural.ecommerce.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewVO {
    
    private Long id;
    
    private Long productId;
    
    private Long userId;
    
    private String userName;
    
    private String userAvatar;
    
    private Integer rating;
    
    private String content;
    
    private List<String> images;
    
    private String video;
    
    private LocalDateTime createTime;
    
    private String time;
    
    private Integer status;
}
