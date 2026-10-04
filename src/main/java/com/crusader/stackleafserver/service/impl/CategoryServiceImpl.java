package com.crusader.stackleafserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.ArticleMapper;
import com.crusader.stackleafserver.mapper.CategoryMapper;
import com.crusader.stackleafserver.model.dto.CategoryDTO;
import com.crusader.stackleafserver.model.entity.Article;
import com.crusader.stackleafserver.model.entity.Category;
import com.crusader.stackleafserver.model.vo.CategoryVO;
import com.crusader.stackleafserver.service.CategoryService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import com.crusader.stackleafserver.service.support.UserAccess;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类业务实现类
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private UserAccess userAccess;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createCategory(CategoryDTO dto) {
        userAccess.requireAdmin();
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getName, dto.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.CATEGORY_NAME_EXISTS);
        }

        Category category = new Category();
        BeanUtils.copyProperties(dto, category);
        try { baseMapper.insert(category); }
        catch (DuplicateKeyException e) { throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.CATEGORY_NAME_EXISTS); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCategory(CategoryDTO dto) {
        userAccess.requireAdmin();
        Category category = baseMapper.selectOne(new LambdaQueryWrapper<Category>()
                .eq(Category::getId, dto.getId()).last("FOR UPDATE"));
        if (category == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.CATEGORY_NOT_FOUND);
        }
        if (dto.getName() != null) { category.setName(dto.getName()); }
        if (dto.getDescription() != null) { category.setDescription(dto.getDescription()); }
        if (dto.getSort() != null) { category.setSort(dto.getSort()); }
        try { baseMapper.updateById(category); }
        catch (DuplicateKeyException e) { throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.CATEGORY_NAME_EXISTS); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        userAccess.requireAdmin();
        if (baseMapper.selectOne(new LambdaQueryWrapper<Category>().eq(Category::getId, id).last("FOR UPDATE")) == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.CATEGORY_NOT_FOUND);
        }
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>().eq(Article::getCategoryId, id));
        if (articleCount > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.CATEGORY_HAS_ARTICLES);
        }
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
