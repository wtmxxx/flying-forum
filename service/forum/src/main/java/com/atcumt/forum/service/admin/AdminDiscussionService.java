package com.atcumt.forum.service.admin;

import com.atcumt.model.forum.dto.DiscussionUpdateDTO;
import com.atcumt.model.forum.vo.DiscussionPostVO;

public interface AdminDiscussionService {
    DiscussionPostVO updateDiscussion(DiscussionUpdateDTO discussionUpdateDTO);

    void deleteDiscussion(Long discussionId);

    void deleteDiscussionComplete(Long discussionId);
}
