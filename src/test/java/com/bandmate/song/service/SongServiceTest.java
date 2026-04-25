package com.bandmate.song.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.AlreadyVotedException;
import com.bandmate.common.exception.UnauthorizedException;
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
    @Mock private BandRepository bandRepository;

    @InjectMocks
    private SongService songService;

    @Test
    @DisplayName("곡 등록 성공")
    void createSong_Success() {
        CreateSongRequest request = new CreateSongRequest();
        request.setTitle("Bohemian Rhapsody");
        request.setArtist("Queen");
        request.setYoutubeUrl("https://youtube.com/watch?v=...");

        Song savedSong = Song.builder()
                .id(1L).title("Bohemian Rhapsody").artist("Queen")
                .youtubeUrl("https://youtube.com/watch?v=...").build();

        given(songRepository.save(any(Song.class))).willReturn(savedSong);

        Song song = songService.createSong(request);

        assertThat(song.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(songRepository, times(1)).save(any(Song.class));
    }

    @Test
    @DisplayName("곡 후보 추가 성공")
    void addSongCandidate_Success() {
        Long bandId = 1L;
        Long songId = 1L;
        LocalDateTime now = LocalDateTime.now();

        AddSongCandidateRequest request = new AddSongCandidateRequest();
        request.setSongId(songId);
        request.setVoteStartDate(now.plusDays(1));
        request.setVoteEndDate(now.plusDays(5));

        Song song = Song.builder().id(songId).title("Bohemian Rhapsody").artist("Queen")
                .youtubeUrl("https://youtube.com/watch?v=...").build();

        BandSong savedBandSong = BandSong.builder()
                .id(1L).bandId(bandId).songId(songId)
                .voteStartDate(now.plusDays(1)).voteEndDate(now.plusDays(5)).build();

        given(songRepository.findById(songId)).willReturn(Optional.of(song));
        given(bandSongRepository.findByBandIdAndSongId(bandId, songId)).willReturn(Optional.empty());
        given(bandSongRepository.save(any(BandSong.class))).willReturn(savedBandSong);

        BandSongResponse response = songService.addSongCandidate(bandId, request, 1L);

        assertThat(response.getTitle()).isEqualTo("Bohemian Rhapsody");
        verify(bandSongRepository, times(1)).save(any(BandSong.class));
    }

    @Test
    @DisplayName("투표 성공")
    void vote_Success() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;
        LocalDateTime now = LocalDateTime.now();

        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        Band band = Band.builder().id(bandId).leaderId(1L).maxVotesPerPerson(3).build();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId)
                .voteStartDate(now.minusHours(1)).voteEndDate(now.plusHours(1))
                .voteCount(0).build();
        SongVote savedVote = SongVote.builder().id(1L).bandSongId(bandSongId).userId(userId).bandId(bandId).build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(0);
        given(songVoteRepository.findByBandSongIdAndUserId(bandSongId, userId)).willReturn(Optional.empty());
        given(songVoteRepository.save(any(SongVote.class))).willReturn(savedVote);
        given(songVoteRepository.countByBandSongId(bandSongId)).willReturn(1);
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);
        given(songVoteRepository.countByBandId(bandId)).willReturn(1);
        given(bandMemberRepository.countByBandId(bandId)).willReturn(3);

        VoteResponse response = songService.vote(bandId, request, userId);

        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getMessage()).isEqualTo("투표가 완료되었습니다.");
        verify(songVoteRepository, times(1)).save(any(SongVote.class));
    }

    @Test
    @DisplayName("중복 투표 방지 — AlreadyVotedException")
    void vote_PreventDuplicateVote() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;
        LocalDateTime now = LocalDateTime.now();

        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        Band band = Band.builder().id(bandId).leaderId(1L).maxVotesPerPerson(3).build();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId)
                .voteStartDate(now.minusHours(1)).voteEndDate(now.plusHours(1)).build();
        SongVote existingVote = SongVote.builder().id(1L).bandSongId(bandSongId).userId(userId).build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(0);
        given(songVoteRepository.findByBandSongIdAndUserId(bandSongId, userId)).willReturn(Optional.of(existingVote));

        assertThatThrownBy(() -> songService.vote(bandId, request, userId))
                .isInstanceOf(AlreadyVotedException.class)
                .hasMessage("이 곡에 이미 투표했습니다.");
    }

    @Test
    @DisplayName("밴드 멤버가 아닌 경우 투표 실패")
    void vote_NonMember_Fail() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 99L;

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> songService.vote(bandId, request, userId))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("밴드 멤버만 투표할 수 있습니다.");
    }

    @Test
    @DisplayName("투표 기간 아닌 경우 투표 실패")
    void vote_CheckVotingPeriod() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 2L;
        LocalDateTime now = LocalDateTime.now();

        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId)
                .voteStartDate(now.plusDays(1))
                .voteEndDate(now.plusDays(5)).build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));

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
    @DisplayName("마지막 멤버 투표 시 최다 득표 곡 자동 선정")
    void vote_autoSelectWhenAllMembersVoted() {
        Long bandId = 1L;
        Long bandSongId = 1L;
        Long userId = 3L;
        LocalDateTime now = LocalDateTime.now();

        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        Band band = Band.builder().id(bandId).leaderId(1L).maxVotesPerPerson(1).build();
        BandSong bandSong = BandSong.builder()
                .id(bandSongId).bandId(bandId).songId(1L)
                .voteStartDate(now.minusHours(1)).voteEndDate(now.plusHours(1))
                .voteCount(2).isSelected(false).build();
        SongVote savedVote = SongVote.builder().id(1L).bandSongId(bandSongId).userId(userId).bandId(bandId).build();

        VoteRequest request = new VoteRequest();
        request.setBandSongId(bandSongId);

        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(bandSongRepository.findById(bandSongId)).willReturn(Optional.of(bandSong));
        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(songVoteRepository.countByBandIdAndUserId(bandId, userId)).willReturn(0);
        given(songVoteRepository.findByBandSongIdAndUserId(bandSongId, userId)).willReturn(Optional.empty());
        given(songVoteRepository.save(any(SongVote.class))).willReturn(savedVote);
        given(songVoteRepository.countByBandSongId(bandSongId)).willReturn(3);
        given(bandSongRepository.save(any(BandSong.class))).willReturn(bandSong);
        given(songVoteRepository.countByBandId(bandId)).willReturn(3);
        given(bandMemberRepository.countByBandId(bandId)).willReturn(3);
        given(bandSongRepository.findByBandId(bandId)).willReturn(List.of(bandSong));

        VoteResponse response = songService.vote(bandId, request, userId);

        assertThat(response.getMessage()).contains("자동으로 선정되었습니다");
        assertThat(bandSong.getIsSelected()).isTrue();
        verify(bandSongRepository, times(2)).save(any(BandSong.class));
    }
}
