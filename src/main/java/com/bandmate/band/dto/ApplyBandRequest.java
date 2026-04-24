package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import lombok.Data;

@Data
public class ApplyBandRequest {
    private Long recruitId;
    private Position position;
}