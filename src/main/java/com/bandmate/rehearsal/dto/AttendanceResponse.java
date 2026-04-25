package com.bandmate.rehearsal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceResponse {
    private Long attendanceId;
    private Long rehearsalId;
    private Long userId;
    private String nickname;
    private LocalDateTime createdAt;
}
