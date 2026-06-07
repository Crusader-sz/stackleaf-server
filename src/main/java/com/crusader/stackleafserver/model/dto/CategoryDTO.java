package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 分类创建/更新请求 DTO
 */
@Data
public class CategoryDTO {

    /** 更新时传入 */
    private Long id;

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过 50 个字符")
    private String name;

    @Size(max = 200, message = "分类描述长度不能超过 200 个字符")
    private String description;

    private Integer sort;
}
