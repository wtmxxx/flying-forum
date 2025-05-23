package com.atcumt.ai.service;

import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.vo.ConversationPageVO;
import com.atcumt.model.ai.vo.ConversationVO;
import com.atcumt.model.ai.vo.FluxVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import reactor.core.publisher.Flux;

public interface AiService {
    Flux<FluxVO> conversation(ConversationDTO conversationDTO) throws Exception;

    ConversationVO getConversation(String conversationId);

    SimplePageQueryVO<ConversationPageVO> getConversations(PageQueryDTO pageQueryDTO);

    void deleteConversation(String conversationId);

    void stopConversation(StopConversationDTO stopConversationDTO);

    void editTitle(TitleDTO titleDTO);
}
