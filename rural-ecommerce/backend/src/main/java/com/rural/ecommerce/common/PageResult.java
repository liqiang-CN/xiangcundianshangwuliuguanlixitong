package com.rural.ecommerce.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    
    private Long total;
    private List<T> list;
    
    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }
}
