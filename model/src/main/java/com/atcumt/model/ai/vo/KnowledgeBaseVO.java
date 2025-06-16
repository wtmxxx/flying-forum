package com.atcumt.model.ai.vo;

import com.atcumt.model.ai.entity.KnowledgeBase;
import com.atcumt.model.ai.enums.FluxType;
import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KnowledgeBaseVO extends FluxVO {
    @Builder.Default
    private String type = FluxType.KNOWLEDGE_BASE_MESSAGE.getValue();
    private List<KnowledgeBase> documents;
}
