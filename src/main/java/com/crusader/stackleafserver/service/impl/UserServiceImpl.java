package com.crusader.stackleafserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.constant.VerificationConstant;
import com.crusader.stackleafserver.enumeration.UserRole;
import com.crusader.stackleafserver.enumeration.UserStatus;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.UserFollowMapper;
import com.crusader.stackleafserver.mapper.UserMapper;
import com.crusader.stackleafserver.model.dto.PasswordResetDTO;
import com.crusader.stackleafserver.model.dto.UserLoginDTO;
import com.crusader.stackleafserver.model.dto.UserRegisterDTO;
import com.crusader.stackleafserver.model.dto.UserUpdateDTO;
import com.crusader.stackleafserver.model.entity.User;
import com.crusader.stackleafserver.model.entity.UserFollow;
import com.crusader.stackleafserver.model.vo.UserVO;
import com.crusader.stackleafserver.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 用户业务实现类
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private UserFollowMapper userFollowMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.USERNAME_EXISTS);
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            Long emailCount = baseMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (emailCount > 0) {
                throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.EMAIL_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole(UserRole.NORMAL.getCode());
        user.setStatus(UserStatus.NORMAL.getCode());
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        baseMapper.insert(user);

        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public String login(UserLoginDTO dto) {
        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            log.warn("登录失败, 用户不存在: username={}", dto.getUsername());
            throw new BusinessException(ResultCodeConstant.UNAUTHORIZED, MessageConstant.USERNAME_OR_PASSWORD_ERROR);
        }
        if (user.getStatus() != null && user.getStatus() == UserStatus.DISABLED.getCode()) {
            log.warn("登录失败, 账号已禁用: userId={}", user.getId());
            throw new BusinessException(ResultCodeConstant.FORBIDDEN, MessageConstant.ACCOUNT_DISABLED);
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败, 密码错误: userId={}", user.getId());
            throw new BusinessException(ResultCodeConstant.UNAUTHORIZED, MessageConstant.USERNAME_OR_PASSWORD_ERROR);
        }

        StpUtil.login(user.getId());
        log.info("用户登录成功: userId={}, username={}", user.getId(), user.getUsername());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        Long userId = StpUtil.getLoginIdAsLong();
        StpUtil.logout();
        log.info("用户退出登录: userId={}", userId);
    }

    @Override
    public void sendVerificationCode(String email) {
        String limitKey = VerificationConstant.LIMIT_KEY_PREFIX + email;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(limitKey))) {
            throw new BusinessException(ResultCodeConstant.TOO_MANY_REQUESTS, MessageConstant.VERIFICATION_CODE_RATE_LIMIT);
        }

        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        String redisKey = VerificationConstant.CODE_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(redisKey, code, VerificationConstant.CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        stringRedisTemplate.opsForValue().set(limitKey, "1", VerificationConstant.LIMIT_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // TODO: 调用邮件服务发送验证码
        log.info("验证码已生成: email={}, code={}", email, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetDTO dto) {
        String redisKey = VerificationConstant.CODE_KEY_PREFIX + dto.getEmail();
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(dto.getVerificationCode())) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.VERIFICATION_CODE_EXPIRED);
        }

        User user = baseMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.EMAIL_NOT_REGISTERED);
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        baseMapper.updateById(user);
        stringRedisTemplate.delete(redisKey);

        StpUtil.kickout(user.getId());
        log.info("密码重置成功, 已踢出所有会话: userId={}", user.getId());
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.USER_NOT_FOUND);
        }
        if (dto.getNickname() != null) { user.setNickname(dto.getNickname()); }
        if (dto.getAvatar() != null) { user.setAvatar(dto.getAvatar()); }
        if (dto.getBio() != null) { user.setBio(dto.getBio()); }
        if (dto.getGithubUrl() != null) { user.setGithubUrl(dto.getGithubUrl()); }
        if (dto.getWebsiteUrl() != null) { user.setWebsiteUrl(dto.getWebsiteUrl()); }
        baseMapper.updateById(user);

        log.info("用户信息更新成功: userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(followUserId)) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.CANNOT_FOLLOW_SELF);
        }

        User targetUser = baseMapper.selectById(followUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.TARGET_USER_NOT_FOUND);
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setUserId(currentUserId);
        userFollow.setFollowUserId(followUserId);
        try {
            userFollowMapper.insert(userFollow);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.ALREADY_FOLLOWED);
        }

        baseMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .setSql("following_count = following_count + 1"));
        baseMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, followUserId)
                .setSql("follower_count = follower_count + 1"));

        log.info("关注成功: userId={}, followUserId={}", currentUserId, followUserId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        User targetUser = baseMapper.selectById(followUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.TARGET_USER_NOT_FOUND);
        }

        int deleted = userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, currentUserId)
                        .eq(UserFollow::getFollowUserId, followUserId));
        if (deleted == 0) {
            throw new BusinessException(ResultCodeConstant.BAD_REQUEST, MessageConstant.NOT_FOLLOWED);
        }

        baseMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .setSql("following_count = following_count - 1"));
        baseMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, followUserId)
                .setSql("follower_count = follower_count - 1"));

        log.info("取消关注成功: userId={}, followUserId={}", currentUserId, followUserId);
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = baseMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.USER_NOT_FOUND);
        }
        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
