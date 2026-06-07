package com.crusader.stackleafserver.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章列表项响应 VO（不含正文详情）
 */
@Data
public class ArticleVO {

    private Long id;

    private String title;

    private String summary;

    private String coverImg;

    private Integer status;

    private Integer isTop;

    private Integer viewCount;

    private Integer likeCount;

    private Integer favoriteCount;

    private Integer commentCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /** 作者信息 */
    private UserVO author;

    /** 分类信息 */
    private CategoryVO category;

    /** 标签列表 */
    private List<TagVO> tags;
}
