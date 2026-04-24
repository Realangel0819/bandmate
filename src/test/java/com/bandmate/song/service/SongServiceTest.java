package com.bandmate.song.service;

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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongServiceTest {

    @Mock
    private SongRepository songRepository;

    @Mock
    private BandSongRepository bandSongRepository;

    @Mock
    private SongVoteRepository songVoteRepository;

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
        given(songVoteRepository.findByBandSongIdAndUserId(bandSongId, userId)).willReturn(Optional.empty());
        given(songVoteRepository.save(any(SongVote.class))).willReturn(savedVote);
        given(songVoteRepository.countByBandSongId(bandSongId)).willReturn(1);
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);

        // when
        VoteResponse response = songService.vote(bandId, request, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
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
        given(songVoteRepository.findByBandSongIdAndUserId(bandSongId, userId))
                .willReturn(Optional.of(existingVote));

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
}