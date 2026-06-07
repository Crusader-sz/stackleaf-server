package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 更新文章请求 DTO
 */
@Data
public class ArticleUpdateDTO {

    @NotNull(message = "文章ID不能为空")
    private Long id;

    @Size(max = 200, message = "标题长度不能超过 200 个字符")
    private String title;

    @Size(max = 500, message = "摘要长度不能超过 500 个字符")
    private String summary;

    private String content;

    private String coverImg;

    private Long categoryId;

    private List<Long> tagIds;

    private Integer status;

    private Integer isTop;
}
