package com.atcumt.gpt.controller.v2;

import com.atcumt.gpt.service.ConversationService;
import com.atcumt.gpt.service.MessageService;
import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.vo.ConversationVO;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("gptControllerV2")
@RequestMapping("/api/gpt/v2")
@RequiredArgsConstructor
@Slf4j
public class GptController {
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final Gson gson;

    @PostMapping("/new_chat")
    @Operation(summary = "新建对话")
    @Parameters({
            @Parameter(name = "userId", description = "用户ID", required = true)
    })
    public Result<ConversationVO> newConversation(@RequestBody ConversationDTO conversationDTO) {
        log.info("新建对话, userId: {}", conversationDTO.getUserId());

        ConversationVO conversationVO = conversationService.newChat(conversationDTO);
        return Result.success(conversationVO);
    }

    @GetMapping("/get_title")
    @Operation(summary = "获取标题")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true)
    })
    public Result<Map<String, String>> newConversation(String conversationId) {
        log.info("获取标题, conversationId: {}", conversationId);

        String title = conversationService.getTitle(conversationId);
        return Result.success(Map.of("title", title));
    }

    @PutMapping("/set_title")
    @Operation(summary = "修改标题")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true),
            @Parameter(name = "title", description = "title内容", required = true)
    })
    public Result<Object> newConversation(String conversationId, String title) {
        log.info("修改标题, conversationId: {}", conversationId);

        conversationService.setTitle(conversationId, title);

        return Result.success();
    }

    @DeleteMapping("/delete_conversation")
    @Operation(summary = "删除对话")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true),
    })
    public Result<Object> deleteConversation(String conversationId) {
        log.info("删除对话, conversationId: {}", conversationId);

        conversationService.deleteConversation(conversationId);

        return Result.success();
    }

}
