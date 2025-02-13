package com.atcumt.post.service;

import com.atcumt.model.post.dto.NewTagDTO;
import com.atcumt.model.post.vo.TagListVO;
import com.atcumt.model.post.vo.TagSimpleVO;
import com.atcumt.model.post.vo.TagVO;

import java.util.List;

public interface TagService {
    TagListVO newTag(NewTagDTO newTagDTO);

    TagVO getTag(Long tagId);

    List<TagSimpleVO> getSimpleTags(List<Long> tagIds);
}
