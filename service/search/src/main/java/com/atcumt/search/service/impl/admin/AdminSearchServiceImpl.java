package com.atcumt.search.service.impl.admin;

import cn.dev33.satoken.stp.StpUtil;
import com.atcumt.common.enums.PermAction;
import com.atcumt.common.enums.PermModule;
import com.atcumt.common.utils.PermissionUtil;
import com.atcumt.model.search.dto.SearchSuggestionDTO;
import com.atcumt.model.search.dto.SuggestionEsDTO;
import com.atcumt.model.search.enums.SuggestionAction;
import com.atcumt.model.search.enums.SuggestionType;
import com.atcumt.search.service.admin.AdminSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminSearchServiceImpl implements AdminSearchService {
    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void newCustomSuggestions(SuggestionEsDTO suggestionEsDTO) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SEARCH, PermAction.CREATE));

        List<Message<SearchSuggestionDTO>> messages = new ArrayList<>();
        for (String oneDocSuggestion : suggestionEsDTO.getSuggestions()) {
            SearchSuggestionDTO searchSuggestionDTO = SearchSuggestionDTO
                    .builder()
                    .action(SuggestionAction.CUSTOM)
                    .suggestion(oneDocSuggestion)
                    .type(SuggestionType.CUSTOM.getValue())
                    .build();
            messages.add(new GenericMessage<>(searchSuggestionDTO));
        }

        rocketMQTemplate.syncSend("search:searchSuggestion", messages);
    }

    @Override
    public void deleteSuggestion(String suggestion, String type) {
        // 检查权限
        StpUtil.checkPermission(PermissionUtil.generate(PermModule.SEARCH, PermAction.DELETE));

        SearchSuggestionDTO searchSuggestionDTO = SearchSuggestionDTO
                .builder()
                .action(SuggestionAction.DELETE)
                .suggestion(suggestion)
                .type(type)
                .build();
        rocketMQTemplate.convertAndSend("search:searchSuggestion", searchSuggestionDTO);
    }
}
