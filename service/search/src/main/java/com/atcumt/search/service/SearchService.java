package com.atcumt.search.service;

import com.atcumt.model.search.vo.SuggestionEsListVO;

import java.io.IOException;
import java.util.List;

public interface SearchService {
    SuggestionEsListVO suggest(String text) throws IOException;
}
