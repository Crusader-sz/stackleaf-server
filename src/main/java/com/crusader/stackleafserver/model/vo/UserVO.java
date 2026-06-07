package com.crusader.stackleafserver.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息响应 VO（脱敏，不含密码）
 */
@Data
public class UserVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String email;

    private String bio;

    private String githubUrl;

    private String websiteUrl;

    private Integer role;

    private Integer followerCount;

    private Integer followingCount;

    private LocalDateTime createTime;
}
