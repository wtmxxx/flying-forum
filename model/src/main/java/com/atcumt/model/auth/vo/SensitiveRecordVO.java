package com.atcumt.model.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveRecordVO {
    private String recordId;
    private String userId;
    private String type;
    private String description;
    private String ip;
    private String region;
    private LocalDateTime recordTime;
}
