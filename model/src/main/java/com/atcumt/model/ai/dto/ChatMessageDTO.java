package com.atcumt.model.ai.dto;

import com.atcumt.model.ai.entity.WebSearch;
import com.atcumt.model.ai.enums.AiStatus;
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
public class ChatMessageDTO {
    private String conversationId;
    private String userId;
    private Integer messageId;
    private Integer parentId;
    private String model;
    private String role;
    private String textContent;
    @Builder.Default
    private Boolean reasoningEnabled = false;
    @Builder.Default
    private Integer reasoningTime = -1;
    @Builder.Default
    private String reasoningStatus = AiStatus.UNUSED.getValue();
    @Builder.Default
    private Boolean searchEnabled = false;
    @Builder.Default
    private List<WebSearch> searchResults = List.of();
    @Builder.Default
    private String searchStatus = AiStatus.UNUSED.getValue();
    @Builder.Default
    private List<MediaFile> mediaFiles = List.of();
    private String status;
    @Builder.Default
    private LocalDateTime createTime = LocalDateTime.now();
}
