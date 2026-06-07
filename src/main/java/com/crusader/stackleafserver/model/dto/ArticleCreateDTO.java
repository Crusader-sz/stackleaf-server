package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 创建文章请求 DTO
 */
@Data
public class ArticleCreateDTO {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500 个字符")
    private String summary;

    @NotBlank(message = "文章内容不能为空")
    private String content;

    private String coverImg;

    @NotNull(message = "分类不能为空")
    private Long categoryId;

    /** 标签ID列表 */
    private List<Long> tagIds;

    /** 状态: 0-草稿 1-发布 */
    private Integer status;
}
