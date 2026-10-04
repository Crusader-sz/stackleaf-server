package com.crusader.stackleafserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
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
import com.crusader.stackleafserver.service.support.UserAccess;
import com.crusader.stackleafserver.service.support.ArticleAccess;
import java.util.Objects;
import java.util.LinkedHashSet;

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
    private UserAccess userAccess;
    @Autowired
    private ArticleAccess articleAccess;

    @Autowired
    private ArticleTagMapper articleTagMapper;
    @Autowired
    private ArticleLikeMapper articleLikeMapper;
    @Autowired
    private ArticleFavoriteMapper articleFavoriteMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private TagMapper tagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(ArticleCreateDTO dto) {
        Long userId = userAccess.currentUser().getId();
        validateStatus(dto.getStatus());
        if (Integer.valueOf(2).equals(dto.getStatus())) { userAccess.requireAdmin(); }
        validateReferences(dto.getCategoryId(), dto.getTagIds());

        Article article = new Article();
        BeanUtils.copyProperties(dto, article);
        article.setAuthorId(userId);
        article.setStatus(dto.getStatus() == null ? 0 : dto.getStatus());
        article.setIsTop(0);
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
        User user = userAccess.currentUser();
        Article article = articleAccess.lock(dto.getId());
        boolean admin = userAccess.isAdmin(user);
        if (!admin && !Objects.equals(article.getAuthorId(), user.getId())) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.NO_PERMISSION_MODIFY_ARTICLE);
        }
        validateStatus(dto.getStatus());
        if ((dto.getTitle() != null && dto.getTitle().isBlank())
                || (dto.getContent() != null && dto.getContent().isBlank())) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.INVALID_PARAMETER);
        }
        if (dto.getIsTop() != null && dto.getIsTop() != 0 && dto.getIsTop() != 1) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.INVALID_PARAMETER);
        }
        if (!admin && (dto.getIsTop() != null || Integer.valueOf(2).equals(dto.getStatus())
                || (Integer.valueOf(2).equals(article.getStatus()) && dto.getStatus() != null
                && !Objects.equals(dto.getStatus(), article.getStatus())))) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.ARTICLE_MODERATION_REQUIRED);
        }
        validateReferences(dto.getCategoryId(), dto.getTagIds());
        if (dto.getTitle() != null) { article.setTitle(dto.getTitle()); }
        if (dto.getSummary() != null) { article.setSummary(dto.getSummary()); }
        if (dto.getContent() != null) { article.setContent(dto.getContent()); }
        if (dto.getCoverImg() != null) { article.setCoverImg(dto.getCoverImg()); }
        if (dto.getCategoryId() != null) { article.setCategoryId(dto.getCategoryId()); }
        if (dto.getStatus() != null) { article.setStatus(dto.getStatus()); }
        if (dto.getIsTop() != null) { article.setIsTop(dto.getIsTop()); }
        baseMapper.updateById(article);

        if (dto.getTagIds() != null) {
            articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>()
                    .eq(ArticleTag::getArticleId, article.getId()));
            saveArticleTags(article.getId(), dto.getTagIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteArticle(Long id) {
        User user = userAccess.currentUser();
        Article article = articleAccess.lock(id);
        if (!userAccess.isAdmin(user) && !Objects.equals(article.getAuthorId(), user.getId())) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.NO_PERMISSION_DELETE_ARTICLE);
        }

        baseMapper.deleteById(id);
        articleTagMapper.delete(new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, id));
        articleLikeMapper.delete(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, id));
        articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getArticleId, id));
        commentMapper.delete(new LambdaQueryWrapper<Comment>().eq(Comment::getArticleId, id));
    }

    @Override
    public ArticleDetailVO getArticleDetail(Long id) {
        Article article = articleAccess.requirePublished(id);
        baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, id).setSql("view_count = view_count + 1"));
        article.setViewCount(article.getViewCount() + 1);
        return toDetail(article);
    }

    @Override
    public ArticleDetailVO getOwnedArticleDetail(Long id) {
        Long userId = userAccess.currentUser().getId();
        Article article = baseMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getId, id).eq(Article::getAuthorId, userId));
        if (article == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.ARTICLE_NOT_FOUND);
        }
        return toDetail(article);
    }

    @Override
    public ArticleDetailVO getAdminArticleDetail(Long id) {
        userAccess.requireAdmin();
        Article article = baseMapper.selectById(id);
        if (article == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.ARTICLE_NOT_FOUND);
        }
        return toDetail(article);
    }

    private ArticleDetailVO toDetail(Article article) {
        Long id = article.getId();
        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        vo.setViewCount(article.getViewCount());
        vo.setAuthor(getUserVO(article.getAuthorId()));
        vo.setCategory(getCategoryVO(article.getCategoryId()));
        vo.setTags(getTagVOListByArticleId(id));

        // 当前用户是否已点赞/收藏
        if (StpUtil.isLogin()) {
            Long userId = userAccess.currentUser().getId();
            vo.setIsLiked(articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                    .eq(ArticleLike::getArticleId, id).eq(ArticleLike::getUserId, userId)) > 0);
            vo.setIsFavorited(articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                    .eq(ArticleFavorite::getArticleId, id).eq(ArticleFavorite::getUserId, userId)) > 0);
        } else {
            vo.setIsLiked(false);
            vo.setIsFavorited(false);
        }

        return vo;
    }

    @Override
    public Page<ArticleVO> pageArticles(ArticleQueryDTO dto) {
        return queryArticles(dto, null, 1);
    }

    @Override
    public Page<ArticleVO> pageOwnedArticles(ArticleQueryDTO dto) {
        return queryArticles(dto, userAccess.currentUser().getId(), dto.getStatus());
    }

    @Override
    public Page<ArticleVO> pageAdminArticles(ArticleQueryDTO dto) {
        userAccess.requireAdmin();
        return queryArticles(dto, null, dto.getStatus());
    }

    private Page<ArticleVO> queryArticles(ArticleQueryDTO dto, Long authorId, Integer status) {
        Page<Article> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(dto.getKeyword() != null, Article::getTitle, dto.getKeyword())
                .eq(dto.getCategoryId() != null, Article::getCategoryId, dto.getCategoryId())
                .eq(status != null, Article::getStatus, status)
                .eq(authorId != null, Article::getAuthorId, authorId)
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
        Long userId = userAccess.currentUser().getId();
        articleAccess.checkPublished(articleAccess.lock(articleId));

        Long count = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId).eq(ArticleLike::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.ALREADY_LIKED);
        }

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
        Long userId = userAccess.currentUser().getId();
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
        Long userId = userAccess.currentUser().getId();
        articleAccess.checkPublished(articleAccess.lock(articleId));

        Long count = articleFavoriteMapper.selectCount(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId));
        if (count > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.ALREADY_FAVORITED);
        }

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
        Long userId = userAccess.currentUser().getId();
        int deleted = articleFavoriteMapper.delete(new LambdaQueryWrapper<ArticleFavorite>()
                .eq(ArticleFavorite::getArticleId, articleId).eq(ArticleFavorite::getUserId, userId));
        if (deleted > 0) {
            baseMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId).setSql("favorite_count = GREATEST(favorite_count - 1, 0)"));
        }
    }

    // ==================== private ====================

    private void validateStatus(Integer status) {
        if (status != null && (status < 0 || status > 2)) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.ARTICLE_STATUS_INVALID);
        }
    }

    private void validateReferences(Long categoryId, List<Long> tagIds) {
        if (categoryId != null && categoryMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, categoryId).last("FOR SHARE")) == null) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.CATEGORY_NOT_FOUND);
        }
        if (!CollectionUtils.isEmpty(tagIds)) {
            if (tagIds.stream().anyMatch(id -> id == null || id <= 0)) {
                throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.TAG_IDS_INVALID);
            }
            for (Long id : tagIds.stream().distinct().sorted().toList()) {
                if (tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getId, id).last("FOR SHARE")) == null) {
                    throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.TAG_IDS_INVALID);
                }
            }
        }
    }

    private void saveArticleTags(Long articleId, List<Long> tagIds) {
        if (CollectionUtils.isEmpty(tagIds)) { return; }
        for (Long tagId : new LinkedHashSet<>(tagIds)) {
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
