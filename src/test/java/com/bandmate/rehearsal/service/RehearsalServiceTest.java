package com.bandmate.rehearsal.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.rehearsal.dto.AttendanceResponse;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.dto.RehearsalResponse;
import com.bandmate.rehearsal.entity.Rehearsal;
import com.bandmate.rehearsal.entity.RehearsalAttendance;
import com.bandmate.rehearsal.repository.RehearsalAttendanceRepository;
import com.bandmate.rehearsal.repository.RehearsalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RehearsalServiceTest {

    @Mock private RehearsalRepository rehearsalRepository;
    @Mock private RehearsalAttendanceRepository attendanceRepository;
    @Mock private BandRepository bandRepository;
    @Mock private BandMemberRepository bandMemberRepository;

    @InjectMocks
    private RehearsalService rehearsalService;

    // ──────────────── 일정 생성 ────────────────

    @Test
    @DisplayName("합주 일정 생성 성공")
    void createRehearsal_Success() {
        Long bandId = 1L;
        Long leaderId = 1L;

        Band band = Band.builder().id(bandId).leaderId(leaderId).build();
        Rehearsal saved = Rehearsal.builder()
                .id(1L).bandId(bandId).title("주간 합주")
                .rehearsalDate(LocalDateTime.now().plusDays(7))
                .location("홍대 연습실").maxCapacity(5).build();

        CreateRehearsalRequest request = new CreateRehearsalRequest(
                "주간 합주", "설명", LocalDateTime.now().plusDays(7), "홍대 연습실", 5);

        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(rehearsalRepository.save(any(Rehearsal.class))).willReturn(saved);

        RehearsalResponse response = rehearsalService.createRehearsal(bandId, request, leaderId);

        assertThat(response.getTitle()).isEqualTo("주간 합주");
        assertThat(response.getMaxCapacity()).isEqualTo(5);
        assertThat(response.getCurrentCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("리더가 아닌 경우 합주 일정 생성 실패")
    void createRehearsal_NotLeader_Fail() {
        Long bandId = 1L;

        Band band = Band.builder().id(bandId).leaderId(1L).build();
        CreateRehearsalRequest request = new CreateRehearsalRequest(
                "합주", "", LocalDateTime.now().plusDays(1), "연습실", 5);

        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));

        assertThatThrownBy(() -> rehearsalService.createRehearsal(bandId, request, 999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("리더만 합주 일정을 생성할 수 있습니다.");
    }

    // ──────────────── 참여 신청 ────────────────

    @Test
    @DisplayName("참여 신청 성공")
    void joinRehearsal_Success() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(2).build();
        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        RehearsalAttendance saved = RehearsalAttendance.builder()
                .id(1L).rehearsalId(rehearsalId).userId(userId).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)).willReturn(Optional.empty());
        given(attendanceRepository.save(any(RehearsalAttendance.class))).willReturn(saved);

        AttendanceResponse response = rehearsalService.joinRehearsal(bandId, rehearsalId, userId);

        assertThat(response.getRehearsalId()).isEqualTo(rehearsalId);
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(rehearsal.getCurrentCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("정원 초과 참여 신청 실패")
    void joinRehearsal_CapacityFull_Fail() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(5).build();
        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("정원이 초과되었습니다.");
    }

    @Test
    @DisplayName("중복 참여 신청 실패")
    void joinRehearsal_DuplicateAttendance_Fail() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(2).build();
        BandMember member = BandMember.builder().bandId(bandId).userId(userId).position(Position.GUITAR).build();
        RehearsalAttendance existing = RehearsalAttendance.builder()
                .rehearsalId(rehearsalId).userId(userId).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.of(member));
        given(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 참여 신청했습니다.");
    }

    @Test
    @DisplayName("밴드 멤버가 아닌 경우 참여 신청 실패")
    void joinRehearsal_NotMember_Fail() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(0).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(bandMemberRepository.findByBandIdAndUserId(bandId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> rehearsalService.joinRehearsal(bandId, rehearsalId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("밴드 멤버만 참여 신청할 수 있습니다.");
    }

    // ──────────────── 참여 취소 ────────────────

    @Test
    @DisplayName("참여 취소 성공")
    void cancelAttendance_Success() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(3).build();
        RehearsalAttendance attendance = RehearsalAttendance.builder()
                .id(1L).rehearsalId(rehearsalId).userId(userId).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)).willReturn(Optional.of(attendance));

        rehearsalService.cancelAttendance(bandId, rehearsalId, userId);

        assertThat(rehearsal.getCurrentCount()).isEqualTo(2);
        verify(attendanceRepository).delete(attendance);
    }

    @Test
    @DisplayName("참여 내역 없는 경우 취소 실패")
    void cancelAttendance_NotFound_Fail() {
        Long bandId = 1L;
        Long rehearsalId = 1L;
        Long userId = 2L;

        Rehearsal rehearsal = Rehearsal.builder()
                .id(rehearsalId).bandId(bandId).maxCapacity(5).currentCount(0).build();

        given(rehearsalRepository.findByIdWithLock(rehearsalId)).willReturn(Optional.of(rehearsal));
        given(attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> rehearsalService.cancelAttendance(bandId, rehearsalId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("참여 신청 내역이 없습니다.");
    }
}
