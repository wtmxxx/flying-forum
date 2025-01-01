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
    private String id;
    private String name;
    private String description;
    private String email;
    private String website;
    private String department;
}
