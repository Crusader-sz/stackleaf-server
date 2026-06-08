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

    // ========== 文章相关 ==========
    public static final String ARTICLE_NOT_FOUND = "文章不存在";
    public static final String NO_PERMISSION_MODIFY_ARTICLE = "无权修改该文章";
    public static final String NO_PERMISSION_DELETE_ARTICLE = "无权删除该文章";
    public static final String ALREADY_LIKED = "已点赞该文章";
    public static final String ALREADY_FAVORITED = "已收藏该文章";

    // ========== 分类/标签相关 ==========
    public static final String CATEGORY_NAME_EXISTS = "分类名称已存在";
    public static final String CATEGORY_NOT_FOUND = "分类不存在";
    public static final String CATEGORY_HAS_ARTICLES = "该分类下存在文章，无法删除";
    public static final String TAG_NAME_EXISTS = "标签名称已存在";
    public static final String TAG_NOT_FOUND = "标签不存在";
    public static final String TAG_HAS_ARTICLES = "该标签已被文章引用，无法删除";

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
