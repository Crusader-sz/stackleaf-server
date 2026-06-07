package com.crusader.stackleafserver.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证接口
                        "/user/login", "/user/logout", "/user/register",
                        "/user/sendVerificationCode", "/user/resetUserPassword",
                        // 公开接口 - 文章列表/详情
                        "/article/page", "/article/{id}",
                        // 公开接口 - 评论列表
                        "/comment/top", "/comment/children/**",
                        // 公开接口 - 分类/标签列表
                        "/category/list", "/tag/list",
                        // 公开接口 - 用户主页
                        "/user/{id}", "/user/following/**", "/user/followers/**",
                        // Swagger / Knife4j
                        "/swagger-ui/**", "/v3/api-docs/**", "/doc.html/**"
                );
    }
}
