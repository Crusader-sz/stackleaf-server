package com.crusader.stackleafserver.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.*;
import com.crusader.stackleafserver.model.dto.*;
import com.crusader.stackleafserver.model.entity.*;
import com.crusader.stackleafserver.model.vo.AdminUserVO;
import com.crusader.stackleafserver.service.support.UserAccess;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
public class AdminUserService {
    @Autowired private UserMapper users;
    @Autowired private UserAccess access;
    @Autowired private ArticleMapper articles;
    @Autowired private CommentMapper comments;
    @Autowired private ArticleLikeMapper likes;
    @Autowired private ArticleFavoriteMapper favorites;
    @Autowired private UserFollowMapper follows;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public Page<AdminUserVO> page(AdminUserQueryDTO dto) {
        access.requireAdmin();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            String keyword = dto.getKeyword().trim();
            query.and(q -> q.like(User::getUsername, keyword).or().like(User::getNickname, keyword));
        }
        query.orderByDesc(User::getId);
        Page<User> source = users.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), query);
        Page<AdminUserVO> result = new Page<>(source.getCurrent(), source.getSize(), source.getTotal());
        result.setRecords(source.getRecords().stream().map(this::toVO).toList());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminUserCreateDTO dto) {
        access.requireAdmin();
        checkEmail(dto.getEmail(), null);
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setEmail(normalizeEmail(dto.getEmail()));
        user.setPassword(encoder.encode(dto.getPassword()));
        user.setFollowerCount(0); user.setFollowingCount(0);
        try { users.insert(user); }
        catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.USERNAME_EXISTS);
        }
        return user.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, AdminUserUpdateDTO dto) {
        access.requireAdmin();
        User user = lockedUser(id);
        if (Objects.equals(id, StpUtil.getLoginIdAsLong()) && (dto.getRole() != 1 || dto.getStatus() != 1)) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.CANNOT_DISABLE_SELF);
        }
        boolean demoted = Integer.valueOf(1).equals(user.getRole()) && dto.getRole() == 0;
        checkEmail(dto.getEmail(), id);
        user.setNickname(dto.getNickname());
        user.setEmail(normalizeEmail(dto.getEmail()));
        user.setRole(dto.getRole()); user.setStatus(dto.getStatus());
        if (dto.getPassword() != null) user.setPassword(encoder.encode(dto.getPassword()));
        LambdaUpdateWrapper<User> update = new LambdaUpdateWrapper<User>().eq(User::getId, id)
                .set(User::getNickname, user.getNickname()).set(User::getEmail, user.getEmail())
                .set(User::getRole, user.getRole()).set(User::getStatus, user.getStatus())
                .set(User::getUpdateTime, java.time.LocalDateTime.now());
        if (dto.getPassword() != null) update.set(User::getPassword, user.getPassword());
        users.update(null, update);
        if (dto.getPassword() != null || dto.getStatus() == 0 || demoted) {
            StpUtil.kickout(id);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        access.requireAdmin();
        lockedUser(id);
        if (Objects.equals(id, StpUtil.getLoginIdAsLong())) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.CANNOT_DELETE_SELF);
        }
        if (articles.selectCount(new LambdaQueryWrapper<Article>().eq(Article::getAuthorId, id)) > 0
                || comments.selectCount(new LambdaQueryWrapper<Comment>().eq(Comment::getUserId, id).or().eq(Comment::getReplyUserId, id)) > 0
                || likes.selectCount(new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getUserId, id)) > 0
                || favorites.selectCount(new LambdaQueryWrapper<ArticleFavorite>().eq(ArticleFavorite::getUserId, id)) > 0
                || follows.selectCount(new LambdaQueryWrapper<UserFollow>().eq(UserFollow::getUserId, id).or().eq(UserFollow::getFollowUserId, id)) > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.USER_HAS_CONTENT);
        }
        users.deleteById(id);
        StpUtil.kickout(id);
    }

    private User lockedUser(Long id) {
        User user = users.selectOne(new LambdaQueryWrapper<User>().eq(User::getId, id).last("FOR UPDATE"));
        if (user == null) throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.USER_NOT_FOUND);
        return user;
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim();
    }

    private void checkEmail(String email, Long id) {
        String value = normalizeEmail(email);
        if (value != null && users.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, value)
                .ne(id != null, User::getId, id)) > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.EMAIL_EXISTS);
        }
    }

    private AdminUserVO toVO(User user) {
        AdminUserVO vo = new AdminUserVO(); BeanUtils.copyProperties(user, vo); return vo;
    }
}
