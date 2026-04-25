package com.bandmate.band.dto;

import com.bandmate.band.entity.BandApplication;
import com.bandmate.band.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ApplicationResponse {
    private Long applicationId;
    private Long bandId;
    private Long userId;
    private String nickname;
    private Position position;
    private BandApplication.ApplicationStatus status;
    private String introduction;
    private LocalDateTime createdAt;
}