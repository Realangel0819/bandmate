package com.bandmate.rehearsal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRehearsalRequest {

    @NotBlank(message = "합주 제목을 입력해주세요.")
    private String title;

    private String description;

    @NotNull(message = "합주 일시를 입력해주세요.")
    private LocalDateTime rehearsalDate;

    private String location;

    @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
    private int maxCapacity;
}
