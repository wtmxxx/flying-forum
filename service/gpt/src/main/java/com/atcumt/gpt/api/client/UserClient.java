package com.atcumt.gpt.api.client;

import com.atcumt.gpt.api.client.fallback.UserClientFallback;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.gpt.vo.ConversationVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "user-service/api/v2/gpt/user", fallbackFactory = UserClientFallback.class)
public interface UserClient {
    @GetMapping("/c/{conversationId}")
    Result<ConversationVO> getConversation(@PathVariable String conversationId);
}
