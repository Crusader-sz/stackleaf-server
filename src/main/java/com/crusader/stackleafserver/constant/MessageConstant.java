package com.crusader.stackleafserver.constant;

/**
 * 信息提示常量类
 */
public class MessageConstant {

    public static final String SUCCESS = "成功";
    public static final String ERROR = "失败";

    // ========== 用户相关 ==========
    public static final String USERNAME_EXISTS = "用户名已存在";
    public static final String EMAIL_EXISTS = "邮箱已被注册";
    public static final String USERNAME_OR_PASSWORD_ERROR = "用户名或密码错误";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String TARGET_USER_NOT_FOUND = "目标用户不存在";
    public static final String CANNOT_FOLLOW_SELF = "不能关注自己";
    public static final String ALREADY_FOLLOWED = "已关注该用户";
    public static final String NOT_FOLLOWED = "未关注该用户";

    // ========== 验证码相关 ==========
    public static final String VERIFICATION_CODE_EXPIRED = "验证码错误或已过期";
    public static final String EMAIL_NOT_REGISTERED = "该邮箱未注册";
    public static final String VERIFICATION_CODE_RATE_LIMIT = "验证码发送过于频繁，请稍后再试";

    // ========== Token 相关 ==========
    public static final String NOT_TOKEN = "未能读取到有效 Token";
    public static final String INVALID_TOKEN = "Token 无效";
    public static final String TOKEN_TIMEOUT = "Token 已过期";
    public static final String TOKEN_BE_REPLACED = "账号已在别处登录";
    public static final String TOKEN_KICK_OUT = "已被踢下线";
    public static final String TOKEN_FREEZE = "Token 已被冻结";
    public static final String TOKEN_NO_PREFIX = "未按照指定前缀提交 Token";
    public static final String NOT_LOGIN = "当前会话未登录";

    public static final String NOT_ROLE_PREFIX = "缺少角色：";
    public static final String NOT_PERMISSION_PREFIX = "缺少权限：";

    private MessageConstant() {}
}
