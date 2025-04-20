package com.atcumt.model.search.entity;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Description("搜索建议响应对象")
public class SuggestionChat {
    @Description("严格规范化的搜索建议列表")
    List<String> suggestions;
}
