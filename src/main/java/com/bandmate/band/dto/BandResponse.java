package com.bandmate.band.dto;

import com.bandmate.band.entity.Position;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BandResponse {
    private Long bandId;
    private String name;
    private String description;
    private Long leaderId;
    private Integer memberCount;
    private LocalDateTime createdAt;
    private Integer maxVotesPerPerson;
}