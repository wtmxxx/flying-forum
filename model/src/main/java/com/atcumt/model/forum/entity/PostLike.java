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
@Document(collection = "post_like")
@TypeAlias("PostLike")
@CompoundIndexes({
        @CompoundIndex(name = "post_user_type_index", def = "{'postId': 1, 'userId': 1, 'postType': 1}", unique = true)
})
public class PostLike {
    @MongoId
    private Long likeId;
    @Indexed
    private Long postId;
    @Indexed
    private String userId;
    private String postType;
    private LocalDateTime createTime;
}