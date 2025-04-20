package com.atcumt.model.search.vo;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class UserEsVO extends SearchEsVO {
    private String userId;
    private String nickname;
    private String bio;
    private Integer level;
    private Integer followersCount;
}
