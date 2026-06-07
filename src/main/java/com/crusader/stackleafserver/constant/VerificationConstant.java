package com.crusader.stackleafserver.constant;

/**
 * 验证码相关常量
 */
public class VerificationConstant {

    /** Redis key 前缀: 验证码 */
    public static final String CODE_KEY_PREFIX = "verification:email:";

    /** Redis key 前缀: 发送频率限制 */
    public static final String LIMIT_KEY_PREFIX = "verification:limit:";

    /** 验证码有效期（分钟） */
    public static final long CODE_EXPIRE_MINUTES = 5;

    /** 发送频率限制（秒） */
    public static final long LIMIT_EXPIRE_SECONDS = 60;

    private VerificationConstant() {}
}
