package com.bandmate.song.controller;

import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.NotFoundException;
import com.bandmate.common.exception.UnauthorizedException;
import com.bandmate.common.util.JwtUtil;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.Song;
import com.bandmate.song.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bands/{bandId}/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final JwtUtil jwtUtil;
    private final BandRepository bandRepository;

    @PostMapping
    public ResponseEntity<Song> createSong(@RequestBody @Valid CreateSongRequest request) {
        return ResponseEntity.ok(songService.createSong(request));
    }

    @PostMapping("/candidates")
    public ResponseEntity<BandSongResponse> addSongCandidate(
            @PathVariable Long bandId,
            @RequestBody @Valid AddSongCandidateRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        return ResponseEntity.ok(songService.addSongCandidate(bandId, request, userId));
    }

    @DeleteMapping("/candidates")
    public ResponseEntity<Void> resetCandidates(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        songService.resetCandidates(bandId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/vote")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long bandId,
            @RequestBody @Valid VoteRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        return ResponseEntity.ok(songService.vote(bandId, request, userId));
    }

    @DeleteMapping("/votes")
    public ResponseEntity<Void> resetVotes(
            @PathVariable Long bandId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        songService.resetVotes(bandId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{bandSongId}/select")
    public ResponseEntity<BandSongResponse> selectSong(
            @PathVariable Long bandId,
            @PathVariable Long bandSongId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        requireLeader(bandId, userId);
        return ResponseEntity.ok(songService.selectSong(bandId, bandSongId));
    }

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

    @GetMapping
    public ResponseEntity<List<BandSongResponse>> getBandSongs(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getBandSongs(bandId));
    }

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
