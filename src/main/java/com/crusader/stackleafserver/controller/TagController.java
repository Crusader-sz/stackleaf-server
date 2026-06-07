package com.crusader.stackleafserver.controller;

import com.crusader.stackleafserver.model.dto.TagDTO;
import com.crusader.stackleafserver.model.vo.TagVO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.TagService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 标签控制器
 */
@RestController
@RequestMapping("/tag")
public class TagController {

    @Autowired
    private TagService tagService;

    @PostMapping
    public Result<Void> create(@Valid @RequestBody TagDTO dto) {
        tagService.createTag(dto);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody TagDTO dto) {
        tagService.updateTag(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagService.deleteTag(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<TagVO>> list() {
        List<TagVO> list = tagService.listAll();
        return Result.success(list);
    }
}
