package com.crusader.stackleafserver.controller;

import com.crusader.stackleafserver.model.dto.UserUpdateDTO;
import com.crusader.stackleafserver.model.vo.ArticleVO;
import com.crusader.stackleafserver.model.vo.UserVO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.ArticleActionService;
import com.crusader.stackleafserver.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器（个人信息、关注、点赞/收藏列表）
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private ArticleActionService articleActionService;

    // ========== 需要登录 ==========

    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @PostMapping("/follow/{id}")
    public Result<Void> follow(@PathVariable Long id) {
        userService.follow(id);
        return Result.success();
    }

    @DeleteMapping("/follow/{id}")
    public Result<Void> unfollow(@PathVariable Long id) {
        userService.unfollow(id);
        return Result.success();
    }

    @GetMapping("/likes")
    public Result<List<ArticleVO>> likedArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(articleActionService.getLikedArticles(userId, pageNum, pageSize).getRecords());
    }

    @GetMapping("/favorites")
    public Result<List<ArticleVO>> favoritedArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(articleActionService.getFavoritedArticles(userId, pageNum, pageSize).getRecords());
    }

    // ========== 公开接口 ==========

    /** 查看用户主页（公开） */
    @GetMapping("/profile/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        return Result.success(userService.getUserById(id));
    }

    /** 关注列表（公开） */
    @GetMapping("/following/{userId}")
    public Result<List<UserVO>> followingList(@PathVariable Long userId) {
        return Result.success(articleActionService.getFollowingList(userId));
    }

    /** 粉丝列表（公开） */
    @GetMapping("/followers/{userId}")
    public Result<List<UserVO>> followerList(@PathVariable Long userId) {
        return Result.success(articleActionService.getFollowerList(userId));
    }
}
