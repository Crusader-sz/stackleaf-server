package com.crusader.stackleafserver.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.exception.BusinessException;
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

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        String message = switch (e.getType()) {
            case NotLoginException.NOT_TOKEN -> MessageConstant.NOT_TOKEN;
            case NotLoginException.INVALID_TOKEN -> MessageConstant.INVALID_TOKEN;
            case NotLoginException.TOKEN_TIMEOUT -> MessageConstant.TOKEN_TIMEOUT;
            case NotLoginException.BE_REPLACED -> MessageConstant.TOKEN_BE_REPLACED;
            case NotLoginException.KICK_OUT -> MessageConstant.TOKEN_KICK_OUT;
            case NotLoginException.TOKEN_FREEZE -> MessageConstant.TOKEN_FREEZE;
            case NotLoginException.NO_PREFIX -> MessageConstant.TOKEN_NO_PREFIX;
            default -> MessageConstant.NOT_LOGIN;
        };
        log.warn("登录校验失败: type={}, message={}", e.getType(), message);
        return Result.error(401, message);
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("角色校验失败: role={}", e.getRole());
        return Result.error(403, MessageConstant.NOT_ROLE_PREFIX + e.getRole());
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限校验失败: permission={}", e.getPermission());
        return Result.error(403, MessageConstant.NOT_PERMISSION_PREFIX + e.getPermission());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
}
