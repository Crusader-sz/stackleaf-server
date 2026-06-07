package com.crusader.stackleafserver.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
    private UserMapper userMapper;

    @Autowired
    private UserFollowMapper userFollowMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String VERIFICATION_CODE_KEY_PREFIX = "verification:email:";
    private static final long VERIFICATION_CODE_EXPIRE_MINUTES = 5;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserRegisterDTO dto) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            Long emailCount = userMapper.selectCount(
                    new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
            if (emailCount > 0) {
                throw new RuntimeException("邮箱已被注册");
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setEmail(dto.getEmail());
        user.setRole(0);
        user.setStatus(1);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        userMapper.insert(user);
    }

    @Override
    public String login(UserLoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public void sendVerificationCode(String email) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        String redisKey = VERIFICATION_CODE_KEY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(redisKey, code, VERIFICATION_CODE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        // TODO: 调用邮件服务发送验证码
        log.info("验证码已生成: email={}, code={}", email, code);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetDTO dto) {
        String redisKey = VERIFICATION_CODE_KEY_PREFIX + dto.getEmail();
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null || !cachedCode.equals(dto.getVerificationCode())) {
            throw new RuntimeException("验证码错误或已过期");
        }

        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (user == null) {
            throw new RuntimeException("该邮箱未注册");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userMapper.updateById(user);
        stringRedisTemplate.delete(redisKey);
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UserUpdateDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }
        if (dto.getGithubUrl() != null) {
            user.setGithubUrl(dto.getGithubUrl());
        }
        if (dto.getWebsiteUrl() != null) {
            user.setWebsiteUrl(dto.getWebsiteUrl());
        }
        userMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (currentUserId.equals(followUserId)) {
            throw new RuntimeException("不能关注自己");
        }

        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, currentUserId)
                        .eq(UserFollow::getFollowUserId, followUserId));
        if (count > 0) {
            throw new RuntimeException("已关注该用户");
        }

        UserFollow userFollow = new UserFollow();
        userFollow.setUserId(currentUserId);
        userFollow.setFollowUserId(followUserId);
        userFollowMapper.insert(userFollow);

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .setSql("following_count = following_count + 1"));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, followUserId)
                .setSql("follower_count = follower_count + 1"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followUserId) {
        Long currentUserId = StpUtil.getLoginIdAsLong();

        Long count = userFollowMapper.selectCount(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, currentUserId)
                        .eq(UserFollow::getFollowUserId, followUserId));
        if (count == 0) {
            throw new RuntimeException("未关注该用户");
        }

        userFollowMapper.delete(
                new LambdaQueryWrapper<UserFollow>()
                        .eq(UserFollow::getUserId, currentUserId)
                        .eq(UserFollow::getFollowUserId, followUserId));

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, currentUserId)
                .setSql("following_count = following_count - 1"));
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, followUserId)
                .setSql("follower_count = follower_count - 1"));
    }

    @Override
    public UserVO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return convertToVO(user);
    }

    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
