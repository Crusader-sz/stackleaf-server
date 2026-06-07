package com.crusader.stackleafserver.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类响应 VO
 */
@Data
public class CategoryVO {

    private Long id;

    private String name;

    private String description;

    private Integer sort;

    private LocalDateTime createTime;
}
