package com.rural.ecommerce.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewReportVO {
    
    private Long id;
    
    private Long reviewId;
    
    private Long reporterId;
    
    private String reporterName;
    
    private String reporterAvatar;
    
    private Integer reportType;
    
    private String reportContent;
    
    private Integer status;
    
    private String adminRemark;
    
    private LocalDateTime handleTime;
    
    private LocalDateTime createTime;
    
    private String createTimeStr;
    
    // 被举报评价信息
    private Long reviewUserId;
    
    private String reviewUserName;
    
    private String reviewUserAvatar;
    
    private Integer reviewRating;
    
    private String reviewContent;
    
    private List<String> reviewImages;
    
    private String reviewVideo;
    
    private LocalDateTime reviewCreateTime;
}
