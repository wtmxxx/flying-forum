package com.atcumt.post.controller.admin.v1;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.NewsDTO;
import com.atcumt.model.post.enums.PostMessage;
import com.atcumt.post.service.admin.AdminNewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("newsControllerAdminV1")
@RequestMapping("/api/post/admin/news/v1")
@Tag(name = "AdminNews", description = "管理员新闻相关接口")
@RequiredArgsConstructor
@Slf4j
public class NewsController {
    private final AdminNewsService adminNewsService;

    @PostMapping("")
    @Operation(summary = "上传新闻", description = "上传新闻")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> uploadNews(@RequestBody List<NewsDTO> newsDTOs) throws AuthorizationException {
        log.info("上传新闻");

        if (newsDTOs == null || newsDTOs.isEmpty()) {
            throw new IllegalArgumentException(PostMessage.NEWS_LIST_EMPTY.getMessage());
        } else if (newsDTOs.size() > 1005) {
            throw new IllegalArgumentException(PostMessage.NEWS_COUNT_TOO_MANY.getMessage());
        }

        adminNewsService.uploadNews(newsDTOs);

        return Result.success();
    }
}
