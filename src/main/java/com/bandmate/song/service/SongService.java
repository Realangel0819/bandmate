package com.bandmate.song.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.*;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.*;
import com.bandmate.song.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final BandRepository bandRepository;

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
        Song song = songRepository.findById(request.getSongId())
                .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));

        bandSongRepository.findByBandIdAndSongId(bandId, request.getSongId())
                .ifPresent(bs -> { throw new DuplicateException("이미 추가된 곡입니다."); });

        BandSong bandSong = BandSong.builder()
                .bandId(bandId)
                .songId(request.getSongId())
                .voteStartDate(request.getVoteStartDate())
                .voteEndDate(request.getVoteEndDate())
                .build();

        return convertToResponse(bandSongRepository.save(bandSong), song);
    }

    // 투표 (밴드 멤버만, 인당 maxVotesPerPerson표)
    public VoteResponse vote(Long bandId, VoteRequest request, Long userId) {
        bandMemberRepository.findByBandIdAndUserId(bandId, userId)
                .orElseThrow(() -> new UnauthorizedException("밴드 멤버만 투표할 수 있습니다."));

        BandSong bandSong = bandSongRepository.findById(request.getBandSongId())
                .orElseThrow(() -> new NotFoundException("곡 후보를 찾을 수 없습니다."));

        if (!bandSong.isVotingActive()) {
            throw new InvalidRequestException("투표 기간이 아닙니다.");
        }

        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        int votesUsed = songVoteRepository.countByBandIdAndUserId(bandId, userId);
        if (votesUsed >= band.getMaxVotesPerPerson()) {
            throw new InvalidRequestException("투표 가능 횟수(" + band.getMaxVotesPerPerson() + "표)를 모두 사용했습니다.");
        }

        if (songVoteRepository.findByBandSongIdAndUserId(request.getBandSongId(), userId).isPresent()) {
            throw new AlreadyVotedException("이 곡에 이미 투표했습니다.");
        }

        SongVote vote = SongVote.builder()
                .bandSongId(request.getBandSongId())
                .userId(userId)
                .bandId(bandId)
                .build();

        SongVote savedVote = songVoteRepository.save(vote);

        bandSong.setVoteCount(songVoteRepository.countByBandSongId(request.getBandSongId()));
        bandSongRepository.save(bandSong);

        // 모든 멤버가 모든 투표를 완료했으면 최다 득표 곡 자동 선정
        String message = "투표가 완료되었습니다.";
        int totalVotes = songVoteRepository.countByBandId(bandId);
        int totalMembers = bandMemberRepository.countByBandId(bandId);

        if (totalVotes >= totalMembers * band.getMaxVotesPerPerson()) {
            bandSongRepository.findByBandId(bandId).stream()
                    .filter(bs -> !bs.getIsSelected() && bs.isVotingActive())
                    .max(java.util.Comparator.comparingInt(BandSong::getVoteCount))
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

    // 곡 선정 (리더, 복수 선택 가능)
    public BandSongResponse selectSong(Long bandId, Long bandSongId) {
        BandSong bandSong = bandSongRepository.findById(bandSongId)
                .orElseThrow(() -> new NotFoundException("곡 후보를 찾을 수 없습니다."));

        bandSong.setIsSelected(true);
        BandSong saved = bandSongRepository.save(bandSong);

        Song song = songRepository.findById(bandSong.getSongId())
                .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));

        return convertToResponse(saved, song);
    }

    // 곡 선정 취소 (리더)
    public void deselectSong(Long bandId, Long bandSongId) {
        BandSong bandSong = bandSongRepository.findById(bandSongId)
                .orElseThrow(() -> new NotFoundException("곡 후보를 찾을 수 없습니다."));

        bandSong.setIsSelected(false);
        bandSongRepository.save(bandSong);
    }

    // 후보곡 전체 초기화 (리더)
    public void resetCandidates(Long bandId) {
        songVoteRepository.deleteByBandId(bandId);
        bandSongRepository.deleteByBandId(bandId);
    }

    // 투표 전체 초기화 (리더, 후보곡 유지)
    public void resetVotes(Long bandId) {
        songVoteRepository.deleteByBandId(bandId);
        bandSongRepository.findByBandId(bandId).forEach(bs -> {
            bs.setVoteCount(0);
            bs.setIsSelected(false);
            bandSongRepository.save(bs);
        });
    }

    // 밴드의 모든 후보곡 조회
    @Transactional(readOnly = true)
    public List<BandSongResponse> getBandSongs(Long bandId) {
        return bandSongRepository.findByBandId(bandId).stream()
                .map(bandSong -> {
                    Song song = songRepository.findById(bandSong.getSongId())
                            .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));
                    return convertToResponse(bandSong, song);
                })
                .collect(Collectors.toList());
    }

    // 선정된 곡들 조회
    @Transactional(readOnly = true)
    public List<BandSongResponse> getSelectedSongs(Long bandId) {
        return bandSongRepository.findSelectedSongs(bandId).stream()
                .map(bandSong -> {
                    Song song = songRepository.findById(bandSong.getSongId())
                            .orElseThrow(() -> new NotFoundException("곡을 찾을 수 없습니다."));
                    return convertToResponse(bandSong, song);
                })
                .collect(Collectors.toList());
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
