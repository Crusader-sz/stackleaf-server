package com.crusader.stackleafserver.model.dto;

import lombok.Data;

/**
 * 文章分页查询 DTO
 */
@Data
public class ArticleQueryDTO {

    /** 当前页码，默认 1 */
    private Integer pageNum = 1;

    /** 每页条数，默认 10 */
    private Integer pageSize = 10;

    /** 分类ID */
    private Long categoryId;

    /** 标签ID */
    private Long tagId;

    /** 关键词（标题模糊搜索） */
    private String keyword;

    /** 状态: 0-草稿 1-已发布 2-下架 */
    private Integer status;
}
