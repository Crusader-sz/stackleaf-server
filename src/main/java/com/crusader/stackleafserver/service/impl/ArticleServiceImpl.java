package com.crusader.stackleafserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.mapper.*;
import com.crusader.stackleafserver.model.dto.ArticleCreateDTO;
import com.crusader.stackleafserver.model.dto.ArticleQueryDTO;
import com.crusader.stackleafserver.model.dto.ArticleUpdateDTO;
import com.crusader.stackleafserver.model.entity.*;
import com.crusader.stackleafserver.model.vo.*;
import com.crusader.stackleafserver.service.ArticleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文章业务实现类
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private ArticleTagMapper articleTagMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(ArticleCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setAuthorId(userId);
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setCommentCount(0);
        baseMapper.insert(article);

        saveArticleTags(article.getId(), dto.getTagIds());
        return article.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(ArticleUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        Article article = baseMapper.selectById(dto.getId());
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权修改该文章");
        }

        BeanUtils.copyProperties(dto, article, "id", "authorId", "viewCount",
                "likeCount", "favoriteCount", "commentCount");
        baseMapper.updateById(article);

        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                .eq(ArticleTag::getArticleId, article.getId()));
        saveArticleTags(article.getId(), dto.getTagIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Article article = baseMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }
        if (!article.getAuthorId().equals(userId)) {
            throw new RuntimeException("无权删除该文章");
        }

        baseMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id));
        articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getArticleId, id));
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        Article article = baseMapper.selectById(id);
        if (article == null) {
            throw new RuntimeException("文章不存在");
        }

        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        vo.setAuthor(getUserVO(article.getAuthorId()));
        vo.setCategory(getCategoryVO(article.getCategoryId()));
        vo.setTags(getTagVOListByArticleId(id));

        // 当前用户是否已点赞/收藏
        try {
            Long userId = StpUtil.getLoginIdAsLong();
            vo.setIsLiked(articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, userId)) > 0);
            vo.setIsFavorited(articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getArticleId, id).eq(ArticleFavorite::getUserId, userId)) > 0);
        } catch (Exception e) {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
        }

        return vo;
    }

    @Override
    public Page<ArticleVO> pageArticles(ArticleQueryDTO dto) {
        Page<Article> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(dto.getKeyword() != null, Article::getTitle, dto.getKeyword())
                .eq(dto.getCategoryId() != null, Article::getCategoryId, dto.getCategoryId())
                .eq(dto.getStatus() != null, Article::getStatus, dto.getStatus())
                .orderByDesc(Article::getIsTop)
                .orderByDesc(Article::getCreateTime);

        if (dto.getTagId() != null) {
            List<ArticleTag> articleTags = articleTagMapper.selectList(
                    new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, dto.getTagId()));
            if (CollectionUtils.isEmpty(articleTags)) {
                return new Page<>(dto.getPageNum(), dto.getPageSize(), 0);
            }
            List<Long> articleIds = articleTags.stream().map(ArticleTag::getArticleId).collect(Collectors.toList());
            wrapper.in(Article::getId, articleIds);
        }

        Page<Article> articlePage = baseMapper.selectPage(page, wrapper);
        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        List<ArticleVO> voList = new ArrayList<>();
        for (Article article : articlePage.getRecords()) {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(article, vo);
            vo.setAuthor(getUserVO(article.getAuthorId()));
            vo.setCategory(getCategoryVO(article.getCategoryId()));
            vo.setTags(getTagVOListByArticleId(article.getId()));
            voList.add(vo);
        }
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeArticle(Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Article article = baseMapper.selectById(articleId);
        if (article == null) { throw new RuntimeException("文章不存在"); }

        Long count = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId).eq(ArticleLike::getUserId, userId));
        if (count > 0) { throw new RuntimeException("已点赞该文章"); }

        ArticleLike like = new ArticleLike();
        like.setArticleId(articleId);
        like.setUserId(userId);
        articleLikeMapper.insert(like);

        baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId).setSql("like_count = like_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeArticle(Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        int deleted = articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId).eq(ArticleLike::getUserId, userId));
        if (deleted > 0) {
            baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId).setSql("like_count = GREATEST(like_count - 1, 0)"));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoriteArticle(Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        Article article = baseMapper.selectById(articleId);
        if (article == null) { throw new RuntimeException("文章不存在"); }

        Long count = articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId));
        if (count > 0) { throw new RuntimeException("已收藏该文章"); }

        ArticleFavorite fav = new ArticleFavorite();
        fav.setArticleId(articleId);
        fav.setUserId(userId);
        articleFavoriteMapper.insert(fav);

        baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId).setSql("favorite_count = favorite_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoriteArticle(Long articleId) {
        Long userId = StpUtil.getLoginIdAsLong();
        int deleted = articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId));
        if (deleted > 0) {
            baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId).setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
        }
    }

    // ==================== private ====================

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) { return; }
        for (Long tagId : tagIds) {
            ArticleTag at = new ArticleTag();
            at.setArticleId(articleId);
            at.setTagId(tagId);
            articleTagMapper.insert(at);
        }
    }

    private List<TagVO> getTagVOListByArticleId(Long articleId) {
        List<ArticleTag> ats = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        if (CollectionUtils.isEmpty(ats)) { return Collections.emptyList(); }
        List<Long> tagIds = ats.stream().map(ArticleTag::getTagId).collect(Collectors.toList());
        return tagMapper.selectBatchIds(tagIds).stream().map(t -> {
            TagVO vo = new TagVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    private UserVO getUserVO(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) { return null; }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    private CategoryVO getCategoryVO(Long categoryId) {
        Category cat = categoryMapper.selectById(categoryId);
        if (cat == null) { return null; }
        CategoryVO vo = new CategoryVO();
        BeanUtils.copyProperties(cat, vo);
        return vo;
    }
}
