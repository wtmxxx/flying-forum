package com.atcumt.gpt.controller.v2;

import com.atcumt.gpt.service.ConversationService;
import com.atcumt.gpt.service.MessageService;
import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.vo.ConversationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController("gptControllerV2")
@RequestMapping("/api/gpt/v2")
@Tag(name = "GPT", description = "GPT相关接口")
@RequiredArgsConstructor
@Slf4j
public class GptController {
    private final ConversationService conversationService;
    private final MessageService messageService;

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

    @DeleteMapping("/delete_messages")
    @Operation(
            summary = "删除消息及其后续消息",
            description = "删除此ID及之后的所有消息，之后请再次使用WebSocket发送新消息请求"
    )
    @Parameters({
            @Parameter(name = "messageId", description = "消息ID", required = true),
    })
    public Result<Object> deleteMessages(String messageId) {
        log.info("修改消息内容, messageId: {}", messageId);

        messageService.deleteMessages(messageId);

        return Result.success();
    }

    @GetMapping("/get_title")
    @Operation(summary = "获取标题")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true)
    })
    public Result<Map<String, String>> getTitle(String conversationId) {
        log.info("获取标题, conversationId: {}", conversationId);

        String title = conversationService.getTitle(conversationId);
        return Result.success(Map.of("title", title));
    }

    @PatchMapping("/set_title")
    @Operation(summary = "修改标题")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true),
            @Parameter(name = "title", description = "title内容", required = true)
    })
    public Result<Object> setTitle(String conversationId, String title) {
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
