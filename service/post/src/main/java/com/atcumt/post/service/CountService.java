package com.atcumt.post.service;

import com.atcumt.model.post.dto.PostViewCountDTO;

public interface CountService {
    void incrPostViewCount(PostViewCountDTO postViewCountDTO);
}
