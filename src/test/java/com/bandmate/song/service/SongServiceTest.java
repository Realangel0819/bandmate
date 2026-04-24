package com.bandmate.song.service;

import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.*;
import com.bandmate.song.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock private SongRepository songRepository;
    @Mock private BandSongRepository bandSongRepository;
    @Mock private SongVoteRepository songVoteRepository;
    @Mock private BandMemberRepository bandMemberRepository;

    @InjectMocks
    private SongService songService;

    @Test
    @DisplayName("곡 등록 성공")
    void createSong_Success() {
        // given
        CreateSongRequest request = new CreateSongRequest();
        request.setTitle("Bohemian Rhapsody");
        request.setArtist("Queen");
        request.setYoutubeUrl("https://youtube.com/watch?v=...");

        Song savedSong = Song.builder()
                .id(1L)
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .youtubeUrl("https://youtube.com/watch?v=...")
                .build();

        given(songRepository.save(any(Song.class))).willReturn(savedSong);

        // when
        Song song = songService.createSong(request);

        // then
        assertThat(song).isNotNull();
        assertThat(song.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(songRepository, times(1)).save(any(Song.class));
    }

    @Test
    @DisplayName("곡 후보 추가 성공")
    void addSongCandidate_Success() {
        // given
        Long bandId = 1L;
        Long songId = 1L;
        Long leaderId = 1L;

        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest request = new AddSongCandidateRequest();
        request.setSongId(songId);
        request.setVoteStartDate(now.plusDays(1));
        request.setVoteEndDate(now.plusDays(5));

        Song song = Song.builder()
                .id(songId)
                .title("Bohemian Rhapsody")
                .artist("Queen")
                .youtubeUrl("https://youtube.com/watch?v=...")
                .build();

        BandSong savedBandSong = BandSong.builder()
                .id(1L)
                .bandId(bandId)
                .songId(songId)
                .voteStartDate(now.plusDays(1))
                .voteEndDate(now.plusDays(5))
                .build();

        given(songRepository.findById(songId)).willReturn(Optional.of(song));
        given(bandSongRepository.findByBandIdAndSongId(bandId, songId)).willReturn(Optional.empty());
        given(bandSongRepository.save(any(BandSong.class))).willReturn(savedBandSong);

        // when
        BandSongResponse response = songService.addSongCandidate(bandId, request, leaderId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(bandSongRepository, times(1)).save(any(BandSong.class));
    }

    @Test
    @DisplayName("투표 성공")
    void vote_Success() {
        // given
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;

        LocalDateTime now = LocalDateTime.now();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId)
                .bandId(bandId)
                .voteStartDate(now.minusHours(1))
                .voteEndDate(now.plusHours(1))
                .voteCount(0)
                .build();

        SongVote savedVote = SongVote.builder()
                .id(1L)
                .bandSongId(bandSongId)
                .userId(userId)
                .bandId(bandId)
                .build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(0); // 미투표 상태
        given(songVoteRepository.save(any(SongVote.class))).willReturn(savedVote);
        given(songVoteRepository.countByBandSongId(bandSongId)).willReturn(1);
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);
        given(songVoteRepository.countByBandId(bandId)).willReturn(1);
        given(bandMemberRepository.countByBandId(bandId)).willReturn(3); // 아직 모두 투표 안 함

        // when
        VoteResponse response = songService.vote(bandId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getMessage()).isEqualTo("투표가 완료되었습니다.");
        verify(songVoteRepository, times(1)).save(any(SongVote.class));
    }

    @Test
    @DisplayName("중복 투표 방지")
    void vote_PreventDuplicateVote() {
        // given
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;

        LocalDateTime now = LocalDateTime.now();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId)
                .bandId(bandId)
                .voteStartDate(now.minusHours(1))
                .voteEndDate(now.plusHours(1))
                .build();

        SongVote existingVote = SongVote.builder()
                .id(1L)
                .bandSongId(bandSongId)
                .userId(userId)
                .build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(1); // 이미 투표함

        // when & then
        assertThatThrownBy(() -> songService.vote(bandId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 투표했습니다.");
    }

    @Test
    @DisplayName("투표 기간 확인")
    void vote_CheckVotingPeriod() {
        // given
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;

        LocalDateTime now = LocalDateTime.now();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId)
                .bandId(bandId)
                .voteStartDate(now.plusDays(1)) // 투표가 아직 시작 안됨
                .voteEndDate(now.plusDays(5))
                .build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));

        // when & then
        assertThatThrownBy(() -> songService.vote(bandId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("투표 기간이 아닙니다.");
    }

    @Test
    @DisplayName("중복 곡 후보 추가 실패")
    void addSongCandidate_DuplicateSong_Fail() {
        Long bandId = 1L;
        Long songId = 1L;
        LocalDateTime now = LocalDateTime.now();

        Song song = Song.builder().id(songId).title("Test").artist("Test").build();
        BandSong existing = BandSong.builder().bandId(bandId).songId(songId)
                .voteStartDate(now).voteEndDate(now.plusDays(3)).build();

        AddSongCandidateRequest request = new AddSongCandidateRequest();
        request.setSongId(songId);
        request.setVoteStartDate(now.plusDays(1));
        request.setVoteEndDate(now.plusDays(5));

        given(songRepository.findById(songId)).willReturn(Optional.of(song));
        given(bandSongRepository.findByBandIdAndSongId(bandId, songId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> songService.addSongCandidate(bandId, request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 추가된 곡입니다.");
    }

    @Test
    @DisplayName("투표 마감 후 곡 선정 성공")
    void selectWinningSong_Success() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long songId = 1L;
        LocalDateTime now = LocalDateTime.now();

        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId).songId(songId)
                .voteStartDate(now.minusDays(5)).voteEndDate(now.minusDays(1))
                .voteCount(3).isSelected(false).build();

        Song song = Song.builder().id(songId).title("Bohemian Rhapsody").artist("Queen").build();

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(bandSongRepository.findByBandId(bandId)).willReturn(List.of(bandSong));
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);
        given(songRepository.findById(songId)).willReturn(Optional.of(song));

        BandSongResponse response = songService.selectWinningSong(bandId, bandSongId);

        assertThat(response.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(bandSongRepository, times(1)).save(any(BandSong.class));
    }

    @Test
    @DisplayName("투표 진행 중 곡 선정 실패")
    void selectWinningSong_VotingNotEnded_Fail() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        LocalDateTime now = LocalDateTime.now();

        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId).songId(1L)
                .voteStartDate(now.minusDays(1)).voteEndDate(now.plusDays(3))
                .build();

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));

        assertThatThrownBy(() -> songService.selectWinningSong(bandId, bandSongId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("투표가 아직 진행 중입니다.");
    }

    @Test
    @DisplayName("마지막 멤버 투표 시 최다 득표 곡 자동 선정")
    void vote_autoSelectWhenAllMembersVoted() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 3L;
        LocalDateTime now = LocalDateTime.now();

        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId).songId(1L)
                .voteStartDate(now.minusHours(1)).voteEndDate(now.plusHours(1))
                .voteCount(2).isSelected(false).build();

        SongVote savedVote = SongVote.builder()
                .id(1L).bandSongId(bandSongId).userId(userId).bandId(bandId).build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(0);
        given(songVoteRepository.save(any(SongVote.class))).willReturn(savedVote);
        given(songVoteRepository.countByBandSongId(bandSongId)).willReturn(3);
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);
        given(songVoteRepository.countByBandId(bandId)).willReturn(3);
        given(bandMemberRepository.countByBandId(bandId)).willReturn(3); // 전원 투표 완료
        given(bandSongRepository.findByBandId(bandId)).willReturn(List.of(bandSong));

        VoteResponse response = songService.vote(bandId, request, userId);

        assertThat(response.getMessage()).contains("자동으로 선정되었습니다");
        // 자동 선정 → isSelected = true
        assertThat(bandSong.getIsSelected()).isTrue();
        verify(bandSongRepository, times(2)).save(any(BandSong.class)); // voteCount 업데이트 + 자동 선정
    }
}