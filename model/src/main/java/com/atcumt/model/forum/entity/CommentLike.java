package com.atcumt.model.forum.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
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
@CompoundIndexes({
        @CompoundIndex(name = "comment_user_index", def = "{'commentId': 1, 'userId': 1}")
})
public class CommentLike {
    @MongoId
    private String likeId;
    @Indexed
    private String commentId;
    @Indexed
    private String userId;
    private LocalDateTime createTime;
}
