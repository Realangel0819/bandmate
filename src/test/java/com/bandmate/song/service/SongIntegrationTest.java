package com.bandmate.song.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
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

/**
 * H2 in-memory DB를 사용하는 공연곡 선정 통합 테스트.
 * @Transactional → 각 테스트 후 자동 롤백.
 */
@SpringBootTest
@Transactional
class SongIntegrationTest {

    @Autowired private SongService songService;
    @Autowired private SongRepository songRepository;
    @Autowired private BandSongRepository bandSongRepository;
    @Autowired private SongVoteRepository songVoteRepository;
    @Autowired private BandRepository bandRepository;
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
                .nickname("리더").build());
        leaderId = leader.getId();

        User m1 = userRepository.save(User.builder()
                .email("song_m1_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버1").build());
        member1Id = m1.getId();

        User m2 = userRepository.save(User.builder()
                .email("song_m2_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버2").build());
        member2Id = m2.getId();

        Band band = bandRepository.save(Band.builder()
                .name("SongTestBand").description("").leaderId(leaderId).build());
        bandId = band.getId();

        // 멤버 등록
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
        // 곡 등록
        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("Under Pressure");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        // 후보 등록 (투표 기간: 과거 ~ 미래)
        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusHours(1));
        candidateReq.setVoteEndDate(now.plusDays(3));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        assertThat(candidate.getVoteCount()).isEqualTo(0);
        assertThat(candidate.getIsVotingActive()).isTrue();

        // 멤버1 투표
        VoteRequest voteReq = new VoteRequest();
        voteReq.setBandSongId(candidate.getId());
        VoteResponse vote1 = songService.vote(bandId, voteReq, member1Id);
        assertThat(vote1.getMessage()).isEqualTo("투표가 완료되었습니다.");

        // 멤버2 투표
        VoteResponse vote2 = songService.vote(bandId, voteReq, member2Id);
        assertThat(vote2.getMessage()).isEqualTo("투표가 완료되었습니다.");

        // DB에서 voteCount 확인
        BandSong bandSong = bandSongRepository.findById(candidate.getId()).orElseThrow();
        assertThat(bandSong.getVoteCount()).isEqualTo(2);
        assertThat(songVoteRepository.countByBandSongId(candidate.getId())).isEqualTo(2);
    }

    @Test
    @DisplayName("중복 투표 방지 (DB 고유 제약 검증)")
    void vote_duplicatePrevented_withDb() {
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
        voteReq.setBandSongId(candidate.getId());

        // 첫 번째 투표 성공
        songService.vote(bandId, voteReq, member1Id);

        // 두 번째 투표 실패 (서비스 레벨 중복 방지)
        assertThatThrownBy(() -> songService.vote(bandId, voteReq, member1Id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 투표했습니다.");
    }

    @Test
    @DisplayName("투표 종료 후 곡 선정 → isSelected=true, selectedSong 조회 가능")
    void selectWinningSong_marksSelectedAndQueryable() {
        CreateSongRequest songReq = new CreateSongRequest();
        songReq.setTitle("Radio Ga Ga");
        songReq.setArtist("Queen");
        Song song = songService.createSong(songReq);

        // 투표 기간: 이미 종료
        LocalDateTime now = LocalDateTime.now();
        AddSongCandidateRequest candidateReq = new AddSongCandidateRequest();
        candidateReq.setSongId(song.getId());
        candidateReq.setVoteStartDate(now.minusDays(5));
        candidateReq.setVoteEndDate(now.minusDays(1));
        BandSongResponse candidate = songService.addSongCandidate(bandId, candidateReq, leaderId);

        // 곡 선정
        BandSongResponse selected = songService.selectWinningSong(bandId, candidate.getId());

        assertThat(selected.getIsSelected()).isTrue();
        assertThat(selected.getTitle()).isEqualTo("Radio Ga Ga");

        // getSelectedSong 조회 가능
        BandSongResponse found = songService.getSelectedSong(bandId);
        assertThat(found.getId()).isEqualTo(candidate.getId());
    }

    @Test
    @DisplayName("활성 후보곡 조회 - 선정된 곡 제외")
    void getActiveCandidates_excludesSelected() {
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

        // 곡 B: 투표 종료 후 선정
        CreateSongRequest reqB = new CreateSongRequest();
        reqB.setTitle("Song B");
        reqB.setArtist("Artist B");
        Song songB = songService.createSong(reqB);
        AddSongCandidateRequest candB = new AddSongCandidateRequest();
        candB.setSongId(songB.getId());
        candB.setVoteStartDate(now.minusDays(5));
        candB.setVoteEndDate(now.minusDays(1));
        BandSongResponse candidateB = songService.addSongCandidate(bandId, candB, leaderId);
        songService.selectWinningSong(bandId, candidateB.getId());

        // 활성 후보 조회: 선정된 곡(B)은 제외, A만 포함
        List<BandSongResponse> active = songService.getActiveCandidates(bandId);
        assertThat(active).hasSize(1);
        assertThat(active.get(0).getTitle()).isEqualTo("Song A");
    }

    @Test
    @DisplayName("모든 멤버가 투표하면 최다 득표 곡 자동 선정 (DB 검증)")
    void vote_autoSelectWhenAllMembersVoted_withDb() {
        // 밴드 멤버: leader + member1 + member2 = 총 3명
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

        // 멤버1, 멤버2 투표 → 아직 leader 미투표 상태
        VoteRequest voteReq = new VoteRequest();
        voteReq.setBandSongId(candidate.getId());

        VoteResponse v1 = songService.vote(bandId, voteReq, member1Id);
        assertThat(v1.getMessage()).isEqualTo("투표가 완료되었습니다.");

        VoteResponse v2 = songService.vote(bandId, voteReq, member2Id);
        assertThat(v2.getMessage()).isEqualTo("투표가 완료되었습니다.");

        // 아직 자동 선정 안 됨 (3명 중 2명만 투표)
        BandSong notYet = bandSongRepository.findById(candidate.getId()).orElseThrow();
        assertThat(notYet.getIsSelected()).isFalse();

        // 리더(마지막 멤버) 투표 → 전원 투표 완료 → 자동 선정
        VoteResponse v3 = songService.vote(bandId, voteReq, leaderId);
        assertThat(v3.getMessage()).contains("자동으로 선정되었습니다");

        // DB에서 isSelected = true 확인
        BandSong autoSelected = bandSongRepository.findById(candidate.getId()).orElseThrow();
        assertThat(autoSelected.getIsSelected()).isTrue();

        // getSelectedSong으로도 조회 가능
        BandSongResponse selectedSong = songService.getSelectedSong(bandId);
        assertThat(selectedSong.getTitle()).isEqualTo("Auto Selected Song");
    }

}
