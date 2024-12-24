package com.atcumt.post.service.admin;

import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.vo.DiscussionPostVO;

public interface AdminDiscussionService {
    DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO);

    void deleteDiscussion(Long discussionId);

    void deleteDiscussionComplete(Long discussionId);
}
