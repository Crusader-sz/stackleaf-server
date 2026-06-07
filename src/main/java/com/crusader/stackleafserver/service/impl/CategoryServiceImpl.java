package com.crusader.stackleafserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.mapper.CategoryMapper;
import com.crusader.stackleafserver.model.dto.CategoryDTO;
import com.crusader.stackleafserver.model.entity.Category;
import com.crusader.stackleafserver.model.vo.CategoryVO;
import com.crusader.stackleafserver.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类业务实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public void createCategory(CategoryDTO dto) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getName, dto.getName()));
        if (count > 0) {
            throw new RuntimeException("分类名称已存在");
        }

        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        baseMapper.insert(category);
    }

    @Override
    public void updateCategory(CategoryDTO dto) {
        Category category = baseMapper.selectById(dto.getId());
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        if (dto.getName() != null) { category.setName(dto.getName()); }
        if (dto.getDescription() != null) { category.setDescription(dto.getDescription()); }
        if (dto.getSort() != null) { category.setSort(dto.getSort()); }
        baseMapper.updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        baseMapper.deleteById(id);
    }

    @Override
    public List<CategoryVO> listAll() {
        List<Category> list = baseMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSort));
        return list.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
