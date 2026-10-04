package com.crusader.stackleafserver.service.impl;

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
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.service.support.ArticleAccess;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论业务实现类
 */
@Service
public class CommentServiceImpl extends ServiceImpl<CommentMapper, Comment> implements CommentService {

    @Autowired
    private com.crusader.stackleafserver.service.support.UserAccess userAccess;

    @Autowired
    private ArticleAccess articleAccess;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createComment(CommentCreateDTO dto) {
        Long userId = userAccess.currentUser().getId();

        articleAccess.checkPublished(articleAccess.lock(dto.getArticleId()));
        if (dto.getParentId() != null && dto.getParentId() != 0) {
            Comment parent = baseMapper.selectById(dto.getParentId());
            if (parent == null || !Integer.valueOf(1).equals(parent.getStatus())
                    || !Objects.equals(parent.getArticleId(), dto.getArticleId())) {
                throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.COMMENT_PARENT_INVALID);
            }
            if (dto.getReplyUserId() != null && !Objects.equals(dto.getReplyUserId(), parent.getUserId())) {
                throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.COMMENT_REPLY_INVALID);
            }
            if (userMapper.selectById(parent.getUserId()) == null) {
                throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.TARGET_USER_NOT_FOUND);
            }
            dto.setReplyUserId(parent.getUserId());
        } else if (dto.getReplyUserId() != null) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.COMMENT_REPLY_INVALID);
        }

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
        Long userId = userAccess.currentUser().getId();
        Comment comment = baseMapper.selectById(id);
        if (comment == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.COMMENT_NOT_FOUND);
        }
        articleAccess.lock(comment.getArticleId());
        // 锁获取前评论可能已被另一事务删除，使用当前读重新确认。
        comment = baseMapper.selectOne(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getId, id).last("FOR UPDATE"));
        if (comment == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.COMMENT_NOT_FOUND);
        }
        if (!Objects.equals(comment.getUserId(), userId)) {
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.NO_PERMISSION_DELETE_COMMENT);
        }

        List<Long> allIds = collectAllChildIds(comment.getArticleId(), id);
        allIds.add(id);

        int deleted = baseMapper.delete(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, comment.getArticleId()).in(Comment::getId, allIds));

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, comment.getArticleId())
                .setSql("comment_count = GREATEST(comment_count - " + deleted + ", 0)"));
    }

    @Override
    public Page<CommentVO> pageTopComments(Long articleId, Integer pageNum, Integer pageSize) {
        articleAccess.requirePublished(articleId);
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
                            .eq(Comment::getArticleId, articleId)
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
        Comment parent = baseMapper.selectById(parentId);
        if (parent == null || !Integer.valueOf(1).equals(parent.getStatus())) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.COMMENT_NOT_FOUND);
        }
        articleAccess.requirePublished(parent.getArticleId());
        List<Comment> children = baseMapper.selectList(
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, parent.getArticleId())
                        .eq(Comment::getParentId, parentId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreateTime));
        return children.isEmpty() ? Collections.emptyList() : buildCommentVOList(children);
    }

    /**
     * 递归收集所有子孙评论ID
     */
    private List<Long> collectAllChildIds(Long articleId, Long parentId) {
        List<Long> ids = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(parentId);
        List<Long> level = List.of(parentId);
        while (!level.isEmpty()) {
            List<Comment> children = baseMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .eq(Comment::getArticleId, articleId).in(Comment::getParentId, level)
                    .select(Comment::getId).last("FOR UPDATE"));
            List<Long> next = new ArrayList<>();
            for (Comment child : children) {
                if (visited.add(child.getId())) {
                    ids.add(child.getId());
                    next.add(child.getId());
                }
            }
            level = next;
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
