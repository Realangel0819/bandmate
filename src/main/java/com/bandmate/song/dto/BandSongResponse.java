package com.bandmate.song.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BandSongResponse {
    private Long bandSongId;
    private Long bandId;
    private Long songId;
    private String title;
    private String artist;
    private String youtubeUrl;
    private LocalDateTime voteStartDate;
    private LocalDateTime voteEndDate;
    private Integer voteCount;
    private Boolean isSelected;
    private Boolean isVotingActive;
    private LocalDateTime createdAt;
}