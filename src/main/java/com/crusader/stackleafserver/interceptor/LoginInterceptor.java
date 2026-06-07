package com.crusader.stackleafserver.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.crusader.stackleafserver.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 - 基于 Sa-Token 进行登录校验
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // Sa-Token 校验登录，未登录会抛出 NotLoginException
        StpUtil.checkLogin();

        // 将当前登录用户信息存入 ThreadLocal，供业务层使用
        ThreadLocalUtil.set(StpUtil.getLoginIdAsLong());

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除 ThreadLocal，防止内存泄漏
        ThreadLocalUtil.remove();
    }
}
