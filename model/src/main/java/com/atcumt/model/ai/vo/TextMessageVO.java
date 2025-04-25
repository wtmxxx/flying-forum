package com.atcumt.model.ai.vo;

import com.atcumt.model.ai.enums.FluxType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TextMessageVO extends FluxVO {
    private String type = FluxType.TEXT_MESSAGE.getValue();
    private Integer messageId;
    private Integer parentId;
    private String model;
    private String role;
    private String textContent;
}
