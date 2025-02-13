package com.atcumt.forum.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.forum.service.SensitiveWordService;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordContainsDTO;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordFindDTO;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordFindVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("sensitiveWordControllerV1")
@RequestMapping("/api/forum/sensitive-word/v1")
@Tag(name = "SensitiveWord", description = "敏感词相关接口")
@RequiredArgsConstructor
@Slf4j
public class SensitiveWordController {
    private final SensitiveWordService sensitiveWordService;

    @PostMapping("/contains")
    @Operation(summary = "检测敏感词", description = "检测是否包含敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Boolean> sensitiveWordContains(@RequestBody SensitiveWordContainsDTO wordContainsDTO) {
        if (wordContainsDTO == null || wordContainsDTO.getContent() == null) {
            return Result.success(false);
        }
        String content = wordContainsDTO.getContent();
        log.info("检测敏感词, userId: {}, content: {}...", UserContext.getUserId(), content.substring(0, Math.min(content.length(), 10)));

        boolean contains = sensitiveWordService.contains(content);

        return Result.success(contains);
    }

    @PostMapping("/find")
    @Operation(summary = "查询敏感词", description = "查询文本中的敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<SensitiveWordFindVO> findSensitiveWord(@RequestBody SensitiveWordFindDTO wordFindDTO) {
        if (wordFindDTO == null || wordFindDTO.getContent() == null) {
            return Result.success(SensitiveWordFindVO.builder().contains(false).build());
        }
        String content = wordFindDTO.getContent();
        log.info("查询敏感词, userId: {}, content: {}...", UserContext.getUserId(), content.substring(0, Math.min(content.length(), 10)));

        Boolean findFirst = wordFindDTO.getFindFirst();
        if (findFirst == null) {
            findFirst = true;
        }
        SensitiveWordFindVO wordFindVO = sensitiveWordService.find(content, findFirst);

        return Result.success(wordFindVO);
    }
}