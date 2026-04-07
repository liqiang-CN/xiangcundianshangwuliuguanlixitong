package com.rural.ecommerce.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF
            .csrf().disable()
            // 禁用 session
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            // 添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            // 配置请求授权
            .authorizeRequests()
            // 允许公开访问的接口
            .antMatchers("/user/login", "/user/register").permitAll()
            .antMatchers("/admin/login").permitAll()
            .antMatchers("/admin/config").permitAll()
            .antMatchers("/product/**").permitAll()
            .antMatchers("/category/**").permitAll()
            .antMatchers("/shop/**").permitAll()
            .antMatchers("/upload").permitAll()
            .antMatchers("/uploads/**").permitAll()
            .antMatchers("/file/upload").permitAll()
            // 其他请求需要认证
            .anyRequest().authenticated()
            .and()
            // 禁用默认的登录页面
            .httpBasic().disable()
            .formLogin().disable();
        
        return http.build();
    }
}
