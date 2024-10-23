package com.atcumt.gpt.service;

import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.vo.ConversationVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ConversationService extends IService<Conversation> {
    ConversationVO newChat(ConversationDTO conversationDTO);

    String getTitle(Long conversationId);

    void setTitle(Long conversationId, String title);

    void deleteConversation(Long conversationId);
}
