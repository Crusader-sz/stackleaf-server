package com.crusader.stackleafserver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.crusader.stackleafserver.mapper.TagMapper;
import com.crusader.stackleafserver.model.dto.TagDTO;
import com.crusader.stackleafserver.model.entity.Tag;
import com.crusader.stackleafserver.model.vo.TagVO;
import com.crusader.stackleafserver.service.TagService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 标签业务实现类
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    @Override
    public void createTag(TagDTO dto) {
        Long count = baseMapper.selectCount(
                new LambdaQueryWrapper<Tag>().eq(Tag::getName, dto.getName()));
        if (count > 0) {
            throw new RuntimeException("标签名称已存在");
        }

        Tag tag = new Tag();
        BeanUtils.copyProperties(dto, tag);
        baseMapper.insert(tag);
    }

    @Override
    public void updateTag(TagDTO dto) {
        Tag tag = baseMapper.selectById(dto.getId());
        if (tag == null) {
            throw new RuntimeException("标签不存在");
        }
        if (dto.getName() != null) { tag.setName(dto.getName()); }
        baseMapper.updateById(tag);
    }

    @Override
    public void deleteTag(Long id) {
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
