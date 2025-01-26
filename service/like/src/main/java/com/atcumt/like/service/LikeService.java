package com.atcumt.like.service;

import com.atcumt.model.like.dto.CommentLikeDTO;
import com.atcumt.model.like.dto.PostLikeDTO;
import com.atcumt.model.like.dto.PostUserLikeDTO;
import com.atcumt.model.like.dto.UserLikeDTO;
import com.atcumt.model.like.vo.PostUserLikeVO;
import com.atcumt.model.like.vo.UserPostLikeVO;

public interface LikeService {
    void likePost(PostLikeDTO postLikeDTO);

    void likeComment(CommentLikeDTO commentLikeDTO);

    UserPostLikeVO getUserLikes(UserLikeDTO userLikeDTO) throws InterruptedException;

    PostUserLikeVO getPostLikes(PostUserLikeDTO postUserLikeDTO);
}
