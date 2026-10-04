package com.crusader.stackleafserver.service.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.ArticleMapper;
import com.crusader.stackleafserver.model.entity.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ArticleAccess {
    @Autowired
    private ArticleMapper articleMapper;

    public Article requirePublished(Long id) {
        Article article = articleMapper.selectById(id);
        checkPublished(article);
        return article;
    }

    // 所有文章/评论写操作在事务中先锁文章行，统一锁顺序，避免删除和新增交错。
    public Article lock(Long id) {
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getId, id).last("FOR UPDATE"));
        if (article == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.ARTICLE_NOT_FOUND);
        }
        return article;
    }

    public void checkPublished(Article article) {
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.ARTICLE_NOT_FOUND);
        }
    }
}
