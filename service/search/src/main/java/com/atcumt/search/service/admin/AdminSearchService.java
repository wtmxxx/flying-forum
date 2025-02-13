package com.atcumt.search.service.admin;

import com.atcumt.model.search.dto.SuggestionEsDTO;

import java.io.IOException;
import java.util.List;

public interface AdminSearchService {
    void newSuggest(SuggestionEsDTO suggestionEsDTO) throws IOException;
}
