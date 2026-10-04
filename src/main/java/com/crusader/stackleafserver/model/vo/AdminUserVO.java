package com.crusader.stackleafserver.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AdminUserVO extends UserVO {
    private String email;
    private Integer status;
}
