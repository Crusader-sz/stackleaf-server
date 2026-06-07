package com.crusader.stackleafserver.enumeration;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {

    NORMAL(0, "普通用户"),
    ADMIN(1, "管理员");

    private final int code;
    private final String desc;

    UserRole(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
