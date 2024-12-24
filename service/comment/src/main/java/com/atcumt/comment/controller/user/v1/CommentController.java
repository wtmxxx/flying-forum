package com.atcumt.comment.controller.user.v1;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("commentControllerV1")
@RequestMapping("/api/forum/comment/v1")
@Tag(name = "Comment", description = "评论相关接口")
@RequiredArgsConstructor
@Slf4j
public class CommentController {
}
