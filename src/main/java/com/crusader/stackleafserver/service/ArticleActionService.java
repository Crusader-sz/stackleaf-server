package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.model.vo.ArticleVO;
import com.crusader.stackleafserver.model.vo.UserVO;

import java.util.List;

/**
 * 文章交互业务接口（点赞、收藏、关注）
 */
public interface ArticleActionService {

    /**
     * 查询用户点赞的文章列表
     */
    Page<ArticleVO> getLikedArticles(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户收藏的文章列表
     */
    Page<ArticleVO> getFavoritedArticles(Long userId, Integer pageNum, Integer pageSize);

    /**
     * 查询用户的关注列表
     */
    List<UserVO> getFollowingList(Long userId);

    /**
     * 查询用户的粉丝列表
     */
    List<UserVO> getFollowerList(Long userId);
}
