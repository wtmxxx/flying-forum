package com.atcumt.model.like.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "comment_like")
@TypeAlias("CommentLike")
public class CommentLike {
    @MongoId
    private Long likeId;
    @Indexed
    private String action;
    @Indexed
    private String commentType;
    @Indexed
    private Long commentId;
    @Indexed
    private String userId;
    @Indexed
    private LocalDateTime createTime;
}
