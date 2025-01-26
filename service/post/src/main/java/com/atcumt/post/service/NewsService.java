package com.atcumt.post.service;

import cn.hutool.json.JSONObject;
import com.atcumt.model.post.dto.NewsListDTO;
import com.atcumt.model.post.vo.NewsListVO;
import com.atcumt.model.post.vo.NewsVO;

public interface NewsService {
    NewsVO getNews(Long newsId);

    NewsListVO getNewsList(NewsListDTO newsListDTO);

    JSONObject getNewsType();
}
