package com.atcumt.model.common.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PageQueryVO<T> {
    private Long totalRecords;
    private Long totalPages;
    private Long page;
    private Long size;
    private List<T> data;

    public static <T> PageQueryVO.PageQueryVOBuilder<T> staticBuilder() {
        return PageQueryVO.builder();
    }
}
