package com.atcumt.search.controller.admin.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.search.dto.SuggestionEsDTO;
import com.atcumt.search.service.admin.AdminSearchService;
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

        adminSearchService.newSuggest(suggestionEsDTO);

        return Result.success();
    }
}
