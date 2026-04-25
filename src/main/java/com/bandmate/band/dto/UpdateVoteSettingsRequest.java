package com.bandmate.band.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateVoteSettingsRequest {

    @NotNull(message = "인당 투표 수를 입력해주세요.")
    @Min(value = 1, message = "최소 1표 이상이어야 합니다.")
    private Integer maxVotesPerPerson;
}
