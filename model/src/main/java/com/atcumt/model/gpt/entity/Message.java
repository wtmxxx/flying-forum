package com.atcumt.model.gpt.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private Long id;
    private Long conversationId;
    private String role;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
