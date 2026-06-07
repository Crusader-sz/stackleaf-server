package com.crusader.stackleafserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体（支持楼中楼）
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long userId;

    /** 父评论ID，0 表示顶级评论 */
    private Long parentId;

    /** 被回复用户ID，顶级评论时为 null */
    private Long replyUserId;

    private String content;

    /** 状态: 0-隐藏 1-正常 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
