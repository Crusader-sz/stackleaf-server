package com.crusader.stackleafserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.mapper.*;
import com.crusader.stackleafserver.model.entity.*;
import com.crusader.stackleafserver.model.vo.ArticleVO;
import com.crusader.stackleafserver.model.vo.CategoryVO;
import com.crusader.stackleafserver.model.vo.TagVO;
import com.crusader.stackleafserver.model.vo.UserVO;
import com.crusader.stackleafserver.service.ArticleActionService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章交互业务实现类（点赞、收藏、关注）
 */
@Service
public class ArticleActionServiceImpl implements ArticleActionService {

    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private ArticleMapper articleMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private TagMapper tagMapper;
    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Override
    public Page<ArticleVO> getLikedArticles(Long userId, Integer pageNum, Integer pageSize) {
        Page<ArticleLike> likePage = articleLikeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ArticleLike>()
                        .eq(ArticleLike::getUserId, userId)
                        .orderByDesc(ArticleLike::getCreateTime));

        return toArticleVOPage(likePage, pageNum, pageSize,
                likePage.getRecords().stream().map(ArticleLike::getArticleId).collect(Collectors.toList()));
    }

    @Override
    public Page<ArticleVO> getFavoritedArticles(Long userId, Integer pageNum, Integer pageSize) {
        Page<ArticleFavorite> favPage = articleFavoriteMapper.selectPage(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<ArticleFavorite>()
                        .eq(ArticleFavorite::getUserId, userId)
                        .orderByDesc(ArticleFavorite::getCreateTime));

        return toArticleVOPage(favPage, pageNum, pageSize,
                favPage.getRecords().stream().map(ArticleFavorite::getArticleId).collect(Collectors.toList()));
    }

    @Override
    public List<UserVO> getFollowingList(Long userId) {
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        if (follows.isEmpty()) { return Collections.emptyList(); }
        return toUserVOList(follows.stream().map(UserFollow::getFollowUserId).collect(Collectors.toList()));
    }

    @Override
    public List<UserVO> getFollowerList(Long userId) {
        List<UserFollow> follows = userFollowMapper.selectList(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getFollowUserId, userId)
                        .orderByDesc(UserFollow::getCreateTime));
        if (follows.isEmpty()) { return Collections.emptyList(); }
        return toUserVOList(follows.stream().map(UserFollow::getUserId).collect(Collectors.toList()));
    }

    // ==================== private ====================

    private Page<ArticleVO> toArticleVOPage(Page<?> sourcePage, int pageNum, int pageSize, List<Long> articleIds) {
        if (articleIds.isEmpty()) { return new Page<>(pageNum, pageSize, 0); }
        List<Article> articles = articleMapper.selectBatchIds(articleIds);
        List<ArticleVO> voList = articles.stream().map(this::convertToArticleVO).collect(Collectors.toList());
        Page<ArticleVO> voPage = new Page<>(pageNum, pageSize, sourcePage.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    private List<UserVO> toUserVOList(List<Long> userIds) {
        return userMapper.selectBatchIds(userIds).stream().map(u -> {
            UserVO vo = new UserVO();
            BeanUtils.copyProperties(u, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private ArticleVO convertToArticleVO(Article article) {
        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(article, vo);

        User author = userMapper.selectById(article.getAuthorId());
        if (author != null) {
            UserVO authorVO = new UserVO();
            BeanUtils.copyProperties(author, authorVO);
            vo.setAuthor(authorVO);
        }
        if (article.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(article.getCategoryId());
            if (cat != null) {
                CategoryVO catVO = new CategoryVO();
                BeanUtils.copyProperties(cat, catVO);
                vo.setCategory(catVO);
            }
        }

        List<ArticleTag> ats = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId()));
        if (!CollectionUtils.isEmpty(ats)) {
            List<Long> tagIds = ats.stream().map(ArticleTag::getTagId).collect(Collectors.toList());
            vo.setTags(tagMapper.selectBatchIds(tagIds).stream().map(t -> {
                TagVO tagVO = new TagVO();
                BeanUtils.copyProperties(t, tagVO);
                return tagVO;
            }).collect(Collectors.toList()));
        } else {
            vo.setTags(Collections.emptyList());
        }
        return vo;
    }
}
