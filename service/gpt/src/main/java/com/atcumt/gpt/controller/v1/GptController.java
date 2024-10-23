package com.atcumt.gpt.controller.v1;

import com.atcumt.gpt.service.ConversationService;
import com.atcumt.gpt.service.MessageService;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("gptControllerV1")
@RequestMapping("/api/gpt/v1")
@RequiredArgsConstructor
@Slf4j
public class GptController {
    private final ConversationService conversationService;
    private final MessageService messageService;
    private final Gson gson;

//    @PostMapping("/send")
//    @Operation(summary = "发送消息给GPT(流式输出)")
//    @Parameters({
//            @Parameter(name = "conversationId", description = "对话ID", required = true),
//            @Parameter(name = "content", description = "消息内容", required = true)
//    })
//    public Flux<Result<MessageVO>> sendMessageStream(@RequestBody MessageDTO messageDTO) {
//        return messageService.processChatStreamFlux(messageDTO)
//                .map(Result::success);  // 返回流式结果
//    }
}