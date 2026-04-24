package com.bandmate.band.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBandRequest {

    @NotBlank(message = "밴드 이름을 입력해주세요.")
    @Size(max = 100, message = "밴드 이름은 100자 이하여야 합니다.")
    private String name;

    private String description;
}
