package com.bandmate.song.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AddSongCandidateRequest {
    private Long songId;
    private LocalDateTime voteStartDate;
    private LocalDateTime voteEndDate;
}