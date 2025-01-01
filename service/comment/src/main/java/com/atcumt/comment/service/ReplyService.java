package com.atcumt.comment.service;

import com.atcumt.model.comment.dto.CommentReplyDTO;
import com.atcumt.model.comment.dto.PostCommentCountDTO;
import com.atcumt.model.comment.dto.ReplyDTO;
import com.atcumt.model.comment.dto.UserReplyDTO;
import com.atcumt.model.comment.vo.CommentReplyVO;
import com.atcumt.model.comment.vo.ReplyVO;
import com.atcumt.model.comment.vo.UserReplyVO;

public interface ReplyService {
    ReplyVO postReply(ReplyDTO replyDTO);

    ReplyVO getReply(Long replyId);

    void deleteReply(Long replyId);

    CommentReplyVO getCommentReplies(CommentReplyDTO commentReplyDTO);

    UserReplyVO getUserReplies(UserReplyDTO userReplyDTO);

    void changePostComment(PostCommentCountDTO postCommentCountDTO);
}
