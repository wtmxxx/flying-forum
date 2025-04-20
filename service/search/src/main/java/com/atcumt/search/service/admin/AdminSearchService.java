package com.atcumt.search.service.admin;

import com.atcumt.model.search.dto.SuggestionEsDTO;

import java.io.IOException;

public interface AdminSearchService {
    void newCustomSuggestions(SuggestionEsDTO suggestionEsDTO) throws IOException;

    void deleteSuggestion(String suggestion, String type);
}
