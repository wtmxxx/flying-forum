package com.atcumt.model.post.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TagVO {
    private Long tagId;
    @Indexed(unique = true)
    private String tagName;
    private Long viewCount;
    private Long usageCount;
    private LocalDateTime createTime;
}
