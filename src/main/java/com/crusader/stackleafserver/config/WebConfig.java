package com.crusader.stackleafserver.config;

import com.crusader.stackleafserver.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证接口
                        "/user/login", "/user/logout", "/user/register",
                        "/user/sendVerificationCode", "/user/resetUserPassword",
                        // 公开接口 - 用户主页/关注/粉丝
                        "/user/profile/*", "/user/following/*", "/user/followers/*",
                        // 公开接口 - 文章
                        "/article/page", "/article/*",
                        // 公开接口 - 评论
                        "/comment/top", "/comment/children/**",
                        // 公开接口 - 分类/标签
                        "/category/list", "/tag/list",
                        // 错误页面
                        "/error",
                        // Swagger / Knife4j
                        "/swagger-ui/**", "/v3/api-docs/**", "/doc.html/**",
                        "/webjars/**"
                );
    }
}
