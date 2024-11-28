package com.atcumt.model.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UntieDisableServiceBatchDTO {
    @Schema(description = "解封列表")
    private List<UntieDisableServiceDTO> services;
}
