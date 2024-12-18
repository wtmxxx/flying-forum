package com.atcumt.model.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "分页查询DTO")
public class TypePageQueryDTO {
    @Schema(description = "类型")
    private String type;
    @Schema(description = "页码", example = "1")
    private Long page;
    @Schema(description = "每页的记录数", example = "10")
    private Long size;

    public void checkParam() {
        if (this.page == null || this.page < 0) {
            setPage(1L);
        }
        if (this.size == null || this.size < 0 || this.size > 1000) {
            setSize(10L);
        }
    }
}

