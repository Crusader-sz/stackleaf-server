package com.crusader.stackleafserver.controller;

import com.crusader.stackleafserver.model.dto.PasswordResetDTO;
import com.crusader.stackleafserver.model.dto.UserLoginDTO;
import com.crusader.stackleafserver.model.dto.UserRegisterDTO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（登录、注册、验证码、密码重置）
 */
@RestController
@RequestMapping("/user")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    @PostMapping("/login")
    public Result<String> login(@Valid @RequestBody UserLoginDTO dto) {
        String token = userService.login(dto);
        return Result.success(token);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success();
    }

    @PostMapping("/sendVerificationCode")
    public Result<Void> sendVerificationCode(@RequestParam String email) {
        userService.sendVerificationCode(email);
        return Result.success();
    }

    @PostMapping("/resetUserPassword")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetDTO dto) {
        userService.resetPassword(dto);
        return Result.success();
    }
}
