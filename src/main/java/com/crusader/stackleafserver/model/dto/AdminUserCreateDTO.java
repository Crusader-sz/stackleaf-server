package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserCreateDTO extends UserRegisterDTO {
    @NotNull(message = "角色不能为空")
    @Min(0) @Max(1)
    private Integer role = 0;
    @NotNull(message = "状态不能为空")
    @Min(0) @Max(1)
    private Integer status = 1;
}
