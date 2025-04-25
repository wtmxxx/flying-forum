package com.atcumt.ai.controller.user.v1;

import com.atcumt.ai.service.AiService;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.ai.dto.ConversationDTO;
import com.atcumt.model.ai.dto.StopConversationDTO;
import com.atcumt.model.ai.dto.TitleDTO;
import com.atcumt.model.ai.vo.ConversationPageVO;
import com.atcumt.model.ai.vo.ConversationVO;
import com.atcumt.model.ai.vo.FluxVO;
import com.atcumt.model.common.dto.PageQueryDTO;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.common.vo.SimplePageQueryVO;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController("aiControllerV1")
@RequestMapping("/api/ai/user/v1")
@Tag(name = "AI", description = "AI相关接口")
@RequiredArgsConstructor
@Slf4j
public class AiController {
    private final AiService aiService;

    @PostMapping(path = "/conversation", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "对话")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Flux<FluxVO> getConversation(@RequestBody ConversationDTO conversationDTO) throws Exception {
        log.info("对话, userId: {}, conversationId: {}", UserContext.getUserId(), conversationDTO.getConversationId());

        return aiService.conversation(conversationDTO);
    }

    @PostMapping("/conversation/logger/api/v1/services/aigc/text-generation/generation")
    @Operation(summary = "对话日志")
    public Result<Object> getConversationLogger(@RequestBody JsonNode jsonNode) {
        log.info("对话日志: {}", jsonNode);

        return Result.success();
    }

    @GetMapping("/conversation/{conversationId}")
    @Operation(summary = "获取对话历史消息")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<ConversationVO> getConversation(@PathVariable(name = "conversationId") String conversationId) {
        log.info("获取对话历史消息, userId: {}, conversationId: {}", UserContext.getUserId(), conversationId);

        ConversationVO conversationVO = aiService.getConversation(conversationId);

        return Result.success(conversationVO);
    }

    @GetMapping("/conversations")
    @Operation(summary = "获取分页对话")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "page", description = "页码", example = "1", required = true),
            @Parameter(name = "size", description = "每页的记录数", example = "10", required = true)
    })
    public Result<SimplePageQueryVO<ConversationPageVO>> getConversations(Long page, Long size) {
        log.info("获取分页对话, userId: {}", UserContext.getUserId());

        PageQueryDTO pageQueryDTO = PageQueryDTO
                .builder()
                .page(page)
                .size(size)
                .build();
        // 分页查询参数校验
        pageQueryDTO.checkParam();

        SimplePageQueryVO<ConversationPageVO> conversations = aiService.getConversations(pageQueryDTO);

        return Result.success(conversations);
    }

    @DeleteMapping("/conversation/{conversationId}")
    @Operation(summary = "删除对话")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> deleteConversation(@PathVariable(name = "conversationId") String conversationId) {
        log.info("删除对话, userId: {}, conversationId: {}", UserContext.getUserId(), conversationId);

        aiService.deleteConversation(conversationId);

        return Result.success();
    }

    @PostMapping("/stopConversation")
    @Operation(summary = "中止对话")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> stopConversation(@RequestBody StopConversationDTO stopConversationDTO) {
        log.info("中止对话, userId: {}, conversationId: {}", UserContext.getUserId(), stopConversationDTO.getConversationId());

        aiService.stopConversation(stopConversationDTO);

        return Result.success();
    }

    @PatchMapping("/conversation/title")
    @Operation(summary = "重命名标题")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> editTitle(@RequestBody TitleDTO titleDTO) {
        log.info("重命名标题, titleDTO: {}", titleDTO);

        aiService.editTitle(titleDTO);

        return Result.success();
    }
}
