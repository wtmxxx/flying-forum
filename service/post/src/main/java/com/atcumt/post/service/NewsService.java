package com.atcumt.post.service;

import com.atcumt.model.post.dto.NewsListDTO;
import com.atcumt.model.post.vo.NewsListVO;
import com.atcumt.model.post.vo.NewsVO;
import com.fasterxml.jackson.databind.JsonNode;

public interface NewsService {
    NewsVO getNews(Long newsId);

    NewsListVO getNewsList(NewsListDTO newsListDTO);

    JsonNode getNewsType();
}
