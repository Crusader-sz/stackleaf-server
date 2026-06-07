package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crusader.stackleafserver.model.dto.CategoryDTO;
import com.crusader.stackleafserver.model.entity.Category;
import com.crusader.stackleafserver.model.vo.CategoryVO;

import java.util.List;

/**
 * 分类业务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 新增分类
     */
    void createCategory(CategoryDTO dto);

    /**
     * 更新分类
     */
    void updateCategory(CategoryDTO dto);

    /**
     * 删除分类
     */
    void deleteCategory(Long id);

    /**
     * 查询所有分类
     */
    List<CategoryVO> listAll();
}
