package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RecruitResponse {
    private Long id;
    private Long bandId;
    private Position position;
    private Integer requiredCount;
    private Integer currentCount;
    private LocalDateTime createdAt;
}