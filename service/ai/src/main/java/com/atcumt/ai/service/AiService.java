package com.atcumt.ai.service;

import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.vo.ConversationPageVO;
import com.atcumt.model.ai.vo.ConversationVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiService {
    SseEmitter conversation(ConversationDTO conversationDTO) throws Exception;

    void stopConversation(StopConversationDTO stopConversationDTO);

    ConversationVO getConversation(String conversationId);

    SimplePageQueryVO<ConversationPageVO> getConversations(PageQueryDTO pageQueryDTO);

    void deleteConversation(String conversationId);

    void editTitle(TitleDTO titleDTO);
}
