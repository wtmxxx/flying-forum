package com.atcumt.post.controller.user.v1;

import com.atcumt.model.common.entity.Result;
import com.atcumt.model.post.dto.NewsListDTO;
import com.atcumt.model.post.vo.NewsListVO;
import com.atcumt.model.post.vo.NewsVO;
import com.atcumt.post.service.NewsService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController("newsControllerV1")
@RequestMapping("/api/post/news/v1")
@Tag(name = "News", description = "新闻相关接口")
@RequiredArgsConstructor
@Slf4j
public class NewsController {
    private final NewsService newsService;

    @GetMapping("/{newsId}")
    @Operation(summary = "获取新闻", description = "获取新闻")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "newsId", description = "新闻ID", in = ParameterIn.PATH, required = true)
    })
    public Result<NewsVO> getNews(@PathVariable("newsId") Long newsId) {
        log.info("获取新闻, newsId: {}", newsId);

        // 获取新闻
        NewsVO newsVO = newsService.getNews(newsId);

        return Result.success(newsVO);
    }

    @GetMapping("")
    @Operation(summary = "获取新闻列表", description = "获取新闻列表")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true),
            @Parameter(name = "newsCategory", description = "新闻分类", in = ParameterIn.QUERY),
            @Parameter(name = "newsType", description = "新闻类型", in = ParameterIn.QUERY),
            @Parameter(name = "sourceName", description = "来源名称", in = ParameterIn.QUERY),
            @Parameter(name = "cursor", description = "游标", in = ParameterIn.QUERY),
            @Parameter(name = "lastNewsId", description = "最后一条新闻ID", in = ParameterIn.QUERY),
            @Parameter(name = "size", description = "数量", in = ParameterIn.QUERY),
            @Parameter(name = "sort", description = "排序", in = ParameterIn.QUERY)
    })
    public Result<NewsListVO> getNewsList(
            @RequestParam(name = "newsCategory", required = false) String newsCategory,
            @RequestParam(name = "newsType", required = false) String newsType,
            @RequestParam(name = "sourceName", required = false) String sourceName,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "lastNewsId", required = false) Long lastNewsId,
            @RequestParam(name = "size", defaultValue = "10") Integer size,
            @RequestParam(name = "sort", defaultValue = "time_desc") String sort
    ) {
        log.info("获取新闻列表");

        // 获取新闻列表
        NewsListDTO newsListDTO = NewsListDTO.builder()
                .newsCategory(newsCategory)
                .newsType(newsType)
                .sourceName(sourceName)
                .cursor(cursor)
                .lastNewsId(lastNewsId)
                .size(size)
                .sort(sort)
                .build();
        NewsListVO newsListVO = newsService.getNewsList(newsListDTO);

        return Result.success(newsListVO);
    }

    @GetMapping("/type")
    @Operation(summary = "获取新闻类型", description = "获取新闻类型")
    @Parameters({
            @Parameter(name = "Authorization", description = "授权Token", in = ParameterIn.HEADER, required = true)
    })
    public Result<JsonNode> getNewsType() {
        log.info("获取新闻类型");

        // 获取新闻类型
        JsonNode newsType = newsService.getNewsType();

        return Result.success(newsType);
    }
}
