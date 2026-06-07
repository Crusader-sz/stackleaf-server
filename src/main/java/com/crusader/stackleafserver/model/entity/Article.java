package com.crusader.stackleafserver.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章实体
 */
@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long authorId;

    private Long categoryId;

    private String title;

    private String summary;

    /** 文章正文，Markdown 格式 */
    private String content;

    private String coverImg;

    /** 状态: 0-草稿 1-已发布 2-下架 */
    private Integer status;

    /** 是否置顶: 0-否 1-是 */
    private Integer isTop;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
