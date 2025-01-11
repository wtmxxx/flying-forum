package com.atcumt.like.service;

import com.atcumt.model.like.dto.CommentLikeDTO;
import com.atcumt.model.like.dto.PostLikeDTO;
import com.atcumt.model.like.vo.UserPostLikeVO;
import com.atcumt.model.user.dto.UserLikeDTO;

public interface LikeService {
    void likePost(PostLikeDTO postLikeDTO);

    void likeComment(CommentLikeDTO commentLikeDTO);

    UserPostLikeVO getUserLikes(UserLikeDTO userLikeDTO) throws InterruptedException;
}
