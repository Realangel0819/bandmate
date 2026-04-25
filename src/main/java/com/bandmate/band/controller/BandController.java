package com.bandmate.band.controller;

import com.bandmate.band.dto.*;
import com.bandmate.band.service.BandService;
import com.bandmate.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bands")
@RequiredArgsConstructor
public class BandController {

    private final BandService bandService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<List<BandResponse>> getAllBands() {
        return ResponseEntity.ok(bandService.getAllBands());
    }

    @PostMapping
    public ResponseEntity<BandResponse> createBand(
            @RequestBody @Valid CreateBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.createBand(request, userId));
    }

    @GetMapping("/{bandId}")
    public ResponseEntity<BandResponse> getBand(@PathVariable Long bandId) {
        return ResponseEntity.ok(bandService.getBand(bandId));
    }

    @GetMapping("/my-bands")
    public ResponseEntity<List<BandResponse>> getMyBands(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.getLeaderBands(userId));
    }

    @PutMapping("/{bandId}/vote-settings")
    public ResponseEntity<BandResponse> updateVoteSettings(
            @PathVariable Long bandId,
            @RequestBody @Valid UpdateVoteSettingsRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.updateVoteSettings(bandId, request, userId));
    }

    @PostMapping("/{bandId}/recruits")
    public ResponseEntity<RecruitResponse> createRecruit(
            @PathVariable Long bandId,
            @RequestBody @Valid CreateRecruitRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        request.setBandId(bandId);
        return ResponseEntity.ok(bandService.createRecruit(request, userId));
    }

    @GetMapping("/{bandId}/recruits")
    public ResponseEntity<List<RecruitResponse>> getBandRecruits(@PathVariable Long bandId) {
        return ResponseEntity.ok(bandService.getBandRecruits(bandId));
    }

    @PostMapping("/{bandId}/apply")
    public ResponseEntity<ApplicationResponse> applyBand(
            @PathVariable Long bandId,
            @RequestBody @Valid ApplyBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.applyBand(bandId, request, userId));
    }

    @GetMapping("/{bandId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getBandApplications(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.getBandApplications(bandId, userId));
    }

    @PutMapping("/{bandId}/applications/{applicationId}/approve")
    public ResponseEntity<ApplicationResponse> approveApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.approveApplication(bandId, applicationId, userId));
    }

    @PutMapping("/{bandId}/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(bandService.rejectApplication(bandId, applicationId, userId));
    }

    @DeleteMapping("/{bandId}")
    public ResponseEntity<Void> deleteBand(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        bandService.deleteBand(bandId, userId);
        return ResponseEntity.noContent().build();
    }
}
