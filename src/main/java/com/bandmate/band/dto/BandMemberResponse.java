package com.bandmate.band.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BandMemberResponse {
    private Long memberId;
    private Long userId;
    private String nickname;
    private String position;
    private LocalDateTime joinedAt;
}
