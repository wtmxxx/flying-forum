package com.atcumt.gpt.api;

import com.atcumt.gpt.service.ConversationService;
import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.vo.ConversationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service/api/gpt/v2")
@RequiredArgsConstructor
public class UserClient {
    private final ConversationService conversationService;

    @GetMapping("/c/{conversationId}")
    public Result<ConversationVO> getConversation(@PathVariable String conversationId) {

        ConversationVO conversationVO = conversationService.getConversation(conversationId);
        return Result.success(conversationVO);
    }
}
