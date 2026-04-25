package com.bandmate.song.controller;

import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.NotFoundException;
import com.bandmate.common.exception.UnauthorizedException;
import com.bandmate.common.util.JwtUtil;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.Song;
import com.bandmate.song.service.SongService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "곡 투표", description = "곡 등록·후보 추가·투표·선정")
@RestController
@RequestMapping("/api/bands/{bandId}/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final JwtUtil jwtUtil;
    private final BandRepository bandRepository;

    @Operation(summary = "곡 등록 (전역)")
    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody @Valid CreateSongRequest request) {
        return ResponseEntity.ok(songService.createSong(request));
    }

    @Operation(summary = "후보곡 추가 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/candidates")
    public ResponseEntity<BandSongResponse> addSongCandidate(
            @PathVariable Long bandId,
            @RequestBody @Valid AddSongCandidateRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        return ResponseEntity.ok(songService.addSongCandidate(bandId, request, userId));
    }

    @Operation(summary = "후보곡 전체 초기화 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/candidates")
    public ResponseEntity<Void> resetCandidates(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        songService.resetCandidates(bandId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "곡 투표",
        description = "밴드 멤버만 가능. 인당 maxVotesPerPerson표 제한. 중복 투표 불가.",
        security = @SecurityRequirement(name = "bearerAuth"),
        responses = {
            @ApiResponse(responseCode = "200", description = "투표 성공"),
            @ApiResponse(responseCode = "409", description = "중복 투표 또는 투표 가능 횟수 초과")
        })
    @PostMapping("/vote")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long bandId,
            @RequestBody @Valid VoteRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(songService.vote(bandId, request, userId));
    }

    @Operation(summary = "투표 전체 초기화 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/votes")
    public ResponseEntity<Void> resetVotes(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        songService.resetVotes(bandId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "곡 선정 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{bandSongId}/select")
    public ResponseEntity<BandSongResponse> selectSong(
            @PathVariable Long bandId,
            @PathVariable Long bandSongId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        return ResponseEntity.ok(songService.selectSong(bandId, bandSongId));
    }

    @Operation(summary = "곡 선정 취소 (리더 전용)",
        security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/{bandSongId}/select")
    public ResponseEntity<Void> deselectSong(
            @PathVariable Long bandId,
            @PathVariable Long bandSongId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        songService.deselectSong(bandId, bandSongId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "밴드 후보곡 전체 조회")
    @GetMapping
    public ResponseEntity<List<BandSongResponse>> getBandSongs(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getBandSongs(bandId));
    }

    @Operation(summary = "선정된 곡 목록 조회")
    @GetMapping("/selected")
    public ResponseEntity<List<BandSongResponse>> getSelectedSongs(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getSelectedSongs(bandId));
    }

    private void requireLeader(Long bandId, Long userId) {
        var band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));
        if (!band.getLeaderId().equals(userId)) {
            throw new UnauthorizedException("리더만 이 작업을 수행할 수 있습니다.");
        }
    }
}
