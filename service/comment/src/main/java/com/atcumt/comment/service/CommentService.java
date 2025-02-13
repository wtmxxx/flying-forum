package com.atcumt.comment.service;

import com.atcumt.model.comment.dto.CommentDTO;
import com.atcumt.model.comment.dto.PostCommentDTO;
import com.atcumt.model.comment.dto.UserCommentDTO;
import com.atcumt.model.comment.vo.CommentVO;
import com.atcumt.model.comment.vo.PostCommentVO;
import com.atcumt.model.comment.vo.UserCommentVO;

public interface CommentService {
    CommentVO postComment(CommentDTO commentDTO);

    CommentVO getComment(Long commentId);

    void deleteComment(Long commentId);

    PostCommentVO getPostComments(PostCommentDTO postCommentDTO);

    UserCommentVO getUserComments(UserCommentDTO userCommentDTO);

    void pinComment(Long commentId);

    void unpinComment(Long commentId);
}
