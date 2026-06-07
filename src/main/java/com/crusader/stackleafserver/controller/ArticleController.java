package com.crusader.stackleafserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.model.dto.ArticleCreateDTO;
import com.crusader.stackleafserver.model.dto.ArticleQueryDTO;
import com.crusader.stackleafserver.model.dto.ArticleUpdateDTO;
import com.crusader.stackleafserver.model.vo.ArticleDetailVO;
import com.crusader.stackleafserver.model.vo.ArticleVO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.ArticleService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文章控制器
 */
@RestController
@RequestMapping("/article")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody ArticleCreateDTO dto) {
        Long id = articleService.createArticle(dto);
        return Result.success(id);
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody ArticleUpdateDTO dto) {
        articleService.updateArticle(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        ArticleDetailVO vo = articleService.getArticleDetail(id);
        return Result.success(vo);
    }

    @GetMapping("/page")
    public Result<Page<ArticleVO>> page(ArticleQueryDTO dto) {
        Page<ArticleVO> page = articleService.pageArticles(dto);
        return Result.success(page);
    }

    @PostMapping("/like/{id}")
    public Result<Void> like(@PathVariable Long id) {
        articleService.likeArticle(id);
        return Result.success();
    }

    @DeleteMapping("/like/{id}")
    public Result<Void> unlike(@PathVariable Long id) {
        articleService.unlikeArticle(id);
        return Result.success();
    }

    @PostMapping("/favorite/{id}")
    public Result<Void> favorite(@PathVariable Long id) {
        articleService.favoriteArticle(id);
        return Result.success();
    }

    @DeleteMapping("/favorite/{id}")
    public Result<Void> unfavorite(@PathVariable Long id) {
        articleService.unfavoriteArticle(id);
        return Result.success();
    }
}
