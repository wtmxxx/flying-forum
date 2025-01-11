package com.atcumt.model.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Wotemo {
    @Builder.Default
    private String id = "wtmxxx";
    @Builder.Default
    private String name = "Wotemo";
    @Builder.Default
    private String description = "Wotemo is a software engineer.";
    @Builder.Default
    private String email = "iswotemo@gmail.com";
    @Builder.Default
    private String website = "https://www.wotemo.com";
    @Builder.Default
    private String department = "CUMT Flying Studio";
}
