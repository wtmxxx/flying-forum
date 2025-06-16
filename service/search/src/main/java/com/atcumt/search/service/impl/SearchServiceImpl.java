package com.atcumt.search.service.impl;

import cn.hutool.core.bean.BeanUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.atcumt.common.utils.UserInfoUtil;
import com.atcumt.model.post.entity.Tag;
import com.atcumt.model.search.dto.PostSearchDTO;
import com.atcumt.model.search.dto.SearchSuggestionDTO;
import com.atcumt.model.search.dto.TagSearchDTO;
import com.atcumt.model.search.dto.UserSearchDTO;
import com.atcumt.model.search.entity.DiscussionEs;
import com.atcumt.model.search.entity.SuggestionEs;
import com.atcumt.model.search.entity.UserEs;
import com.atcumt.model.search.enums.*;
import com.atcumt.model.search.vo.*;
import com.atcumt.model.user.vo.UserInfoSimpleVO;
import com.atcumt.search.service.SearchService;
import com.atcumt.search.utils.EsConvertUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {
    private final ElasticsearchClient elasticsearchClient;
    private final RocketMQTemplate rocketMQTemplate;
    private final UserInfoUtil userInfoUtil;
    private final ObjectMapper objectMapper;
    private final EsConvertUtil esConvertUtil;
    private final MongoTemplate mongoTemplate;

    @Override
    public SuggestionEsListVO suggest(String text) throws IOException {
        if (text.matches("^[a-z]+('[a-z]+)+$")) {
            text = text.replace("'", "");
        }
        String formatText = text.toLowerCase();

        SearchRequest searchRequest = SearchRequest.of(sr ->
            sr.query(q -> q.bool(b -> b
                     .must(m -> m.match(mt -> mt
                             .field("suggestion")
                             .query(formatText)
                             .fuzziness("AUTO")))
                     .filter(f -> f.range(r -> r
                             .number(n -> n
                                     .field("score")
                                     .gte(10.0)
                             )))))
                    .size(10).index("suggestion")
                    .source(sc -> sc.filter(sf -> sf
                            .includes("suggestion", "type")))
                    .sort(s -> s.field(fs -> fs.field("score").order(SortOrder.Desc)))
        );
        SearchResponse<SuggestionEs> searchResponse = elasticsearchClient.search(searchRequest, SuggestionEs.class);
        List<SuggestionEsVO> suggestionEsVOList = new ArrayList<>();
        for (var hit : searchResponse.hits().hits()) {
            SuggestionEs suggestionEs = hit.source();
            if (suggestionEs == null) {
                suggestionEs = SuggestionEs.builder().build();
            }

            SuggestionEsVO suggestionEsVO = SuggestionEsVO
                    .builder()
                    .suggestionId(hit.id())
                    .suggestion(suggestionEs.getSuggestion())
                    .type(suggestionEs.getType())
                    .build();
            suggestionEsVOList.add(suggestionEsVO);
        }
        return SuggestionEsListVO.builder().suggestions(suggestionEsVOList).build();
    }

    public SearchVO searchPost(PostSearchDTO postSearchDTO) throws Exception {
        newSearchSuggestion(postSearchDTO.getText());

        // 构建 Bool 查询
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder
                .minimumShouldMatch("1")
                .should(s -> s.multiMatch(mm -> mm
                        .fields("title", "content")
                        .query(postSearchDTO.getText())
                        .boost(1.5f)))
                .should(s -> s.nested(n -> n
                        .path("tags")
                        .query(nq -> nq.match(m -> m
                                .field("tags.tagName")
                                .query(postSearchDTO.getText())))));

        if (!SearchTimeLimit.ALL.getValue().equalsIgnoreCase(postSearchDTO.getSearchTimeLimit())) {
            boolQueryBuilder.filter(f -> f.range(r -> r
                    .date(d -> d
                            .field("createTime")
                            .gte(getTimeRange(postSearchDTO.getSearchTimeLimit()))
                    )));
        }

        // 构建搜索请求
        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index(postSearchDTO.getSearchContentType())
                .from(postSearchDTO.getFrom())
                .size(postSearchDTO.getSize())
                .terminateAfter(5000L + postSearchDTO.getFrom() + postSearchDTO.getSize())
                .highlight(h -> h
                        .fields("title", field -> field
                                .preTags("<em>")
                                .postTags("</em>")
                        )
                        .fields("content", field -> field
                                .preTags("<em>")
                                .postTags("</em>")
                        )
                );

        // 设置排序
        if (PostSearchSortType.DEFAULT.getValue().equalsIgnoreCase(postSearchDTO.getSearchSortType())) {
            // 构建 Function Score 查询
            Query functionScoreQuery = Query.of(q -> q.functionScore(fs -> fs
                    .query(qb -> qb.bool(boolQueryBuilder.build()))
                    .functions(fns -> fns
                            .fieldValueFactor(fvf -> fvf
                                    .field("score")
                                    .factor(1.0)
                                    .modifier(FieldValueFactorModifier.Sqrt)
                                    .missing(1.0)
                            )
                            .weight(0.2)
                    )
                    .functions(qb -> qb
                            .fieldValueFactor(fvf -> fvf
                                    .field("createTime")
                                    .factor(1.0)
                                    .modifier(FieldValueFactorModifier.Log1p)
                                    .missing(1.0)
                            )
                            .weight(0.2)
                    )
                    .scoreMode(FunctionScoreMode.Sum)
                    .boostMode(FunctionBoostMode.Multiply)
            ));

            searchRequest.query(functionScoreQuery);
        } else {
            searchRequest.query(q -> q.bool(boolQueryBuilder.build()));
            searchRequest.sort(s -> s.field(f -> f
                    .field(getPostSortField(postSearchDTO.getSearchSortType()))
                    .order(getPostSortOrder(postSearchDTO.getSearchSortType()))
            ));
        }

        // 发起搜索
        SearchResponse<JsonNode> response = elasticsearchClient.search(searchRequest.build(), JsonNode.class);

        List<Hit<JsonNode>> searchHits = response.hits().hits();

        Set<String> userIdSet = new HashSet<>();

        for (Hit<JsonNode> searchHit : searchHits) {
            if (searchHit.source() != null) {
                String userId = searchHit.source().get("userId").asText(null);
                if (userId != null) {
                    userIdSet.add(userId);
                }
            }
        }
        List<String> userIdList = new ArrayList<>(userIdSet);

        List<UserInfoSimpleVO> userInfoSimpleVOs = userInfoUtil.getUserInfoSimpleBatch(userIdList);
        Map<String, UserInfoSimpleVO> userInfoSimpleVOMap;
        if (userInfoSimpleVOs != null && !userInfoSimpleVOs.isEmpty()) {
            userInfoSimpleVOMap = userInfoSimpleVOs.stream()
                    .collect(Collectors.toMap(
                            UserInfoSimpleVO::getUserId,
                            userInfoSimpleVO -> userInfoSimpleVO,
                            (_, replacement) -> replacement
                    ));
        } else {
            userInfoSimpleVOMap = new HashMap<>();
        }

        List<SearchEsVO> hits = searchHits.parallelStream()
                .filter(hit -> hit.source() != null)
                .map(hit -> {
                    try {
                        if (SearchContentType.DISCUSSION.getValue().equals(hit.index())) {
                            return esConvertUtil.toDiscussionEsVO(
                                    hit.id(),
                                    objectMapper.treeToValue(hit.source(), DiscussionEs.class),
                                    hit.highlight(),
                                    userInfoSimpleVOMap);
                        } else {
                            log.error("不支持的索引类型: {}", hit.index());
                        }
                    } catch (Exception e) {
                        log.error("转换搜索结果失败, hit: {}", hit, e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        SearchVO searchVO = SearchVO
                .builder()
                .from(postSearchDTO.getFrom())
                .size(hits.size())
                .hits(hits)
                .build();

        return searchVO;
    }

    public SearchVO searchUser(UserSearchDTO userSearchDTO) throws Exception {
        // 构建 Bool 查询
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder
                .minimumShouldMatch("1")
                .should(s -> s.term(mm -> mm
                        .field("username")
                        .value(userSearchDTO.getText())
                        .boost(50.0f)))
                .should(s -> s.match(mm -> mm
                        .field("nickname")
                        .query(userSearchDTO.getText())
                        .fuzziness("AUTO")
                        .boost(2.0f)))
                .should(s -> s.multiMatch(mm -> mm
                        .fields("bio", "hometown", "major")
                        .query(userSearchDTO.getText())))
                .should(s -> s.match(mm -> mm
                        .field("statuses")
                        .query(userSearchDTO.getText())
                        .boost(0.5f)));

        // 构建搜索请求
        SearchRequest.Builder searchRequest = new SearchRequest.Builder()
                .index("user")
                .from(userSearchDTO.getFrom())
                .size(userSearchDTO.getSize())
                .terminateAfter(5000L + userSearchDTO.getFrom() + userSearchDTO.getSize())
                .source(sc -> sc.filter(sf -> sf.includes(List.of(
                        "userId", "nickname", "bio", "level", "followersCount"
                ))))
                .query(q -> q.bool(boolQueryBuilder.build()));

        // 设置排序
        if (!UserSearchSortType.DEFAULT.getValue().equalsIgnoreCase(userSearchDTO.getSearchSortType())) {
            searchRequest.sort(s -> s.field(f -> f
                    .field(getUserSortField(userSearchDTO.getSearchSortType()))
                    .order(getUserSortOrder(userSearchDTO.getSearchSortType()))
            ));
        }

        // 发起搜索
        SearchResponse<UserEs> response = elasticsearchClient.search(searchRequest.build(), UserEs.class);

        List<SearchEsVO> hits = new ArrayList<>();

        for (Hit<UserEs> searchHit : response.hits().hits()) {
            if (searchHit.source() != null) {
                UserEsVO userEsVO = BeanUtil.toBean(searchHit.source(), UserEsVO.class);
                userEsVO.setUserId(searchHit.id());
                hits.add(userEsVO);
            }
        }

        SearchVO searchVO = SearchVO
                .builder()
                .from(userSearchDTO.getFrom())
                .size(hits.size())
                .hits(hits)
                .build();

        return searchVO;
    }

    public SearchVO searchTag(TagSearchDTO tagSearchDTO) throws Exception {
        SearchRequest searchRequest = SearchRequest.of(sr -> sr
                .index("suggestion")
                .from(tagSearchDTO.getFrom())
                .size(tagSearchDTO.getSize())
                .terminateAfter(5000L + tagSearchDTO.getFrom() + tagSearchDTO.getSize())
                .query(q -> q.bool(b -> b
                        .must(m -> m.match(mt -> mt
                                .field("suggestion")
                                .query(tagSearchDTO.getText())))
                        .filter(f -> f.match(fm -> fm
                                .field("type")
                                .query(SuggestionType.TAG.getValue())))))
                        .source(sc -> sc.filter(sf -> sf
                                .includes("suggestion")))
        );
        SearchResponse<SuggestionEs> searchResponse = elasticsearchClient.search(searchRequest, SuggestionEs.class);
        List<String> tagNames = new ArrayList<>();
        for (var hit : searchResponse.hits().hits()) {
            if (hit.source() != null) {
                tagNames.add(hit.source().getSuggestion());
            }
        }

        List<SearchEsVO> tagVOs = new ArrayList<>();
        if (!tagNames.isEmpty()) {
            Map<String, TagEsVO> tags = mongoTemplate
                    .find(new org.springframework.data.mongodb.core.query.Query(Criteria.where("tagName").in(tagNames)), Tag.class)
                    .parallelStream()
                    .collect(Collectors.toMap(Tag::getTagName, tag -> BeanUtil.toBean(tag, TagEsVO.class)));

            tagNames.forEach(tagName -> tagVOs.add(tags.get(tagName)));
        }
        tagVOs.remove(null);

        SearchVO searchVO = SearchVO
                .builder()
                .from(tagSearchDTO.getFrom())
                .size(tagVOs.size())
                .hits(tagVOs)
                .build();

        return searchVO;
    }

    // 获取时间范围的方法
    private String getTimeRange(String timeLimit) {
        return switch (SearchTimeLimit.fromValue(timeLimit)) {
            case ONE_DAY -> "now-1d/d";
            case THREE_DAYS -> "now-3d/d";
            case ONE_WEEK -> "now-1w/w";
            case ONE_MONTH -> "now-1M/M";
            case THREE_MONTHS -> "now-3M/M";
            case HALF_YEAR -> "now-6M/M";
            case ONE_YEAR -> "now-1y/y";
            default -> "1970-01-01T00:00:00Z";  // 默认是全部
        };
    }

    // 获取排序字段的方法
    private String getPostSortField(String sortType) {
        return switch (PostSearchSortType.fromValue(sortType)) {
            case MOST_LIKED -> "likeCount";  // 按照点赞数排序
            case MOST_COMMENTED -> "commentCount";  // 按照评论数排序
            case LATEST_POST, EARLIEST_POST -> "createTime";  // 按照创建时间排序
            default -> "score";  // 默认按照创建时间排序
        };
    }

    // 获取排序方式的方法
    private SortOrder getPostSortOrder(String sortType) {
        return switch (PostSearchSortType.fromValue(sortType)) {
            case MOST_LIKED -> SortOrder.Desc;  // 根据点赞数降序
            case MOST_COMMENTED -> SortOrder.Desc;  // 根据评论数降序
            case LATEST_POST -> SortOrder.Desc;  // 根据创建时间降序
            case EARLIEST_POST -> SortOrder.Asc;  // 根据创建时间升序
            default -> SortOrder.Desc;  // 默认排序
        };
    }

    // 获取排序字段的方法
    private String getUserSortField(String sortType) {
        return switch (UserSearchSortType.fromValue(sortType)) {
            case MOST_FOLLOWER, LEAST_FOLLOWER -> "followersCount"; // 按照粉丝数排序
            case HIGHEST_LEVEL, LOWEST_LEVEL -> "experience"; // 按照经验值排序
            default -> "username"; // 默认按照用户名排序
        };
    }

    // 获取排序方式的方法
    private SortOrder getUserSortOrder(String sortType) {
        return switch (UserSearchSortType.fromValue(sortType)) {
            case MOST_FOLLOWER -> SortOrder.Desc; // 根据粉丝数降序
            case LEAST_FOLLOWER -> SortOrder.Asc; // 根据粉丝数升序
            case HIGHEST_LEVEL -> SortOrder.Desc; // 根据经验值降序
            case LOWEST_LEVEL -> SortOrder.Asc; // 根据经验值升序
            default -> SortOrder.Desc; // 默认排序
        };
    }

    private void newSearchSuggestion(String suggestion) {
        SearchSuggestionDTO searchSuggestionDTO = SearchSuggestionDTO
                .builder()
                .action(SuggestionAction.SEARCH)
                .suggestion(suggestion)
                .type(SuggestionType.SEARCH.getValue())
                .build();

        rocketMQTemplate.asyncSend("search:searchSuggestion", searchSuggestionDTO, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {}

            @Override
            public void onException(Throwable e) {
                log.error("搜索提示消息发送失败, suggestion: {}", searchSuggestionDTO, e);
            }
        });
    }
}