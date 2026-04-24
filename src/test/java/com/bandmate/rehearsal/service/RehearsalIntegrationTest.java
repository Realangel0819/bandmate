package com.bandmate.rehearsal.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.rehearsal.dto.AttendanceResponse;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.entity.Rehearsal;
import com.bandmate.rehearsal.repository.RehearsalAttendanceRepository;
import com.bandmate.rehearsal.repository.RehearsalRepository;
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
 * H2 in-memory DB를 사용하는 합주 일정 통합 테스트.
 * 비관적 락 정원 제한 로직을 실제 DB 트랜잭션으로 검증.
 * @Transactional → 각 테스트 후 자동 롤백.
 */
@SpringBootTest
@Transactional
class RehearsalIntegrationTest {

    @Autowired private RehearsalService rehearsalService;
    @Autowired private RehearsalRepository rehearsalRepository;
    @Autowired private RehearsalAttendanceRepository attendanceRepository;
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
                .email("rh_leader_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("리더").build());
        leaderId = leader.getId();

        User m1 = userRepository.save(User.builder()
                .email("rh_m1_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버1").build());
        member1Id = m1.getId();

        User m2 = userRepository.save(User.builder()
                .email("rh_m2_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버2").build());
        member2Id = m2.getId();

        Band band = bandRepository.save(Band.builder()
                .name("RehearsalTestBand").description("").leaderId(leaderId).build());
        bandId = band.getId();

        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(leaderId).position(Position.VOCAL).build());
        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(member1Id).position(Position.GUITAR).build());
        bandMemberRepository.save(BandMember.builder().bandId(bandId).userId(member2Id).position(Position.DRUM).build());
    }

    private Long createRehearsal(int capacity) {
        CreateRehearsalRequest request = new CreateRehearsalRequest(
                "정기 합주", "주간 합주", LocalDateTime.now().plusDays(7), "홍대 연습실", capacity);
        return rehearsalService.createRehearsal(bandId, request, leaderId).getId();
    }

    @Test
    @DisplayName("합주 일정 생성 후 DB에서 조회 가능")
    void createRehearsal_persistsToDb() {
        Long rehearsalId = createRehearsal(5);

        assertThat(rehearsalRepository.findById(rehearsalId)).isPresent();

        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId).orElseThrow();
        assertThat(rehearsal.getTitle()).isEqualTo("정기 합주");
        assertThat(rehearsal.getMaxCapacity()).isEqualTo(5);
        assertThat(rehearsal.getCurrentCount()).isEqualTo(0);
        assertThat(rehearsal.getBandId()).isEqualTo(bandId);
    }

    @Test
    @DisplayName("리더가 아닌 경우 합주 일정 생성 실패")
    void createRehearsal_notLeader_fails() {
        CreateRehearsalRequest request = new CreateRehearsalRequest(
                "불법합주", "", LocalDateTime.now().plusDays(1), "연습실", 5);

        assertThatThrownBy(() -> rehearsalService.createRehearsal(bandId, request, member1Id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리더만 합주 일정을 생성할 수 있습니다.");
    }

    @Test
    @DisplayName("참여 신청 → DB currentCount 증가 + attendance 저장 확인")
    void joinRehearsal_incrementsCountAndSavesAttendance() {
        Long rehearsalId = createRehearsal(5);

        AttendanceResponse response = rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id);

        assertThat(response.getUserId()).isEqualTo(member1Id);
        assertThat(response.getRehearsalId()).isEqualTo(rehearsalId);

        // currentCount 증가 확인 (같은 트랜잭션 내 1차 캐시에서 반영됨)
        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId).orElseThrow();
        assertThat(rehearsal.getCurrentCount()).isEqualTo(1);

        // attendance 저장 확인
        assertThat(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, member1Id)).isPresent();
    }

    @Test
    @DisplayName("정원 초과 참여 신청 불가 (DB 상태 기반)")
    void joinRehearsal_capacityLimit_withDb() {
        Long rehearsalId = createRehearsal(1);

        // 정원 1명 → member1 신청 성공
        rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id);

        // member2 신청 실패 (정원 초과)
        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, member2Id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정원이 초과되었습니다.");

        // DB attendance 1개만 존재
        assertThat(attendanceRepository.findByRehearsalId(rehearsalId)).hasSize(1);

        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId).orElseThrow();
        assertThat(rehearsal.getCurrentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("중복 참여 신청 불가 (DB 상태 기반)")
    void joinRehearsal_duplicatePrevented_withDb() {
        Long rehearsalId = createRehearsal(5);

        rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id);

        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 참여 신청했습니다.");

        // DB에 attendance 1개만 존재
        assertThat(attendanceRepository.findByRehearsalId(rehearsalId)).hasSize(1);
    }

    @Test
    @DisplayName("참여 취소 → DB currentCount 감소 + attendance 삭제 확인")
    void cancelAttendance_decrementsCountAndDeletesAttendance() {
        Long rehearsalId = createRehearsal(5);

        rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id);
        rehearsalService.joinRehearsal(bandId, rehearsalId, member2Id);

        // member1 취소
        rehearsalService.cancelAttendance(bandId, rehearsalId, member1Id);

        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId).orElseThrow();
        assertThat(rehearsal.getCurrentCount()).isEqualTo(1);
        assertThat(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, member1Id)).isEmpty();
        assertThat(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, member2Id)).isPresent();
    }

    @Test
    @DisplayName("리더가 참여자 목록 조회 가능")
    void getAttendances_leaderCanView() {
        Long rehearsalId = createRehearsal(5);

        rehearsalService.joinRehearsal(bandId, rehearsalId, member1Id);
        rehearsalService.joinRehearsal(bandId, rehearsalId, member2Id);

        List<AttendanceResponse> attendances = rehearsalService.getAttendances(bandId, rehearsalId, leaderId);

        assertThat(attendances).hasSize(2);
        assertThat(attendances).extracting(AttendanceResponse::getUserId)
                .containsExactlyInAnyOrder(member1Id, member2Id);
    }

    @Test
    @DisplayName("비멤버는 참여 신청 불가")
    void joinRehearsal_nonMember_fails() {
        Long rehearsalId = createRehearsal(5);

        User stranger = userRepository.save(User.builder()
                .email("stranger_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("외부인").build());

        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, stranger.getId()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("밴드 멤버만 참여 신청할 수 있습니다.");
    }
}
