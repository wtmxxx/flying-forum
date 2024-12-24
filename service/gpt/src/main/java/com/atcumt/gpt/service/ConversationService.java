package com.atcumt.gpt.service;

import com.atcumt.common.exception.UnauthorizedException;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.PageQueryVO;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.entity.Conversation;
import com.atcumt.model.gpt.vo.ConversationPageVO;
import com.atcumt.model.gpt.vo.ConversationVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface ConversationService extends IService<Conversation> {
    ConversationVO newChat(ConversationDTO conversationDTO);

    String getTitle(String conversationId) throws UnauthorizedException;

    void setTitle(String conversationId, String title) throws UnauthorizedException;

    void deleteConversation(String conversationId) throws UnauthorizedException;

    ConversationVO getConversation(String conversationId) throws UnauthorizedException;

    PageQueryVO<ConversationPageVO> getConversationTitles(PageQueryDTO pageQueryDTO);
}
