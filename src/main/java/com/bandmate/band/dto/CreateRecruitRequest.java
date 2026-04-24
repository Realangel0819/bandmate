package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import lombok.Data;

@Data
public class CreateRecruitRequest {
    private Long bandId;
    private Position position;
    private Integer requiredCount;
}