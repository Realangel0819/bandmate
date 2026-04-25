package com.bandmate.song.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.AlreadyVotedException;
import com.bandmate.song.dto.*;
import com.bandmate.song.entity.BandSong;
import com.bandmate.song.entity.Song;
import com.bandmate.song.repository.BandSongRepository;
import com.bandmate.song.repository.SongRepository;
import com.bandmate.song.repository.SongVoteRepository;
import com.bandmate.user.entity.User;
import com.bandmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class SongIntegrationTest {

    @Autowired private SongService songService;
    @Autowired private BandRepository bandRepository;
    @Autowired private SongRepository songRepository;
    @Autowired private BandSongRepository bandSongRepository;
    @Autowired private SongVoteRepository songVoteRepository;
    @Autowired private BandMemberRepository bandMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long bandId;
    private Long leaderId;
    private Long member1Id;
    private Long member2Id;

    @BeforeEach
    void setUp() {
        User leader = userRepository.save(User.builder()
                .email("song_leader_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("리더" + System.nanoTime()).build());
        leaderId = leader.getId();

        User m1 = userRepository.save(User.builder()
                .email("song_m1_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버1" + System.nanoTime()).build());
        member1Id = m1.getId();

        User m2 = userRepository.save(User.builder()
                .email("song_m2_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버2" + System.nanoTime()).build());
        member2Id = m2.getId();

        Band band = bandRepository.save(Band.builder()
                .name("SongTestBand").description("").leaderId(leaderId).build());
        bandId = band.getId();

        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(leaderId).position(Position.VOCAL).build());
        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(member1Id).position(Position.GUITAR).build());
        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(member2Id).position(Position.DRUM).build());
    }

    @Test
    @DisplayName("곡 등록 후 DB에서 조회 가능")
    void createSong_persistsToDb() {
        CreateSongRequest request = new CreateSongRequest();
        request.setTitle("Bohemian Rhapsody");
        request.setArtist("Queen");
        request.setYoutubeUrl("https://youtube.com/watch?v=test");

        Song song = songService.createSong(request);

        assertThat(song.getId()).isNotNull();
        assertThat(song.getTitle()).isEqualTo("Bohemian Rhapsody");
        assertThat(songRepository.findById(song.getId())).isPresent();
    }

    @Test
    @DisplayName("곡 후보 등록 → 투표 → voteCount 증가 확인")
    void addCandidate_andVote_voteCountIncreases() {
        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("Under Pressure");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusHours(1));
        candidateReq.setVoteEndDate(now.plusDays(3));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        assertThat(candidate.getVoteCount()).isEqualTo(0);
        assertThat(candidate.getIsVotingActive()).isTrue();

        VoteRequest voteReq = new VoteRequest();
        voteReq.setBandSongId(candidate.getBandSongId());
        VoteResponse vote1 = songService.vote(bandId, voteReq, member1Id);
        assertThat(vote1.getMessage()).isEqualTo("투표가 완료되었습니다.");

        VoteResponse vote2 = songService.vote(bandId, voteReq, member2Id);
        assertThat(vote2.getMessage()).isEqualTo("투표가 완료되었습니다.");

        BandSong bandSong = bandSongRepository.findById(candidate.getBandSongId()).orElseThrow();
        assertThat(bandSong.getVoteCount()).isEqualTo(2);
        assertThat(songVoteRepository.countByBandSongId(candidate.getBandSongId())).isEqualTo(2);
    }

    @Test
    @DisplayName("중복 투표 방지 (DB 고유 제약 검증)")
    void vote_duplicatePrevented_withDb() {
        // maxVotesPerPerson=1이면 두 번째 시도에서 "횟수 초과"가 먼저 발동하므로 3으로 설정
        Band band = bandRepository.findById(bandId).orElseThrow();
        band.setMaxVotesPerPerson(3);
        bandRepository.save(band);

        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("We Will Rock You");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusHours(1));
        candidateReq.setVoteEndDate(now.plusDays(3));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        VoteRequest voteReq = new VoteRequest();
        voteReq.setBandSongId(candidate.getBandSongId());

        songService.vote(bandId, voteReq, member1Id);

        assertThatThrownBy(() -> songService.vote(bandId, voteReq, member1Id))
                .isInstanceOf(AlreadyVotedException.class)
                .hasMessage("이 곡에 이미 투표했습니다.");
    }

    @Test
    @DisplayName("투표 종료 후 곡 선정 → isSelected=true, 선정 목록에서 조회 가능")
    void selectSong_marksSelectedAndQueryable() {
        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("Radio Ga Ga");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusDays(5));
        candidateReq.setVoteEndDate(now.minusDays(1));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        BandSongResponse selected = songService.selectSong(bandId, candidate.getBandSongId());

        assertThat(selected.getIsSelected()).isTrue();
        assertThat(selected.getTitle()).isEqualTo("Radio Ga Ga");

        List<BandSongResponse> selectedSongs = songService.getSelectedSongs(bandId);
        assertThat(selectedSongs).hasSize(1);
        assertThat(selectedSongs.get(0).getBandSongId()).isEqualTo(candidate.getBandSongId());
    }

    @Test
    @DisplayName("선정된 곡을 제외한 후보곡 조회")
    void getBandSongs_excludesSelectedWhenFiltered() {
        LocalDateTime now = LocalDateTime.now();

        // 곡 A: 활성 후보
        CreateSongRequest reqA = new CreateSongRequest();
        reqA.setTitle("Song A");
        reqA.setArtist("Artist A");
        Song songA = songService.createSong(reqA);
        AddSongCandidateRequest candA = new AddSongCandidateRequest();
        candA.setSongId(songA.getId());
        candA.setVoteStartDate(now.minusHours(1));
        candA.setVoteEndDate(now.plusDays(3));
        songService.addSongCandidate(bandId, candA, leaderId);

        // 곡 B: 선정 완료
        CreateSongRequest reqB = new CreateSongRequest();
        reqB.setTitle("Song B");
        reqB.setArtist("Artist B");
        Song songB = songService.createSong(reqB);
        AddSongCandidateRequest candB = new AddSongCandidateRequest();
        candB.setSongId(songB.getId());
        candB.setVoteStartDate(now.minusDays(5));
        candB.setVoteEndDate(now.minusDays(1));
        BandSongResponse candidateB = songService.addSongCandidate(bandId, candB, leaderId);
        songService.selectSong(bandId, candidateB.getBandSongId());

        List<BandSongResponse> selected = songService.getSelectedSongs(bandId);
        assertThat(selected).hasSize(1);
        assertThat(selected.get(0).getTitle()).isEqualTo("Song B");

        List<BandSongResponse> all = songService.getBandSongs(bandId);
        assertThat(all).hasSize(2);
    }

    @Test
    @DisplayName("모든 멤버가 투표하면 최다 득표 곡 자동 선정 (DB 검증)")
    void vote_autoSelectWhenAllMembersVoted_withDb() {
        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("Auto Selected Song");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusHours(1));
        candidateReq.setVoteEndDate(now.plusDays(3));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        VoteRequest voteReq = new VoteRequest();
        voteReq.setBandSongId(candidate.getBandSongId());

        VoteResponse v1 = songService.vote(bandId, voteReq, member1Id);
        assertThat(v1.getMessage()).isEqualTo("투표가 완료되었습니다.");

        VoteResponse v2 = songService.vote(bandId, voteReq, member2Id);
        assertThat(v2.getMessage()).isEqualTo("투표가 완료되었습니다.");

        // 아직 자동 선정 안 됨 (3명 중 2명만 투표)
        BandSong notYet = bandSongRepository.findById(candidate.getBandSongId()).orElseThrow();
        assertThat(notYet.getIsSelected()).isFalse();

        // 리더(마지막 멤버) 투표 → 전원 투표 완료 → 자동 선정
        VoteResponse v3 = songService.vote(bandId, voteReq, leaderId);
        assertThat(v3.getMessage()).contains("자동으로 선정되었습니다");

        BandSong autoSelected = bandSongRepository.findById(candidate.getBandSongId()).orElseThrow();
        assertThat(autoSelected.getIsSelected()).isTrue();

        List<BandSongResponse> selectedSongs = songService.getSelectedSongs(bandId);
        assertThat(selectedSongs).hasSize(1);
        assertThat(selectedSongs.get(0).getTitle()).isEqualTo("Auto Selected Song");
    }
}
