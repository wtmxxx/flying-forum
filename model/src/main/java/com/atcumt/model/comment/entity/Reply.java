package com.atcumt.model.comment.entity;

import com.atcumt.model.common.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "reply")
@TypeAlias("Reply")
@CompoundIndexes({
        @CompoundIndex(name = "replyToId_score_replyId_idx", def = "{'replyToId': -1, 'score': -1, 'replyId': -1}"),
        @CompoundIndex(name = "replyToId_createTime_replyId_idx", def = "{'replyToId': -1, 'createTime': -1, 'replyId': -1}"),
        @CompoundIndex(name = "userId_createTime_replyId_idx", def = "{'userId': 1, 'createTime': -1, 'replyId': -1}")
})
public class Reply {
    @MongoId
    private Long replyId;
    private Long replyToId;
    @Indexed
    private String replyToUserId;
    @Indexed
    private Long rootCommentId;
    @Indexed
    private String postType;
    @Indexed
    private Long postId;
    private String userId;
    private String content;
    private List<MediaFile> mediaFiles;
    private Integer likeCount;
    private Integer dislikeCount;
    @Indexed(direction = IndexDirection.DESCENDING)
    private Double score;
    @Indexed
    private LocalDateTime createTime;
}
