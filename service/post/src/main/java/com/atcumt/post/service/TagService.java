package com.atcumt.post.service;

import com.atcumt.model.post.dto.NewTagDTO;
import com.atcumt.model.post.vo.TagListVO;
import com.atcumt.model.post.vo.TagVO;

public interface TagService {
    TagListVO newTag(NewTagDTO newTagDTO);

    TagVO getTag(Long tagId);
}
