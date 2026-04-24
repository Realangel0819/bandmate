package com.bandmate.song.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddSongCandidateRequest {

    @NotNull(message = "곡을 선택해주세요.")
    private Long songId;

    @NotNull(message = "투표 시작일을 입력해주세요.")
    private LocalDateTime voteStartDate;

    @NotNull(message = "투표 종료일을 입력해주세요.")
    private LocalDateTime voteEndDate;
}
