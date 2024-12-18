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
public class Conversation {
    @TableId
    private String conversationId;
    private String userId;
    private String title;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
