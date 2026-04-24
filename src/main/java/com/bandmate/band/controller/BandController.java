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

    @PostMapping
    public ResponseEntity<BandResponse> createBand(
            @RequestBody @Valid CreateBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7)); // "Bearer " 제거
        BandResponse response = bandService.createBand(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bandId}")
    public ResponseEntity<BandResponse> getBand(@PathVariable Long bandId) {
        BandResponse response = bandService.getBand(bandId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-bands")
    public ResponseEntity<List<BandResponse>> getMyBands(
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<BandResponse> responses = bandService.getLeaderBands(userId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{bandId}/recruits")
    public ResponseEntity<RecruitResponse> createRecruit(
            @PathVariable Long bandId,
            @RequestBody @Valid CreateRecruitRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        request.setBandId(bandId);
        RecruitResponse response = bandService.createRecruit(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{bandId}/apply")
    public ResponseEntity<ApplicationResponse> applyBand(
            @PathVariable Long bandId,
            @RequestBody @Valid ApplyBandRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        ApplicationResponse response = bandService.applyBand(bandId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{bandId}/applications/{applicationId}/approve")
    public ResponseEntity<ApplicationResponse> approveApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        ApplicationResponse response = bandService.approveApplication(bandId, applicationId, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{bandId}/applications/{applicationId}/reject")
    public ResponseEntity<ApplicationResponse> rejectApplication(
            @PathVariable Long bandId,
            @PathVariable Long applicationId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        ApplicationResponse response = bandService.rejectApplication(bandId, applicationId, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{bandId}")
    public ResponseEntity<Void> deleteBand(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        bandService.deleteBand(bandId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{bandId}/applications")
    public ResponseEntity<List<ApplicationResponse>> getBandApplications(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        List<ApplicationResponse> responses = bandService.getBandApplications(bandId, userId);
        return ResponseEntity.ok(responses);
    }
}