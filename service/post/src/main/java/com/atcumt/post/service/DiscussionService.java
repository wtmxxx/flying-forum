package com.atcumt.post.service;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.post.dto.DiscussionDTO;
import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.vo.DiscussionPostVO;
import com.atcumt.model.post.vo.DiscussionVO;

public interface DiscussionService {
    DiscussionPostVO postDiscussion(DiscussionDTO discussionDTO) throws Exception;

    DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO) throws AuthorizationException;

    DiscussionPostVO saveDiscussionAsDraft(DiscussionDTO discussionDTO);

    void deleteDiscussion(Long discussionId) throws AuthorizationException;

    void privateDiscussion(Long discussionId) throws AuthorizationException;

    DiscussionVO getDiscussion(Long discussionId);

    void pinDiscussion(Long discussionId);

    void unpinDiscussion(Long discussionId);
}
