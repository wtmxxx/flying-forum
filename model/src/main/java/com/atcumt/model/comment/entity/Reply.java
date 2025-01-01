package com.atcumt.model.comment.entity;

import com.atcumt.model.common.entity.MediaFile;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;
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
public class Reply {
    @MongoId
    private Long replyId;
    @Indexed
    private Long replyToId;
    @Indexed
    private String replyToUserId;
    @Indexed
    private Long rootCommentId;
    @Indexed
    private Long postId;
    @Indexed
    private String postType;
    @Indexed
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
