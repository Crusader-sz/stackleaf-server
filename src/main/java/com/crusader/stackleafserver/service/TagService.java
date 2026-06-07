package com.crusader.stackleafserver.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.crusader.stackleafserver.model.dto.TagDTO;
import com.crusader.stackleafserver.model.entity.Tag;
import com.crusader.stackleafserver.model.vo.TagVO;

import java.util.List;

/**
 * 标签业务接口
 */
public interface TagService extends IService<Tag> {

    /**
     * 新增标签
     */
    void createTag(TagDTO dto);

    /**
     * 更新标签
     */
    void updateTag(TagDTO dto);

    /**
     * 删除标签
     */
    void deleteTag(Long id);

    /**
     * 查询所有标签
     */
    List<TagVO> listAll();
}
