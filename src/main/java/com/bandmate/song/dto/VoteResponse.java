package com.bandmate.song.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class VoteResponse {
    private Long id;
    private Long bandSongId;
    private Long userId;
    private String message;
}