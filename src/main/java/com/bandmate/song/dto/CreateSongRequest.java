package com.bandmate.song.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSongRequest {

    @NotBlank(message = "곡 제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "아티스트를 입력해주세요.")
    private String artist;

    private String youtubeUrl;
}
