package com.bandmate.rehearsal.controller;

import com.bandmate.common.util.JwtUtil;
import com.bandmate.rehearsal.dto.AttendanceResponse;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.dto.RehearsalResponse;
import com.bandmate.rehearsal.service.RehearsalService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bands/{bandId}/rehearsals")
@RequiredArgsConstructor
public class RehearsalController {

    private final RehearsalService rehearsalService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<RehearsalResponse> createRehearsal(
            @PathVariable Long bandId,
            @RequestBody @Valid CreateRehearsalRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.createRehearsal(bandId, request, userId));
    }

    @GetMapping
    public ResponseEntity<List<RehearsalResponse>> getRehearsals(@PathVariable Long bandId) {
        return ResponseEntity.ok(rehearsalService.getRehearsals(bandId));
    }

    @GetMapping("/{rehearsalId}")
    public ResponseEntity<RehearsalResponse> getRehearsal(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId) {
        return ResponseEntity.ok(rehearsalService.getRehearsal(bandId, rehearsalId));
    }

    @PostMapping("/{rehearsalId}/join")
    public ResponseEntity<AttendanceResponse> joinRehearsal(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.joinRehearsal(bandId, rehearsalId, userId));
    }

    @DeleteMapping("/{rehearsalId}/join")
    public ResponseEntity<Void> cancelAttendance(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        rehearsalService.cancelAttendance(bandId, rehearsalId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{rehearsalId}/attendances")
    public ResponseEntity<List<AttendanceResponse>> getAttendances(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.getAttendances(bandId, rehearsalId, userId));
    }
}
