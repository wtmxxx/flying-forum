package com.atcumt.search.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.search.vo.SuggestionEsListVO;
import com.atcumt.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

@RestController("searchControllerV1")
@RequestMapping("/api/search/v1")
@Tag(name = "Search", description = "搜索相关接口")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final SearchService searchService;

    @GetMapping("/suggest")
    @Operation(summary = "搜索提示", description = "获取搜索提示")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<SuggestionEsListVO> suggest(String text) throws IOException {
        log.info("搜索提示, userId: {}", UserContext.getUserId());

        SuggestionEsListVO suggestionEsListVO = searchService.suggest(text);

        return Result.success(suggestionEsListVO);
    }
}
