package com.atcumt.post.controller.admin.v1;

import com.atcumt.model.common.entity.Result;
import com.atcumt.post.service.admin.AdminTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("tagControllerAdminV1")
@RequestMapping("/api/post/admin/tag/v1")
@Tag(name = "AdminTag", description = "管理员标签相关接口")
@RequiredArgsConstructor
@Slf4j
public class TagController {
    private final AdminTagService adminTagService;

    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签", description = "删除标签")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> deleteTag(@PathVariable("tagId") Long tagId) {
        log.info("删除标签, tagId: {}", tagId);

        adminTagService.deleteTag(tagId);

        return Result.success();
    }
}
