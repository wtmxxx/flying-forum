package com.atcumt.forum.service;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.forum.dto.DiscussionDTO;
import com.atcumt.model.forum.dto.DiscussionUpdateDTO;
import com.atcumt.model.forum.vo.DiscussionPostVO;

public interface DiscussionService {
    DiscussionPostVO postDiscussion(DiscussionDTO discussionDTO) throws Exception;

    DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) throws AuthorizationException;

    DiscussionPostVO saveDiscussionAsDraft(DiscussionDTO discussionDTO);

    void deleteDiscussion(Long discussionId) throws AuthorizationException;

    void privateDiscussion(Long discussionId) throws AuthorizationException;
}
