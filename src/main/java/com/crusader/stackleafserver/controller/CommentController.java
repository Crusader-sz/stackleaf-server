package com.crusader.stackleafserver.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.model.dto.CommentCreateDTO;
import com.crusader.stackleafserver.model.vo.CommentVO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.CommentService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论控制器
 */
@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;

    @PostMapping
    public Result<Long> create(@Valid @RequestBody CommentCreateDTO dto) {
        Long id = commentService.createComment(dto);
        return Result.success(id);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success();
    }

    @GetMapping("/top")
    public Result<Page<CommentVO>> pageTopComments(
            @RequestParam Long articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CommentVO> page = commentService.pageTopComments(articleId, pageNum, pageSize);
        return Result.success(page);
    }

    @GetMapping("/children/{parentId}")
    public Result<List<CommentVO>> childComments(@PathVariable Long parentId) {
        List<CommentVO> list = commentService.getChildComments(parentId);
        return Result.success(list);
    }
}
