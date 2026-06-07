package com.crusader.stackleafserver.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 创建评论请求 DTO
 */
@Data
public class CommentCreateDTO {

    @NotNull(message = "文章ID不能为空")
    private Long articleId;

    /** 父评论ID，0 或 null 表示顶级评论 */
    private Long parentId;

    /** 被回复用户ID，顶级评论时为 null */
    private Long replyUserId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
