package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRecruitRequest {

    private Long bandId;

    @NotNull(message = "포지션을 선택해주세요.")
    private Position position;

    @NotNull(message = "모집 인원을 입력해주세요.")
    @Min(value = 1, message = "모집 인원은 1명 이상이어야 합니다.")
    private Integer requiredCount;
}
