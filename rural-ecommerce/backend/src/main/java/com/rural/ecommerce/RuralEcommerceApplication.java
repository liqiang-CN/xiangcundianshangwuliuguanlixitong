package com.rural.ecommerce;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.rural.ecommerce.mapper", "com.rural.ecommerce.repository"})
public class RuralEcommerceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RuralEcommerceApplication.class, args);
    }
}
