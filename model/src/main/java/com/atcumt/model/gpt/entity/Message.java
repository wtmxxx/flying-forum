package com.atcumt.model.gpt.entity;

import com.baomidou.mybatisplus.annotation.TableId;
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
    @TableId
    private String messageId;
    private String conversationId;
    private String role;
    private String content;
    private String citations;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
