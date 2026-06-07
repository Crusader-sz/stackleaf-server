package com.crusader.stackleafserver.controller;

import com.crusader.stackleafserver.model.dto.CategoryDTO;
import com.crusader.stackleafserver.model.vo.CategoryVO;
import com.crusader.stackleafserver.result.Result;
import com.crusader.stackleafserver.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public Result<Void> create(@Valid @RequestBody CategoryDTO dto) {
        categoryService.createCategory(dto);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@Valid @RequestBody CategoryDTO dto) {
        categoryService.updateCategory(dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<CategoryVO>> list() {
        List<CategoryVO> list = categoryService.listAll();
        return Result.success(list);
    }
}
