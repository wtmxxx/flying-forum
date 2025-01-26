package com.atcumt.post.service;

import com.atcumt.model.post.dto.PostFeedDTO;
import com.atcumt.model.post.vo.PostFeedListVO;

public interface PostService {
    PostFeedListVO getPosts(PostFeedDTO postFeedDTO);
}
