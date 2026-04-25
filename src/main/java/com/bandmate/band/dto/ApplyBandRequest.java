package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplyBandRequest {

    @NotNull(message = "모집 공고를 선택해주세요.")
    private Long recruitId;

    @NotNull(message = "포지션을 선택해주세요.")
    private Position position;

    private String introduction;
}
