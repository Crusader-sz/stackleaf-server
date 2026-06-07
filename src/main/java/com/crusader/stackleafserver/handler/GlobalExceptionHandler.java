package com.crusader.stackleafserver.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.crusader.stackleafserver.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 未登录 / token 异常
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        String message = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> "未能读取到有效 Token";
            case NotLoginException.INVALID_TOKEN -> "Token 无效";
            case NotLoginException.TOKEN_TIMEOUT -> "Token 已过期";
            case NotLoginException.BE_REPLACED -> "账号已在别处登录";
            case NotLoginException.KICK_OUT -> "已被踢下线";
            case NotLoginException.TOKEN_FREEZE -> "Token 已被冻结";
            case NotLoginException.NO_PREFIX -> "未按照指定前缀提交 Token";
            default -> "当前会话未登录";
        };
        log.warn("登录校验失败: type={}, message={}", e.getType(), message);
        return Result.error(401, message);
    }

    /**
     * 缺少角色
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("角色校验失败: role={}", e.getRole());
        return Result.error(403, "缺少角色：" + e.getRole());
    }

    /**
     * 缺少权限
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限校验失败: permission={}", e.getPermission());
        return Result.error(403, "缺少权限：" + e.getPermission());
    }
}
