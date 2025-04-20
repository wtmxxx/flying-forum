package com.atcumt.search.controller.user.v1;

import com.atcumt.common.utils.UserContext;
import com.atcumt.model.common.entity.Result;
import com.atcumt.model.search.dto.PostSearchDTO;
import com.atcumt.model.search.dto.TagSearchDTO;
import com.atcumt.model.search.dto.UserSearchDTO;
import com.atcumt.model.search.enums.PostSearchSortType;
import com.atcumt.model.search.enums.SearchContentType;
import com.atcumt.model.search.enums.SearchTimeLimit;
import com.atcumt.model.search.enums.UserSearchSortType;
import com.atcumt.model.search.vo.SearchVO;
import com.atcumt.model.search.vo.SuggestionEsListVO;
import com.atcumt.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Collections;

@RestController("searchControllerV1")
@RequestMapping("/api/search/v1")
@Tag(name = "Search", description = "搜索相关接口")
@RequiredArgsConstructor
@Slf4j
public class SearchController {
    private final SearchService searchService;
    private final RocketMQTemplate rocketMQTemplate;

    @GetMapping("/suggest")
    @Operation(summary = "搜索提示", description = "获取搜索提示")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "text", description = "搜索文本", in = ParameterIn.QUERY, required = true)
    })
    public Result<SuggestionEsListVO> suggest(String text) throws IOException {
        log.info("搜索提示, userId: {}", UserContext.getUserId());

        SuggestionEsListVO suggestionEsListVO = searchService.suggest(text);

        return Result.success(suggestionEsListVO);
    }

    @GetMapping("/search/post")
    @Operation(summary = "搜索帖子", description = "获取搜索帖子列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "text", description = "搜索文本", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "searchContentType", description = "搜索内容类型", in = ParameterIn.QUERY),
            @Parameter(name = "searchSortType", description = "搜索排序类型", in = ParameterIn.QUERY),
            @Parameter(name = "searchTimeLimit", description = "搜索时间限制", in = ParameterIn.QUERY),
            @Parameter(name = "from", description = "搜索起始位置", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "搜索数量", in = ParameterIn.QUERY)
    })
    public Result<SearchVO> search(
            @Parameter(description = "搜索文本", required = true) String text,
            @Parameter(description = "搜索内容类型") String searchContentType,
            @Parameter(description = "搜索排序类型") String searchSortType,
            @Parameter(description = "搜索时间限制") String searchTimeLimit,
            @Parameter(description = "搜索起始位置") Integer from,
            @Parameter(description = "搜索数量") Integer size
    ) throws Exception {
        log.info("搜索帖子, userId: {}", UserContext.getUserId());

        text = text.trim();
        if (text.length() > 100) text = text.substring(0, 100);
        if (searchContentType == null || searchContentType.isEmpty()) searchContentType = SearchContentType.POST.getValue();
        if (searchSortType == null || searchSortType.isEmpty()) searchSortType = PostSearchSortType.DEFAULT.getValue();
        if (searchTimeLimit == null || searchTimeLimit.isEmpty()) searchTimeLimit = SearchTimeLimit.ALL.getValue();
        if (from == null || from < 0 || from > 1000) from = 0;
        if (size == null || size <= 0 || size > 1000) size = 10;
        if (from + size > 1000) return Result.success(new SearchVO(0, 0, Collections.emptyList()));

        PostSearchDTO postSearchDTO = PostSearchDTO
                .builder()
                .text(text)
                .searchContentType(searchContentType)
                .searchSortType(searchSortType)
                .searchTimeLimit(searchTimeLimit)
                .from(from)
                .size(size)
                .build();

        SearchVO searchVO = searchService.searchPost(postSearchDTO);

        return Result.success(searchVO);
    }

    @GetMapping("/search/user")
    @Operation(summary = "搜索用户", description = "获取搜索用户列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "text", description = "搜索文本", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "searchSortType", description = "搜索排序类型", in = ParameterIn.QUERY),
            @Parameter(name = "from", description = "搜索起始位置", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "搜索数量", in = ParameterIn.QUERY)
    })
    public Result<SearchVO> search(
            @Parameter(description = "搜索文本", required = true) String text,
            @Parameter(description = "搜索排序类型") String searchSortType,
            @Parameter(description = "搜索起始位置") Integer from,
            @Parameter(description = "搜索数量") Integer size
    ) throws Exception {
        log.info("搜索用户, userId: {}", UserContext.getUserId());

        text = text.trim();
        if (text.length() > 100) text = text.substring(0, 100);
        if (searchSortType == null || searchSortType.isEmpty()) searchSortType = UserSearchSortType.DEFAULT.getValue();
        if (from == null || from < 0 || from > 1000) from = 0;
        if (size == null || size <= 0 || size > 100) size = 10;
        if (from + size > 1000) return Result.success(new SearchVO(0, 0, Collections.emptyList()));

        UserSearchDTO userSearchDTO = UserSearchDTO
                .builder()
                .text(text)
                .searchSortType(searchSortType)
                .from(from)
                .size(size)
                .build();

        SearchVO searchVO = searchService.searchUser(userSearchDTO);

        return Result.success(searchVO);
    }

    @GetMapping("/search/tag")
    @Operation(summary = "搜索标签", description = "获取搜索标签列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "text", description = "搜索文本", in = ParameterIn.QUERY, required = true),
            @Parameter(name = "from", description = "搜索起始位置", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "搜索数量", in = ParameterIn.QUERY)
    })
    public Result<SearchVO> search(
            @Parameter(description = "搜索文本", required = true) String text,
            @Parameter(description = "搜索起始位置") Integer from,
            @Parameter(description = "搜索数量") Integer size
    ) throws Exception {
        log.info("搜索标签, userId: {}", UserContext.getUserId());

        text = text.trim();
        if (text.length() > 100) text = text.substring(0, 100);
        if (from == null || from < 0 || from > 1000) from = 0;
        if (size == null || size <= 0 || size > 50) size = 10;
        if (from + size > 1000) return Result.success(new SearchVO(0, 0, Collections.emptyList()));

        TagSearchDTO tagSearchDTO = TagSearchDTO
                .builder()
                .text(text)
                .from(from)
                .size(size)
                .build();

        SearchVO searchVO = searchService.searchTag(tagSearchDTO);

        return Result.success(searchVO);
    }

}
