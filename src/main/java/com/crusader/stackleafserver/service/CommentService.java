package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.crusader.stackleafserver.model.dto.CommentCreateDTO;
import com.crusader.stackleafserver.model.entity.Comment;
import com.crusader.stackleafserver.model.vo.CommentVO;

import java.util.List;

/**
 * 评论业务接口
 */
public interface CommentService extends IService<Comment> {

    /**
     * 发表评论
     */
    Long createComment(CommentCreateDTO dto);

    /**
     * 删除评论
     */
    void deleteComment(Long id);

    /**
     * 分页查询顶级评论（含子评论）
     */
    Page<CommentVO> pageTopComments(Long articleId, Integer pageNum, Integer pageSize);

    /**
     * 查询某条评论的子评论列表（楼中楼）
     */
    List<CommentVO> getChildComments(Long parentId);
}
