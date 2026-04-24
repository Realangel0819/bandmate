package com.bandmate.song.dto;

import lombok.Data;

@Data
public class CreateSongRequest {
    private String title;
    private String artist;
    private String youtubeUrl;
}