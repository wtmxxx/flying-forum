package com.atcumt.model.forum.sensitive.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordFindVO {
    Boolean contains;
    List<String> words;
}
