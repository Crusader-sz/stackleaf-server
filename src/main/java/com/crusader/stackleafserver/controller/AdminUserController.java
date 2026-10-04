package com.crusader.stackleafserver.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.crusader.stackleafserver.model.dto.*;
import com.crusader.stackleafserver.model.vo.*;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.AdminUserService;
import com.crusader.stackleafserver.service.UserService;
import com.crusader.stackleafserver.service.support.UserAccess;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminUserController {
    @Autowired private AdminUserService users;
    @Autowired private UserAccess access;
    @Autowired private UserService userService;

    public record Session(UserVO user, long timeoutSeconds) {}

    @GetMapping("/session")
    public Result<Session> session() {
        access.requireAdmin();
        return Result.success(new Session(userService.getCurrentUser(), StpUtil.getTokenTimeout()));
    }
    @GetMapping("/users")
    public Result<Page<AdminUserVO>> page(@Valid AdminUserQueryDTO dto) {
        return Result.success(users.page(dto));
    }
    @PostMapping("/users")
    public Result<Long> create(@Valid @RequestBody AdminUserCreateDTO dto) {
        return Result.success(users.create(dto));
    }
    @PutMapping("/users/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody AdminUserUpdateDTO dto) {
        users.update(id, dto); return Result.success();
    }
    @DeleteMapping("/users/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        users.delete(id); return Result.success();
    }
}
