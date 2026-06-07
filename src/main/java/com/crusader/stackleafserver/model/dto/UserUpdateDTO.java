package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新请求 DTO
 */
@Data
public class UserUpdateDTO {

    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;

    private String avatar;

    @Size(max = 300, message = "个人简介长度不能超过 300 个字符")
    private String bio;

    private String githubUrl;

    private String websiteUrl;
}
