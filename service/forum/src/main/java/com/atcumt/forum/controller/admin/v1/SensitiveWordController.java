package com.atcumt.forum.controller.admin.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.forum.service.admin.SensitiveWordAdminService;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.forum.sensitive.dto.SensitiveWordListDTO;
import com.atcumt.model.forum.sensitive.entity.SensitiveWordConfig;
import com.atcumt.model.forum.sensitive.vo.SensitiveWordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController("sensitiveWordControllerAdminV1")
@RequestMapping("/api/forum/admin/sensitive-word/v1")
@Tag(name = "AdminSensitiveWord", description = "管理员敏感词相关接口")
@RequiredArgsConstructor
@Slf4j
public class SensitiveWordController {
    private final SensitiveWordAdminService sensitiveWordAdminService;

    @PostMapping("/add")
    @Operation(summary = "新增敏感词", description = "新增敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> addSensitiveWord(@RequestBody SensitiveWordListDTO sensitiveWordListDTO) {
        log.info("新增敏感词, userId: {}", UserContext.getUserId());

        sensitiveWordAdminService.addSensitiveWord(sensitiveWordListDTO);

        return Result.success();
    }

    @PostMapping("/remove")
    @Operation(summary = "删除敏感词", description = "删除敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> addSensitiveWord(@RequestBody List<String> words) {
        log.info("删除敏感词, userId: {}", UserContext.getUserId());

        sensitiveWordAdminService.removeSensitiveWord(words);

        return Result.success();
    }

    @GetMapping("/words")
    @Operation(summary = "获取敏感词", description = "获取敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "type", description = "类型", example = "DENY", in = ParameterIn.QUERY),
            @Parameter(name = "tag", description = "标签", example = "全局", in = ParameterIn.QUERY)
    })
    public Result<List<SensitiveWordVO>> getSensitiveWords(
            @RequestParam(name = "type", defaultValue = "ALL") String type,
            @RequestParam(name = "tag", defaultValue = "ALL") String tag
    ) {
        log.info("获取敏感词, type: {}, tag: {}, userId: {}", type, tag, UserContext.getUserId());

        List<SensitiveWordVO> sensitiveWords = sensitiveWordAdminService.getSensitiveWords(type, tag);

        return Result.success(sensitiveWords);
    }

    @PostMapping("/config")
    @Operation(summary = "配置敏感词", description = "配置敏感词")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> configSensitiveWord(@RequestBody SensitiveWordConfig sensitiveWordConfig) {
        log.info("配置敏感词, userId: {}", UserContext.getUserId());

        sensitiveWordAdminService.configSensitiveWord(sensitiveWordConfig);

        return Result.success();
    }

    @GetMapping("/config")
    @Operation(summary = "获取敏感词配置", description = "获取敏感词配置")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<SensitiveWordConfig> getSensitiveWordConfig() {
        log.info("获取敏感词配置, userId: {}", UserContext.getUserId());

        SensitiveWordConfig sensitiveWordConfig = sensitiveWordAdminService.getSensitiveWordConfig();

        return Result.success(sensitiveWordConfig);
    }

    @GetMapping("/tags")
    @Operation(summary = "获取敏感词标签", description = "获取敏感词标签")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "word", description = "敏感词", in = ParameterIn.QUERY)
    })
    public Result<Set<String>> getSensitiveWordTags(@RequestParam(name = "word") String word) {
        log.info("获取敏感词标签, userId: {}", UserContext.getUserId());

        Set<String> sensitiveWordTags = sensitiveWordAdminService.getSensitiveWordTags(word);

        return Result.success(sensitiveWordTags);
    }
}
