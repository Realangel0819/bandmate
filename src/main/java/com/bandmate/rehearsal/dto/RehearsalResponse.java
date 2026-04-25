package com.bandmate.rehearsal.dto;

import com.bandmate.rehearsal.entity.Rehearsal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RehearsalResponse {
    private Long rehearsalId;
    private Long bandId;
    private String title;
    private String description;
    private LocalDateTime rehearsalDate;
    private String location;
    private int maxCapacity;
    private int currentAttendees;
    private LocalDateTime createdAt;

    public static RehearsalResponse from(Rehearsal rehearsal) {
        return new RehearsalResponse(
                rehearsal.getId(),
                rehearsal.getBandId(),
                rehearsal.getTitle(),
                rehearsal.getDescription(),
                rehearsal.getRehearsalDate(),
                rehearsal.getLocation(),
                rehearsal.getMaxCapacity(),
                rehearsal.getCurrentCount(),
                rehearsal.getCreatedAt()
        );
    }
}
