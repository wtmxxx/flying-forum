package com.atcumt.model.ai.entity;

import com.atcumt.model.common.entity.MediaFile;
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
public class Message {
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
    private List<MediaFile> files;
    private String status;
    private LocalDateTime createTime;
}