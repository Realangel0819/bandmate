package com.bandmate.song.service;

import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.common.exception.DuplicateException;
import com.bandmate.common.exception.InvalidRequestException;
import com.bandmate.common.exception.NotFoundException;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.*;
import com.bandmate.song.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SongService {

    private final SongRepository songRepository;
    private final BandSongRepository bandSongRepository;
    private final SongVoteRepository songVoteRepository;
    private final BandMemberRepository bandMemberRepository;

    // 곡 등록
    public Song createSong(CreateSongRequest request) {
        Song song = Song.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .youtubeUrl(request.getYoutubeUrl())
                .build();

        return songRepository.save(song);
    }

    // 밴드 곡 후보 추가
    public BandSongResponse addSongCandidate(Long bandId, AddSongCandidateRequest request, Long leaderId) {
        // 리더 확인은 컨트롤러에서 처리
        
        Song song = songRepository.findById(request.getSongId())
                .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));

        // 중복 후보 확인
        bandSongRepository.findByBandIdAndSongId(bandId, request.getSongId())
                .ifPresent(bs -> { throw new DuplicateException("이미 추가된 곡입니다."); });

        BandSong bandSong = BandSong.builder()
                .bandId(bandId)
                .songId(request.getSongId())
                .voteStartDate(request.getVoteStartDate())
                .voteEndDate(request.getVoteEndDate())
                .build();

        BandSong savedBandSong = bandSongRepository.save(bandSong);

        return convertToResponse(savedBandSong, song);
    }

    // 투표
    public VoteResponse vote(Long bandId, VoteRequest request, Long userId) {
        BandSong bandSong = bandSongRepository.findById(request.getBandSongId())
                .orElseThrow(() -> new NotFoundException("곡 후보를 찾을 수 없습니다."));

        // 투표 가능 여부 확인
        if (!bandSong.isVotingActive()) {
            throw new InvalidRequestException("투표 기간이 아닙니다.");
        }

        // 밴드 내 1인 1표 — 어떤 후보에도 이미 투표했으면 거부
        if (songVoteRepository.countByBandIdAndUserId(bandId, userId) > 0) {
            throw new DuplicateException("이미 투표했습니다.");
        }

        SongVote vote = SongVote.builder()
                .bandSongId(request.getBandSongId())
                .userId(userId)
                .bandId(bandId)
                .build();

        SongVote savedVote = songVoteRepository.save(vote);

        // 투표 수 업데이트
        bandSong.setVoteCount(songVoteRepository.countByBandSongId(request.getBandSongId()));
        bandSongRepository.save(bandSong);

        // 모든 밴드 멤버가 투표를 완료했으면 자동 마감 및 최다 득표 곡 선정
        String message = "투표가 완료되었습니다.";
        int totalVotes = songVoteRepository.countByBandId(bandId);
        int totalMembers = bandMemberRepository.countByBandId(bandId);

        if (totalVotes >= totalMembers) {
            bandSongRepository.findByBandId(bandId).stream()
                    .filter(bs -> !bs.getIsSelected() && bs.isVotingActive())
                    .max(Comparator.comparingInt(BandSong::getVoteCount))
                    .ifPresent(winner -> {
                        winner.setIsSelected(true);
                        bandSongRepository.save(winner);
                    });
            message = "모든 멤버가 투표를 완료했습니다. 최다 득표곡이 자동으로 선정되었습니다.";
        }

        return new VoteResponse(
                savedVote.getId(),
                savedVote.getBandSongId(),
                savedVote.getUserId(),
                message
        );
    }

    // 투표 마감 후 최다 득표 곡 자동 선정
    @Transactional
    public BandSongResponse selectWinningSong(Long bandId, Long bandSongId) {
        BandSong bandSong = bandSongRepository.findById(bandSongId)
                .orElseThrow(() -> new NotFoundException("곡 후보를 찾을 수 없습니다."));

        // 투표가 끝났는지 확인
        if (!bandSong.isVotingEnded()) {
            throw new InvalidRequestException("투표가 아직 진행 중입니다.");
        }

        // 이미 선정된 곡이 있으면 미선정으로 변경
        List<BandSong> selectedSongs = bandSongRepository.findByBandId(bandId).stream()
                .filter(BandSong::getIsSelected)
                .collect(Collectors.toList());

        for (BandSong selected : selectedSongs) {
            selected.setIsSelected(false);
            bandSongRepository.save(selected);
        }

        // 최다 득표 곡 선정
        bandSong.setIsSelected(true);
        BandSong savedBandSong = bandSongRepository.save(bandSong);

        Song song = songRepository.findById(bandSong.getSongId())
                .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));

        return convertToResponse(savedBandSong, song);
    }

    // 밴드의 활성 후보곡 조회 (투표 진행 중 또는 예정)
    public List<BandSongResponse> getActiveCandidates(Long bandId) {
        return bandSongRepository.findActiveCandidates(bandId).stream()
                .map(bandSong -> {
                    Song song = songRepository.findById(bandSong.getSongId())
                            .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));
                    return convertToResponse(bandSong, song);
                })
                .collect(Collectors.toList());
    }

    // 밴드의 모든 후보곡 조회
    public List<BandSongResponse> getBandSongs(Long bandId) {
        return bandSongRepository.findByBandId(bandId).stream()
                .map(bandSong -> {
                    Song song = songRepository.findById(bandSong.getSongId())
                            .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));
                    return convertToResponse(bandSong, song);
                })
                .collect(Collectors.toList());
    }

    // 선정된 곡 조회
    public BandSongResponse getSelectedSong(Long bandId) {
        BandSong bandSong = bandSongRepository.findSelectedSong(bandId)
                .orElseThrow(() -> new NotFoundException("선정된 곡이 없습니다."));

        Song song = songRepository.findById(bandSong.getSongId())
                .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));

        return convertToResponse(bandSong, song);
    }

    private BandSongResponse convertToResponse(BandSong bandSong, Song song) {
        return new BandSongResponse(
                bandSong.getId(),
                bandSong.getBandId(),
                bandSong.getSongId(),
                song.getTitle(),
                song.getArtist(),
                song.getYoutubeUrl(),
                bandSong.getVoteStartDate(),
                bandSong.getVoteEndDate(),
                bandSong.getVoteCount(),
                bandSong.getIsSelected(),
                bandSong.isVotingActive(),
                bandSong.getCreatedAt()
        );
    }
}