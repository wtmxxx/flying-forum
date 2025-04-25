package com.atcumt.model.ai.vo;

import com.atcumt.model.ai.enums.FluxType;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class TitleVO extends FluxVO {
    private String type = FluxType.TITLE.getValue();
    private String title;
}
