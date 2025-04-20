package com.atcumt.ai.controller.user.v1;

//@RestController("aiControllerV1")
//@RequestMapping("/api/ai/user/v1")
//@Tag(name = "AI", description = "AI相关接口")
//@RequiredArgsConstructor
//@Slf4j
//public class GptController {
//    private final ConversationService conversationService;
//    private final MessageService messageService;
//
//    @PostMapping("/conversation")
//    @Operation(summary = "新建对话")
//    @Parameters({
//            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
//    })
//    public Result<ConversationVO> newConversation() {
//        log.info("新建对话, userId: {}", UserContext.getUserId());
//
//        ConversationVO conversationVO = conversationService.newChat(
//                ConversationDTO
//                        .builder()
//                        .userId(UserContext.getUserId())
//                        .build());
//        return Result.success(conversationVO);
//    }
//
//    @GetMapping("/conversations")
//    @Operation(summary = "获取分页对话标题")
//    @Parameters({
//            @Parameter(name = "page", description = "页码", example = "1", required = true),
//            @Parameter(name = "size", description = "每页的记录数", example = "10", required = true)
//    })
//    public Result<PageQueryVO<ConversationPageVO>> getConversationTitles(Long page, Long size) {
//        log.info("获取分页对话标题, page: {}, size: {}", page, size);
//
//        PageQueryDTO pageQueryDTO = PageQueryDTO
//                .builder()
//                .page(page)
//                .size(size)
//                .build();
//
//        // 分页查询参数校验
//        pageQueryDTO.checkParam();
//        PageQueryVO<ConversationPageVO> pageQueryVO = conversationService.getConversationTitles(pageQueryDTO);
//
//        return Result.success(pageQueryVO);
//    }
//
//    @GetMapping("/conversation/{conversationId}")
//    @Operation(summary = "获取对话内容")
//    @Parameters({
//            @Parameter(name = "conversationId", description = "对话ID", required = true)
//    })
//    public Result<ConversationVO> getConversation(@PathVariable String conversationId) throws UnauthorizedException {
//        log.info("获取对话内容, conversationId: {}", conversationId);
//
//        ConversationVO conversationVO = conversationService.getConversation(conversationId);
//        return Result.success(conversationVO);
//    }
//
//    @DeleteMapping("/messages")
//    @Operation(
//            summary = "删除消息及其后续消息",
//            description = "删除此ID及之后的所有消息，之后请再次使用WebSocket发送新消息请求"
//    )
//    @Parameters({
//            @Parameter(name = "messageId", description = "消息ID", required = true),
//    })
//    public Result<Object> deleteMessages(String messageId) {
//        log.info("修改消息内容, messageId: {}", messageId);
//
//        messageService.deleteMessages(messageId);
//
//        return Result.success();
//    }
//
//    @GetMapping("/title")
//    @Operation(summary = "获取标题")
//    @Parameters({
//            @Parameter(name = "conversationId", description = "对话ID", required = true)
//    })
//    public Result<Map<String, String>> getTitle(String conversationId) throws UnauthorizedException {
//        log.info("获取标题, conversationId: {}", conversationId);
//
//        String title = conversationService.getTitle(conversationId);
//        return Result.success(Map.of("title", title));
//    }
//
//    @PatchMapping("/title")
//    @Operation(summary = "修改标题")
//    @Parameters({
//            @Parameter(name = "conversationId", description = "对话ID", required = true),
//            @Parameter(name = "title", description = "title内容", required = true)
//    })
//    public Result<Object> setTitle(String conversationId, String title) throws UnauthorizedException {
//        log.info("修改标题, conversationId: {}", conversationId);
//
//        conversationService.setTitle(conversationId, title);
//
//        return Result.success();
//    }
//
//    @DeleteMapping("/conversation")
//    @Operation(summary = "删除对话")
//    @Parameters({
//            @Parameter(name = "conversationId", description = "对话ID", required = true),
//    })
//    public Result<Object> deleteConversation(String conversationId) throws UnauthorizedException {
//        log.info("删除对话, conversationId: {}", conversationId);
//
//        conversationService.deleteConversation(conversationId);
//
//        return Result.success();
//    }
//
//}