package com.atcumt.model.ai.vo;

import com.atcumt.model.ai.entity.AiSearch;
import com.atcumt.model.common.vo.MediaFileVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageVO {
    private Integer messageId;
    private Integer parentId;
    private String model;
    private String role;
    private String content;
    private Boolean reasoningEnabled;
    private String reasoningContent;
    private Integer reasoningTime;
    private String reasoningStatus;
    private Boolean searchEnabled;
    private List<AiSearch> searchResults;
    private String searchStatus;
    private List<MediaFileVO> files;
    private String status;
    private LocalDateTime createTime;
}
