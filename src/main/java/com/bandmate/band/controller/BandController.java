package com.bandmate.band.controller;

import com.bandmate.band.dto.*;
import com.bandmate.band.service.BandService;
import com.bandmate.common.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "밴드", description = "밴드 생성·조회·모집·지원·멤버 관리")
@RestController
@RequestMapping("/api/bands")
@RequiredArgsConstructor
public class BandController {

    private final BandService bandService;
    private final JwtUtil jwtUtil;

    @Operation(summary = "전체 밴드 목록 조회", description = "로그인 불필요. 최신순 정렬.")
    @GetMapping
    public ResponseEntity<List<BandResponse>> getAllBands() {
        return ResponseEntity.ok(bandService.getAllBands());
    }

    @Operation(summary = "밴드 생성", description = "생성자가 자동으로 리더(VOCAL)로 등록됩니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "403", description = "인증 필요")
        })
    @PostMapping
    public ResponseEntity<BandResponse> createBand(
            @RequestBody @Valid CreateBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.createBand(request, userId));
    }

    @Operation(summary = "밴드 단건 조회")
    @GetMapping("/{bandId}")
    public ResponseEntity<BandResponse> getBand(@PathVariable Long bandId) {
        return ResponseEntity.ok(bandService.getBand(bandId));
    }

    @Operation(summary = "내가 속한 밴드 목록", description = "리더/멤버 모두 포함.",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/my-bands")
    public ResponseEntity<List<BandResponse>> getMyBands(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.getMyBands(userId));
    }

    @Operation(summary = "현재 멤버 목록 조회", description = "닉네임·포지션 포함.")
    @GetMapping("/{bandId}/members")
    public ResponseEntity<List<BandMemberResponse>> getBandMembers(@PathVariable Long bandId) {
        return ResponseEntity.ok(bandService.getBandMembers(bandId));
    }

    @Operation(summary = "멤버 강퇴 / 탈퇴",
        description = "리더: 모든 멤버 강퇴 가능. 일반 멤버: 본인만 탈퇴. 리더 본인은 탈퇴 불가.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "204", description = "처리 성공"),
            @ApiResponse(responseCode = "400", description = "리더는 탈퇴 불가"),
            @ApiResponse(responseCode = "403", description = "권한 없음")
        })
    @DeleteMapping("/{bandId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long bandId,
            @PathVariable @Parameter(description = "BandMember PK (멤버 목록 조회의 memberId)") Long memberId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        bandService.removeMember(bandId, memberId, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "인당 투표 수 변경 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{bandId}/vote-settings")
    public ResponseEntity<BandResponse> updateVoteSettings(
            @PathVariable Long bandId,
            @RequestBody @Valid UpdateVoteSettingsRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.updateVoteSettings(bandId, request, userId));
    }

    @Operation(summary = "모집 공고 등록 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/{bandId}/recruits")
    public ResponseEntity<RecruitResponse> createRecruit(
            @PathVariable Long bandId,
            @RequestBody @Valid CreateRecruitRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        request.setBandId(bandId);
        return ResponseEntity.ok(bandService.createRecruit(request, userId));
    }

    @Operation(summary = "모집 공고 목록 조회")
    @GetMapping("/{bandId}/recruits")
    public ResponseEntity<List<RecruitResponse>> getBandRecruits(@PathVariable Long bandId) {
        return ResponseEntity.ok(bandService.getBandRecruits(bandId));
    }

    @Operation(summary = "밴드 지원",
        description = "자기소개(introduction) 선택 입력. 정원 초과·중복 지원·기존 멤버는 차단됩니다.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "지원 성공"),
            @ApiResponse(responseCode = "409", description = "중복 지원 / 이미 멤버 / 정원 초과")
        })
    @PostMapping("/{bandId}/apply")
    public ResponseEntity<ApplicationResponse> applyBand(
            @PathVariable Long bandId,
            @RequestBody @Valid ApplyBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.applyBand(bandId, request, userId));
    }

    @Operation(summary = "지원서 목록 조회 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{bandId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getBandApplications(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.getBandApplications(bandId, userId));
    }

    @Operation(summary = "지원 승인 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{bandId}/applications/{applicationId}/approve")
    public ResponseEntity<ApplicationResponse> approveApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.approveApplication(bandId, applicationId, userId));
    }

    @Operation(summary = "지원 거절 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{bandId}/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.rejectApplication(bandId, applicationId, userId));
    }

    @Operation(summary = "밴드 삭제 — Soft Delete (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{bandId}")
    public ResponseEntity<Void> deleteBand(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        bandService.deleteBand(bandId, userId);
        return ResponseEntity.noContent().build();
    }
}
