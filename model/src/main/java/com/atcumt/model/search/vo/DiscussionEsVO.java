package com.atcumt.model.search.vo;

import com.atcumt.model.common.vo.MediaFileVO;
import com.atcumt.model.search.entity.TagSimpleEs;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class DiscussionEsVO extends SearchEsVO {
    private String postType;
    private String postId;
    private String userId;
    private UserInfoSimpleVO userInfo;
    private String title;
    private String excerpt;
    private String content;
    private List<MediaFileVO> mediaFiles;
    private List<TagSimpleEs> tags;
    private Integer likeCount;
    private Integer commentCount;
    private String status;
    private Double score;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Map<String, List<String>> highlight;
}
