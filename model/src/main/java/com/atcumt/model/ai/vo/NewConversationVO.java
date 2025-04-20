package com.atcumt.model.ai.vo;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewConversationVO {
    private String type = "newConversation";
    private String conversationId;
    private Integer messageId;
    private Integer parentId;
}
