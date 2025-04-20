package com.atcumt.search.controller.admin.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.search.dto.SuggestionEsDTO;
import com.atcumt.model.search.enums.SuggestionType;
import com.atcumt.search.service.admin.AdminSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController("adminSearchControllerV1")
@RequestMapping("/api/search/admin/v1")
@Tag(name = "AdminSearch", description = "管理员搜索相关接口")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final AdminSearchService adminSearchService;

    @PostMapping("/suggest")
    @Operation(summary = "新增搜索提示", description = "新增搜索提示")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<Object> suggest(@RequestBody SuggestionEsDTO suggestionEsDTO) throws IOException {
        log.info("新增搜索提示, userId: {}", UserContext.getUserId());

        adminSearchService.newCustomSuggestions(suggestionEsDTO);

        return Result.success();
    }

    @DeleteMapping("/suggest")
    @Operation(summary = "删除搜索提示", description = "删除搜索提示")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "suggestion", description = "搜索提示", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "type", description = "搜索提示类型，默认无论何种类型", in = ParameterIn.QUERY)
    })
    public Result<Object> delete(
            @RequestParam("suggestion") String suggestion,
            @RequestParam(value = "type", required = false) String type
    ) {
        log.info("删除搜索提示, suggestion: {}, type: {}, userId: {}", suggestion, type, UserContext.getUserId());

        if (type == null || type.isEmpty()) {
            type = SuggestionType.WHATEVER_TYPE.getValue();
        }
        adminSearchService.deleteSuggestion(suggestion, type);

        return Result.success();
    }
}
