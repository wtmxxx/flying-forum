package com.atcumt.model.gpt.vo;

import com.atcumt.model.gpt.entity.Message;
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
public class ConversationVO {
    private String id;
    private String userId;
    private String title;
    private List<Message> messages;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
