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
        Song song = songService.createSong(request);
        return ResponseEntity.ok(song);
    }

    @PostMapping("/candidates")
    public ResponseEntity<BandSongResponse> addSongCandidate(
            @PathVariable Long bandId,
            @RequestBody @Valid AddSongCandidateRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        var band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));
        if (!band.getLeaderId().equals(userId)) {
            throw new UnauthorizedException("리더만 곡을 추가할 수 있습니다.");
        }

        BandSongResponse response = songService.addSongCandidate(bandId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/vote")
    public ResponseEntity<VoteResponse> vote(
            @PathVariable Long bandId,
            @RequestBody @Valid VoteRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));
        VoteResponse response = songService.vote(bandId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{bandSongId}/select")
    public ResponseEntity<BandSongResponse> selectWinningSong(
            @PathVariable Long bandId,
            @PathVariable Long bandSongId,
            @RequestHeader("Authorization") String token) {
        Long userId = jwtUtil.getUserIdFromToken(token.substring(7));

        var band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));
        if (!band.getLeaderId().equals(userId)) {
            throw new UnauthorizedException("리더만 곡을 선정할 수 있습니다.");
        }

        BandSongResponse response = songService.selectWinningSong(bandId, bandSongId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<BandSongResponse>> getBandSongs(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getBandSongs(bandId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<BandSongResponse>> getActiveCandidates(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getActiveCandidates(bandId));
    }

    @GetMapping("/selected")
    public ResponseEntity<BandSongResponse> getSelectedSong(@PathVariable Long bandId) {
        return ResponseEntity.ok(songService.getSelectedSong(bandId));
    }
}
