package com.bandmate.rehearsal.dto;

import com.bandmate.rehearsal.entity.RehearsalAttendance;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long id;
    private Long rehearsalId;
    private Long userId;
    private LocalDateTime createdAt;

    public static AttendanceResponse from(RehearsalAttendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getRehearsalId(),
                attendance.getUserId(),
                attendance.getCreatedAt()
        );
    }
}
