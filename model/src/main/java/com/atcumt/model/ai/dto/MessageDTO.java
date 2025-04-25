package com.atcumt.model.ai.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO {
    private String conversationId;
    private String textContent;
}
