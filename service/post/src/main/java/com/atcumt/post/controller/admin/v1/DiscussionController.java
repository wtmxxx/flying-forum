package com.atcumt.post.controller.admin.v1;

import com.atcumt.common.exception.AuthorizationException;
import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.DiscussionUpdateDTO;
import com.atcumt.model.post.vo.DiscussionPostVO;
import com.atcumt.post.service.DiscussionService;
import com.atcumt.post.service.admin.AdminDiscussionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("discussionControllerAdminV1")
@RequestMapping("/api/post/admin/discussion/v1")
@Tag(name = "AdminDiscussion", description = "管理员杂谈相关接口")
@RequiredArgsConstructor
@Slf4j
public class DiscussionController {
    private final DiscussionService discussionService;
    private final AdminDiscussionService adminDiscussionService;

    @PatchMapping("/")
    @Operation(summary = "修改杂谈", description = "修改杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<DiscussionPostVO> updateDiscussion(@RequestBody DiscussionUpdateDTO discussionUpdateDTO) throws AuthorizationException {
        log.info("修改杂谈, authorId: {}", UserContext.getUserId());

        DiscussionPostVO discussionPostVO = adminDiscussionService.updateDiscussion(discussionUpdateDTO);

        return Result.success(discussionPostVO);
    }

    @DeleteMapping("/")
    @Operation(summary = "删除杂谈", description = "删除杂谈")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "discussionId", description = "杂谈ID", required = true),
            @Parameter(name = "isLogic", description = "是否为逻辑删除，默认为true")
    })
    public Result<Object> deleteDiscussion(Long discussionId, Boolean isLogic) {
        log.info("删除杂谈, authorId: {}", UserContext.getUserId());

        if (isLogic == null) isLogic = true;

        // 删除帖子
        if (isLogic) adminDiscussionService.deleteDiscussion(discussionId);
        else adminDiscussionService.deleteDiscussionComplete(discussionId);

        return Result.success();
    }
}
