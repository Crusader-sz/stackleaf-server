package com.crusader.stackleafserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.constant.MessageConstant;
import com.crusader.stackleafserver.constant.ResultCodeConstant;
import com.crusader.stackleafserver.exception.BusinessException;
import com.crusader.stackleafserver.mapper.ArticleTagMapper;
import com.crusader.stackleafserver.mapper.TagMapper;
import com.crusader.stackleafserver.model.dto.TagDTO;
import com.crusader.stackleafserver.model.entity.ArticleTag;
import com.crusader.stackleafserver.model.entity.Tag;
import com.crusader.stackleafserver.model.vo.TagVO;
import com.crusader.stackleafserver.service.TagService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DuplicateKeyException;
import com.crusader.stackleafserver.service.support.UserAccess;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签业务实现类
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Autowired
    private UserAccess userAccess;

    @Autowired
    private ArticleTagMapper articleTagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createTag(TagDTO dto) {
        userAccess.requireAdmin();
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getName, dto.getName()));
        if (count > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.TAG_NAME_EXISTS);
        }

        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        try { baseMapper.insert(tag); }
        catch (DuplicateKeyException e) { throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.TAG_NAME_EXISTS); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTag(TagDTO dto) {
        userAccess.requireAdmin();
        Tag tag = baseMapper.selectOne(new LambdaQueryWrapper<Tag>()
                .eq(Tag::getId, dto.getId()).last("FOR UPDATE"));
        if (tag == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.TAG_NOT_FOUND);
        }
        if (dto.getName() != null) { tag.setName(dto.getName()); }
        try { baseMapper.updateById(tag); }
        catch (DuplicateKeyException e) { throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.TAG_NAME_EXISTS); }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Long id) {
        userAccess.requireAdmin();
        if (baseMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getId, id).last("FOR UPDATE")) == null) {
            throw new BusinessException(ResultCodeConstant.NOT_FOUND, MessageConstant.TAG_NOT_FOUND);
        }
        Long refCount = articleTagMapper.selectCount(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, id));
        if (refCount > 0) {
            throw new BusinessException(ResultCodeConstant.CONFLICT, MessageConstant.TAG_HAS_ARTICLES);
        }
        baseMapper.deleteById(id);
    }

    @Override
    public List<TagVO> listAll() {
        List<Tag> list = baseMapper.selectList(null);
        return list.stream().map(t -> {
            TagVO vo = new TagVO();
            BeanUtils.copyProperties(t, vo);
            return vo;
        }).collect(Collectors.toList());
    }
}
