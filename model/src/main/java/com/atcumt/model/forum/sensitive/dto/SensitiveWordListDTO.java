package com.atcumt.model.forum.sensitive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SensitiveWordListDTO {
    List<SensitiveWordDTO> words;
}
