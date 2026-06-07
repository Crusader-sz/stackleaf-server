package com.crusader.stackleafserver.model.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章详情响应 VO（包含正文内容）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ArticleDetailVO extends ArticleVO {

    /** 文章正文，Markdown 格式 */
    private String content;

    /** 当前用户是否已点赞 */
    private Boolean isLiked;

    /** 当前用户是否已收藏 */
    private Boolean isFavorited;
}
