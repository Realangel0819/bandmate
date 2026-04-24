package com.bandmate.rehearsal.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.common.exception.DuplicateException;
import com.bandmate.common.exception.InvalidRequestException;
import com.bandmate.common.exception.NotFoundException;
import com.bandmate.common.exception.UnauthorizedException;
import com.bandmate.rehearsal.dto.AttendanceResponse;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.dto.RehearsalResponse;
import com.bandmate.rehearsal.entity.Rehearsal;
import com.bandmate.rehearsal.entity.RehearsalAttendance;
import com.bandmate.rehearsal.repository.RehearsalAttendanceRepository;
import com.bandmate.rehearsal.repository.RehearsalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RehearsalService {

    private final RehearsalRepository rehearsalRepository;
    private final RehearsalAttendanceRepository attendanceRepository;
    private final BandRepository bandRepository;
    private final BandMemberRepository bandMemberRepository;

    // 일정 생성 (리더만)
    public RehearsalResponse createRehearsal(Long bandId, CreateRehearsalRequest request, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 합주 일정을 생성할 수 있습니다.");
        }

        Rehearsal rehearsal = Rehearsal.builder()
                .bandId(bandId)
                .title(request.getTitle())
                .description(request.getDescription())
                .rehearsalDate(request.getRehearsalDate())
                .location(request.getLocation())
                .maxCapacity(request.getMaxCapacity())
                .build();

        return RehearsalResponse.from(rehearsalRepository.save(rehearsal));
    }

    // 합주 일정 목록 조회
    @Transactional(readOnly = true)
    public List<RehearsalResponse> getRehearsals(Long bandId) {
        bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        return rehearsalRepository.findByBandId(bandId).stream()
                .map(RehearsalResponse::from)
                .collect(Collectors.toList());
    }

    // 합주 일정 단건 조회
    @Transactional(readOnly = true)
    public RehearsalResponse getRehearsal(Long bandId, Long rehearsalId) {
        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

        if (!rehearsal.getBandId().equals(bandId)) {
            throw new InvalidRequestException("해당 밴드의 일정이 아닙니다.");
        }

        return RehearsalResponse.from(rehearsal);
    }

    /**
     * 참여 신청 - 동시성 처리 핵심
     *
     * 여러 멤버가 동시에 마지막 자리에 신청하는 경우를 처리한다.
     * findByIdWithLock()이 SELECT FOR UPDATE를 발행해 rehearsal row를 잠근다.
     * 정원 체크와 currentCount 증가가 같은 트랜잭션 안에서 직렬화되므로
     * 정원 초과가 절대 발생하지 않는다.
     */
    public AttendanceResponse joinRehearsal(Long bandId, Long rehearsalId, Long userId) {
        // 비관적 락 획득 — 이 시점부터 다른 트랜잭션은 같은 row에 대해 대기
        Rehearsal rehearsal = rehearsalRepository.findByIdWithLock(rehearsalId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

        if (!rehearsal.getBandId().equals(bandId)) {
            throw new InvalidRequestException("해당 밴드의 일정이 아닙니다.");
        }

        // 밴드 멤버 여부 확인
        bandMemberRepository.findByBandIdAndUserId(bandId, userId)
                .orElseThrow(() -> new UnauthorizedException("밴드 멤버만 참여 신청할 수 있습니다."));

        // 중복 신청 확인
        attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)
                .ifPresent(a -> { throw new DuplicateException("이미 참여 신청했습니다."); });

        // 정원 확인 — 락을 잡은 상태에서 체크하므로 동시 요청이 있어도 안전
        if (rehearsal.getCurrentCount() >= rehearsal.getMaxCapacity()) {
            throw new InvalidRequestException("정원이 초과되었습니다.");
        }

        RehearsalAttendance attendance = RehearsalAttendance.builder()
                .rehearsalId(rehearsalId)
                .userId(userId)
                .build();
        attendanceRepository.save(attendance);

        rehearsal.setCurrentCount(rehearsal.getCurrentCount() + 1);

        return AttendanceResponse.from(attendance);
    }

    // 참여 신청 취소
    public void cancelAttendance(Long bandId, Long rehearsalId, Long userId) {
        Rehearsal rehearsal = rehearsalRepository.findByIdWithLock(rehearsalId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

        if (!rehearsal.getBandId().equals(bandId)) {
            throw new InvalidRequestException("해당 밴드의 일정이 아닙니다.");
        }

        RehearsalAttendance attendance = attendanceRepository.findByRehearsalIdAndUserId(rehearsalId, userId)
                .orElseThrow(() -> new NotFoundException("참여 신청 내역이 없습니다."));

        attendanceRepository.delete(attendance);
        rehearsal.setCurrentCount(rehearsal.getCurrentCount() - 1);
    }

    // 참여자 목록 조회 (리더만)
    @Transactional(readOnly = true)
    public List<AttendanceResponse> getAttendances(Long bandId, Long rehearsalId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 참여자 목록을 조회할 수 있습니다.");
        }

        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

        if (!rehearsal.getBandId().equals(bandId)) {
            throw new InvalidRequestException("해당 밴드의 일정이 아닙니다.");
        }

        return attendanceRepository.findByRehearsalId(rehearsalId).stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
    }
}
