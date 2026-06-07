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

    @GetMapping("/me")
    public Result<UserVO> currentUser() {
        UserVO vo = userService.getCurrentUser();
        return Result.success(vo);
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UserUpdateDTO dto) {
        userService.updateProfile(dto);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO vo = userService.getUserById(id);
        return Result.success(vo);
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

    @GetMapping("/following/{userId}")
    public Result<List<UserVO>> followingList(@PathVariable Long userId) {
        List<UserVO> list = articleActionService.getFollowingList(userId);
        return Result.success(list);
    }

    @GetMapping("/followers/{userId}")
    public Result<List<UserVO>> followerList(@PathVariable Long userId) {
        List<UserVO> list = articleActionService.getFollowerList(userId);
        return Result.success(list);
    }

    @GetMapping("/likes")
    public Result<List<ArticleVO>> likedArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<ArticleVO> list = articleActionService.getLikedArticles(userId, pageNum, pageSize).getRecords();
        return Result.success(list);
    }

    @GetMapping("/favorites")
    public Result<List<ArticleVO>> favoritedArticles(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        List<ArticleVO> list = articleActionService.getFavoritedArticles(userId, pageNum, pageSize).getRecords();
        return Result.success(list);
    }
}
