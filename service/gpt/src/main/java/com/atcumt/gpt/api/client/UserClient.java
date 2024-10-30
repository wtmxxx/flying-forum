package com.atcumt.gpt.api.client;

import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.vo.ConversationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service/api/gpt/v2")
public interface UserClient {
    @GetMapping("/c/{conversationId}")
    Result<ConversationVO> getConversation(@PathVariable String conversationId);
}
