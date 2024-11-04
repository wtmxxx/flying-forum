package com.atcumt.gpt.service;

import com.atcumt.model.common.PageQueryDTO;
import com.atcumt.model.common.PageQueryVO;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.vo.ConversationPageVO;
import com.atcumt.model.gpt.vo.ConversationVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ConversationService extends IService<Conversation> {
    ConversationVO newChat(ConversationDTO conversationDTO);

    String getTitle(String conversationId);

    void setTitle(String conversationId, String title);

    void deleteConversation(String conversationId);

    ConversationVO getConversation(String conversationId);

    PageQueryVO<ConversationPageVO> getConversationTitles(PageQueryDTO pageQueryDTO);
}
