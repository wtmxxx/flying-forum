package com.atcumt.gpt.controller.user.v2;

import com.atcumt.common.utils.UserContext;
import com.atcumt.gpt.service.ConversationService;
import com.atcumt.gpt.service.MessageService;
import com.atcumt.model.common.PageQueryDTO;
import com.atcumt.model.common.PageQueryVO;
import com.atcumt.model.common.Result;
import com.atcumt.model.gpt.dto.ConversationDTO;
import com.atcumt.model.gpt.vo.ConversationPageVO;
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
@RequestMapping("/api/v2/gpt/user")
@Tag(name = "GPT", description = "GPT相关接口")
@RequiredArgsConstructor
@Slf4j
public class GptController {
    private final ConversationService conversationService;
    private final MessageService messageService;

    @PostMapping("/c")
    @Operation(summary = "新建对话")
    public Result<ConversationVO> newConversation() {
        log.info("新建对话, userId: {}", UserContext.getUser());

        ConversationVO conversationVO = conversationService.newChat(
                ConversationDTO
                        .builder()
                        .userId(UserContext.getUser())
                        .build());
        return Result.success(conversationVO);
    }

    @GetMapping("/c")
    @Operation(summary = "获取分页对话标题")
    public Result<PageQueryVO<ConversationPageVO>> getConversationTitles(@RequestBody PageQueryDTO pageQueryDTO) {
        log.info("获取分页对话标题, page: {}, size: {}", pageQueryDTO.getPage(), pageQueryDTO.getSize());

        // 分页查询参数校验
        pageQueryDTO.checkParam();
        PageQueryVO<ConversationPageVO> pageQueryVO = conversationService.getConversationTitles(pageQueryDTO);

        return Result.success(pageQueryVO);
    }

    @GetMapping("/c/{conversationId}")
    @Operation(summary = "获取对话内容")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true)
    })
    public Result<ConversationVO> getConversation(@PathVariable String conversationId) {
        log.info("获取对话内容, conversationId: {}", conversationId);

        ConversationVO conversationVO = conversationService.getConversation(conversationId);
        return Result.success(conversationVO);
    }

    @DeleteMapping("/messages")
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

    @GetMapping("/title")
    @Operation(summary = "获取标题")
    @Parameters({
            @Parameter(name = "conversationId", description = "对话ID", required = true)
    })
    public Result<Map<String, String>> getTitle(String conversationId) {
        log.info("获取标题, conversationId: {}", conversationId);

        String title = conversationService.getTitle(conversationId);
        return Result.success(Map.of("title", title));
    }

    @PatchMapping("/title")
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

    @DeleteMapping("/c")
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
