package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crusader.stackleafserver.model.dto.UserLoginDTO;
import com.crusader.stackleafserver.model.dto.UserRegisterDTO;
import com.crusader.stackleafserver.model.dto.UserUpdateDTO;
import com.crusader.stackleafserver.model.dto.PasswordResetDTO;
import com.crusader.stackleafserver.model.entity.User;
import com.crusader.stackleafserver.model.vo.UserVO;

/**
 * 用户业务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(UserRegisterDTO dto);

    /**
     * 用户登录，返回 Token
     */
    String login(UserLoginDTO dto);

    /**
     * 退出登录
     */
    void logout();

    /**
     * 发送邮箱验证码
     */
    void sendVerificationCode(String email);

    /**
     * 重置密码
     */
    void resetPassword(PasswordResetDTO dto);

    /**
     * 获取当前登录用户信息
     */
    UserVO getCurrentUser();

    /**
     * 更新用户信息
     */
    void updateProfile(UserUpdateDTO dto);

    /**
     * 关注用户
     */
    void follow(Long followUserId);

    /**
     * 取消关注
     */
    void unfollow(Long followUserId);

    /**
     * 获取用户信息（公开）
     */
    UserVO getUserById(Long userId);
}
