package com.atcumt.post.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.NewTagDTO;
import com.atcumt.model.post.vo.TagListVO;
import com.atcumt.model.post.vo.TagVO;
import com.atcumt.post.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("tagControllerV1")
@RequestMapping("/api/post/tag/v1")
@Tag(name = "Tag", description = "标签相关接口")
@RequiredArgsConstructor
@Slf4j
public class TagController {
    private final TagService tagService;

    @PostMapping("")
    @Operation(summary = "新建或查询标签", description = "新建或查询标签，如果标签不存在则新建，否则返回已有标签")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<TagListVO> newTag(@RequestBody NewTagDTO newTagDTO) {
        log.info("新建或查询标签, userId: {}, tagNames: {}", UserContext.getUserId(), newTagDTO.getTagNames());

        TagListVO tags = tagService.newTag(newTagDTO);

        return Result.success(tags);
    }

    @GetMapping("/{tagId}")
    @Operation(summary = "获取标签", description = "获取标签")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<TagVO> getTag(@PathVariable("tagId") Long tagId) {
        log.info("获取标签, tagId: {}", tagId);

        TagVO tagVO = tagService.getTag(tagId);

        return Result.success(tagVO);
    }
}
