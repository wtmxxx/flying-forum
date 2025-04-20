package com.atcumt.model.common.vo;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SimplePageQueryVO<T> {
    private Long page;
    private Long size;
    private List<T> data;

    public static <T> SimplePageQueryVO.SimplePageQueryVOBuilder<T> staticBuilder() {
        return SimplePageQueryVO.builder();
    }
}
