package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crusader.stackleafserver.model.dto.ArticleCreateDTO;
import com.crusader.stackleafserver.model.dto.ArticleQueryDTO;
import com.crusader.stackleafserver.model.dto.ArticleUpdateDTO;
import com.crusader.stackleafserver.model.entity.Article;
import com.crusader.stackleafserver.model.vo.ArticleDetailVO;
import com.crusader.stackleafserver.model.vo.ArticleVO;

/**
 * 文章业务接口
 */
public interface ArticleService extends IService<Article> {

    /**
     * 创建文章
     */
    Long createArticle(ArticleCreateDTO dto);

    /**
     * 更新文章
     */
    void updateArticle(ArticleUpdateDTO dto);

    /**
     * 删除文章
     */
    void deleteArticle(Long id);

    /**
     * 文章详情
     */
    ArticleDetailVO getArticleDetail(Long id);

    /**
     * 分页查询文章列表
     */
    Page<ArticleVO> pageArticles(ArticleQueryDTO dto);

    Page<ArticleVO> pageOwnedArticles(ArticleQueryDTO dto);

    Page<ArticleVO> pageAdminArticles(ArticleQueryDTO dto);

    ArticleDetailVO getOwnedArticleDetail(Long id);

    ArticleDetailVO getAdminArticleDetail(Long id);

    /**
     * 点赞文章
     */
    void likeArticle(Long articleId);

    /**
     * 取消点赞
     */
    void unlikeArticle(Long articleId);

    /**
     * 收藏文章
     */
    void favoriteArticle(Long articleId);

    /**
     * 取消收藏
     */
    void unfavoriteArticle(Long articleId);
}
