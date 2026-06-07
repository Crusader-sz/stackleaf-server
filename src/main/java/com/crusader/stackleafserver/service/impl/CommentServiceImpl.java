package com.crusader.stackleafserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.mapper.ArticleMapper;
import com.crusader.stackleafserver.mapper.CommentMapper;
import com.crusader.stackleafserver.mapper.UserMapper;
import com.crusader.stackleafserver.model.dto.CommentCreateDTO;
import com.crusader.stackleafserver.model.entity.Article;
import com.crusader.stackleafserver.model.entity.Comment;
import com.crusader.stackleafserver.model.entity.User;
import com.crusader.stackleafserver.model.vo.CommentVO;
import com.crusader.stackleafserver.model.vo.UserVO;
import com.crusader.stackleafserver.service.CommentService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论业务实现类
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();

        Comment comment = new Comment();
        BeanUtils.copyProperties(dto, comment);
        comment.setUserId(userId);
        if (comment.getParentId() == null || comment.getParentId() == 0) {
            comment.setParentId(0L);
            comment.setReplyUserId(null);
        }
        comment.setStatus(1);
        baseMapper.insert(comment);

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, dto.getArticleId())
                .setSql("comment_count = comment_count + 1"));

        return comment.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long id) {
        Long userId = StpUtil.getLoginIdAsLong();
        Comment comment = baseMapper.selectById(id);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new RuntimeException("只能删除自己的评论");
        }

        List<Long> allIds = collectAllChildIds(id);
        allIds.add(id);

        baseMapper.delete(new LambdaQueryWrapper<Comment>().in(Comment::getId, allIds));

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, comment.getArticleId())
                .setSql("comment_count = comment_count - " + allIds.size()));
    }

    @Override
    public Page<CommentVO> pageTopComments(Long articleId, Integer pageNum, Integer pageSize) {
        Page<Comment> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .eq(Comment::getParentId, 0L)
                .eq(Comment::getStatus, 1)
                .orderByDesc(Comment::getCreateTime);
        Page<Comment> result = baseMapper.selectPage(page, wrapper);

        if (result.getRecords().isEmpty()) {
            return new Page<>(pageNum, pageSize, 0);
        }

        List<CommentVO> voList = buildCommentVOList(result.getRecords());
        for (CommentVO topVo : voList) {
            List<Comment> children = baseMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getParentId, topVo.getId())
                            .eq(Comment::getStatus, 1)
                            .orderByAsc(Comment::getCreateTime));
            topVo.setChildren(children.isEmpty() ? Collections.emptyList() : buildCommentVOList(children));
        }

        Page<CommentVO> voPage = new Page<>(pageNum, pageSize, result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public List<CommentVO> getChildComments(Long parentId) {
        List<Comment> children = baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, parentId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreateTime));
        return children.isEmpty() ? Collections.emptyList() : buildCommentVOList(children);
    }

    /**
     * 递归收集所有子孙评论ID
     */
    private List<Long> collectAllChildIds(Long parentId) {
        List<Long> ids = new ArrayList<>();
        List<Comment> children = baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getParentId, parentId)
                        .select(Comment::getId));
        for (Comment child : children) {
            ids.add(child.getId());
            ids.addAll(collectAllChildIds(child.getId()));
        }
        return ids;
    }

    /**
     * 评论实体列表转 VO 列表，填充 user / replyUser
     */
    private List<CommentVO> buildCommentVOList(List<Comment> comments) {
        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        comments.stream().map(Comment::getReplyUserId).filter(Objects::nonNull).forEach(userIds::add);

        Map<Long, UserVO> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, this::toUserVO));

        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(c, vo);
            vo.setUser(userMap.get(c.getUserId()));
            if (c.getReplyUserId() != null) {
                vo.setReplyUser(userMap.get(c.getReplyUserId()));
            }
            return vo;
        }).collect(Collectors.toList());
    }

    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
