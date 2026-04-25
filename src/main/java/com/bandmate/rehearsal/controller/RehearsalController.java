package com.bandmate.rehearsal.controller;

import com.bandmate.common.util.JwtUtil;
import com.bandmate.rehearsal.dto.AttendanceResponse;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.dto.RehearsalResponse;
import com.bandmate.rehearsal.service.RehearsalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "합주 일정", description = "합주 생성·조회·신청·취소·참여자 목록")
@RestController
@RequestMapping("/api/bands/{bandId}/rehearsals")
@RequiredArgsConstructor
public class RehearsalController {

    private final RehearsalService rehearsalService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "합주 일정 생성 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<RehearsalResponse> createRehearsal(
            @PathVariable Long bandId,
            @RequestBody @Valid CreateRehearsalRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.createRehearsal(bandId, request, userId));
    }

    @Operation(summary = "합주 일정 목록 조회")
    @GetMapping
    public ResponseEntity<List<RehearsalResponse>> getRehearsals(@PathVariable Long bandId) {
        return ResponseEntity.ok(rehearsalService.getRehearsals(bandId));
    }

    @Operation(summary = "합주 일정 단건 조회")
    @GetMapping("/{rehearsalId}")
    public ResponseEntity<RehearsalResponse> getRehearsal(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId) {
        return ResponseEntity.ok(rehearsalService.getRehearsal(bandId, rehearsalId));
    }

    @Operation(summary = "합주 신청",
        description = "비관적 락(SELECT FOR UPDATE)으로 동시 정원 초과를 방지합니다. 중복 신청 불가.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "신청 성공"),
            @ApiResponse(responseCode = "409", description = "정원 초과 또는 중복 신청")
        })
    @PostMapping("/{rehearsalId}/join")
    public ResponseEntity<AttendanceResponse> joinRehearsal(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.joinRehearsal(bandId, rehearsalId, userId));
    }

    @Operation(summary = "합주 신청 취소",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{rehearsalId}/join")
    public ResponseEntity<Void> cancelAttendance(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        rehearsalService.cancelAttendance(bandId, rehearsalId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "참여자 목록 조회 (멤버 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{rehearsalId}/attendances")
    public ResponseEntity<List<AttendanceResponse>> getAttendances(
            @PathVariable Long bandId,
            @PathVariable Long rehearsalId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(rehearsalService.getAttendances(bandId, rehearsalId, userId));
    }
}
