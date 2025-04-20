package com.atcumt.search.service;

import com.atcumt.model.search.dto.PostSearchDTO;
import com.atcumt.model.search.dto.TagSearchDTO;
import com.atcumt.model.search.dto.UserSearchDTO;
import com.atcumt.model.search.vo.SearchVO;
import com.atcumt.model.search.vo.SuggestionEsListVO;

import java.io.IOException;

public interface SearchService {
    SuggestionEsListVO suggest(String text) throws IOException;

    SearchVO searchPost(PostSearchDTO postSearchDTO) throws Exception;

    SearchVO searchUser(UserSearchDTO userSearchDTO) throws Exception;

    SearchVO searchTag(TagSearchDTO tagSearchDTO) throws Exception;
}
