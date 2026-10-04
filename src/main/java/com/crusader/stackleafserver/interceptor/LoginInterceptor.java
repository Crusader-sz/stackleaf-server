package com.crusader.stackleafserver.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.crusader.stackleafserver.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器 - 基于 Sa-Token 进行登录校验
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @org.springframework.beans.factory.annotation.Autowired
    private com.crusader.stackleafserver.service.support.UserAccess userAccess;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    private boolean isPublic(HttpServletRequest request) {
        String path = new org.springframework.web.util.UrlPathHelper().getPathWithinApplication(request);
        if (path.equals("/error") || pathMatcher.match("/swagger-ui/**", path)
                || pathMatcher.match("/v3/api-docs/**", path) || pathMatcher.match("/webjars/**", path)
                || path.equals("/swagger-ui.html") || pathMatcher.match("/doc.html/**", path)) {
            return true;
        }
        if ("POST".equals(request.getMethod())) {
            return java.util.Set.of("/user/login", "/user/register", "/user/sendVerificationCode",
                    "/user/resetUserPassword").contains(path);
        }
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        return java.util.Set.of("/article/page", "/category/list", "/tag/list", "/comment/top").contains(path)
                || path.matches("/article/[0-9]+") || path.matches("/comment/children/[0-9]+")
                || path.matches("/user/(profile|following|followers)/[0-9]+");
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 放行 CORS 预检请求
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(request)) {
            return true;
        }

        // Sa-Token 校验登录，未登录会抛出 NotLoginException
        StpUtil.checkLogin();
        userAccess.currentUser();

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
