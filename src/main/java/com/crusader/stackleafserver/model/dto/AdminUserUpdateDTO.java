package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminUserUpdateDTO {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称长度不能超过 50 个字符")
    private String nickname;
    @Email(message = "邮箱格式不正确")
    @Size(max = 100)
    private String email;
    @Size(min = 6, max = 50, message = "密码长度 6-50 个字符")
    @Pattern(regexp = "(?s).*\\S.*", message = "密码不能全为空白")
    private String password;
    @NotNull(message = "角色不能为空")
    @Min(0) @Max(1)
    private Integer role;
    @NotNull(message = "状态不能为空")
    @Min(0) @Max(1)
    private Integer status;
}
