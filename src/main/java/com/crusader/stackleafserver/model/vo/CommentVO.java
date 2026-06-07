package com.crusader.stackleafserver.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论响应 VO（支持楼中楼）
 */
@Data
public class CommentVO {

    private Long id;

    private Long articleId;

    private Long userId;

    /** 父评论ID，0 表示顶级评论 */
    private Long parentId;

    /** 被回复用户信息 */
    private UserVO replyUser;

    private String content;

    private LocalDateTime createTime;

    /** 评论者信息 */
    private UserVO user;

    /** 子评论列表（楼中楼） */
    private List<CommentVO> children;
}
