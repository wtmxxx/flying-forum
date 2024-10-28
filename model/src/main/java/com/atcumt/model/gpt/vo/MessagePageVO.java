package com.atcumt.model.gpt.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessagePageVO {
    private String id;
    private String role;
    private String content;
    private String citations;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

