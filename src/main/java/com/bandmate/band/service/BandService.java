package com.bandmate.band.service;

import com.bandmate.band.dto.*;
import com.bandmate.band.entity.*;
import com.bandmate.band.repository.*;
import com.bandmate.common.exception.DuplicateException;
import com.bandmate.common.exception.InvalidRequestException;
import com.bandmate.common.exception.NotFoundException;
import com.bandmate.common.exception.UnauthorizedException;
import com.bandmate.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BandService {

    private final BandRepository bandRepository;
    private final BandRecruitRepository bandRecruitRepository;
    private final BandApplicationRepository bandApplicationRepository;
    private final BandMemberRepository bandMemberRepository;
    private final UserRepository userRepository;

    // 전체 밴드 목록 조회
    @Transactional(readOnly = true)
    public List<BandResponse> getAllBands() {
        return bandRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(band -> new BandResponse(
                        band.getId(),
                        band.getName(),
                        band.getDescription(),
                        band.getLeaderId(),
                        bandMemberRepository.countByBandId(band.getId()),
                        band.getCreatedAt(),
                        band.getMaxVotesPerPerson()
                ))
                .collect(Collectors.toList());
    }

    // 밴드 생성
    public BandResponse createBand(CreateBandRequest request, Long leaderId) {
        Band band = Band.builder()
                .name(request.getName())
                .description(request.getDescription())
                .leaderId(leaderId)
                .build();

        Band savedBand = bandRepository.save(band);

        BandMember leader = BandMember.builder()
                .bandId(savedBand.getId())
                .userId(leaderId)
                .position(Position.VOCAL)
                .build();
        bandMemberRepository.save(leader);

        return new BandResponse(
                savedBand.getId(),
                savedBand.getName(),
                savedBand.getDescription(),
                savedBand.getLeaderId(),
                1,
                savedBand.getCreatedAt(),
                savedBand.getMaxVotesPerPerson()
        );
    }

    // 밴드 조회
    @Transactional(readOnly = true)
    public BandResponse getBand(Long bandId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        int memberCount = bandMemberRepository.countByBandId(bandId);

        return new BandResponse(
                band.getId(),
                band.getName(),
                band.getDescription(),
                band.getLeaderId(),
                memberCount,
                band.getCreatedAt(),
                band.getMaxVotesPerPerson()
        );
    }

    // 내가 속한 모든 밴드 조회 (리더 + 멤버)
    @Transactional(readOnly = true)
    public List<BandResponse> getMyBands(Long userId) {
        return bandMemberRepository.findByUserId(userId).stream()
                .map(m -> bandRepository.findById(m.getBandId()).orElse(null))
                .filter(band -> band != null)
                .map(band -> new BandResponse(
                        band.getId(),
                        band.getName(),
                        band.getDescription(),
                        band.getLeaderId(),
                        bandMemberRepository.countByBandId(band.getId()),
                        band.getCreatedAt(),
                        band.getMaxVotesPerPerson()
                ))
                .collect(Collectors.toList());
    }

    // 멤버 탈퇴 / 강퇴
    public void removeMember(Long bandId, Long memberId, Long requesterId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        BandMember target = bandMemberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException("멤버를 찾을 수 없습니다."));

        if (!target.getBandId().equals(bandId)) {
            throw new InvalidRequestException("해당 밴드의 멤버가 아닙니다.");
        }

        // 리더는 탈퇴 불가 (밴드 삭제로 처리)
        if (target.getUserId().equals(band.getLeaderId())) {
            throw new InvalidRequestException("리더는 탈퇴할 수 없습니다. 밴드를 삭제해주세요.");
        }

        boolean isLeader = band.getLeaderId().equals(requesterId);
        boolean isSelf = target.getUserId().equals(requesterId);

        if (!isLeader && !isSelf) {
            throw new UnauthorizedException("권한이 없습니다.");
        }

        bandMemberRepository.delete(target);
    }

    // 투표 설정 업데이트 (리더만)
    public BandResponse updateVoteSettings(Long bandId, UpdateVoteSettingsRequest request, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 투표 설정을 변경할 수 있습니다.");
        }

        band.setMaxVotesPerPerson(request.getMaxVotesPerPerson());
        Band saved = bandRepository.save(band);

        return new BandResponse(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getLeaderId(),
                bandMemberRepository.countByBandId(bandId),
                saved.getCreatedAt(),
                saved.getMaxVotesPerPerson()
        );
    }

    // 밴드 멤버 목록 조회
    @Transactional(readOnly = true)
    public List<BandMemberResponse> getBandMembers(Long bandId) {
        bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));
        return bandMemberRepository.findByBandId(bandId).stream()
                .map(m -> new BandMemberResponse(
                        m.getId(),
                        m.getUserId(),
                        m.getUser() != null ? m.getUser().getNickname() : "알 수 없음",
                        m.getPosition() != null ? m.getPosition().name() : null,
                        m.getJoinedAt()
                ))
                .collect(Collectors.toList());
    }

    // 모집 정보 추가
    public RecruitResponse createRecruit(CreateRecruitRequest request, Long leaderId) {
        Band band = bandRepository.findById(request.getBandId())
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 모집 정보를 추가할 수 있습니다.");
        }

        BandRecruit recruit = BandRecruit.builder()
                .bandId(request.getBandId())
                .position(request.getPosition())
                .requiredCount(request.getRequiredCount())
                .build();

        BandRecruit savedRecruit = bandRecruitRepository.save(recruit);

        return new RecruitResponse(
                savedRecruit.getId(),
                savedRecruit.getBandId(),
                savedRecruit.getPosition(),
                savedRecruit.getRequiredCount(),
                savedRecruit.getCurrentCount(),
                savedRecruit.getCreatedAt()
        );
    }

    // 지원
    public ApplicationResponse applyBand(Long bandId, ApplyBandRequest request, Long userId) {
        bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        BandRecruit recruit = bandRecruitRepository.findById(request.getRecruitId())
                .orElseThrow(() -> new NotFoundException("모집 정보를 찾을 수 없습니다."));

        if (recruit.getCurrentCount() >= recruit.getRequiredCount()) {
            throw new InvalidRequestException("해당 포지션의 정원이 다 찼습니다.");
        }

        bandApplicationRepository.findByBandIdAndUserId(bandId, userId)
                .ifPresent(app -> { throw new DuplicateException("이미 이 밴드에 지원했습니다."); });

        bandMemberRepository.findByBandIdAndUserId(bandId, userId)
                .ifPresent(member -> { throw new DuplicateException("이미 이 밴드의 멤버입니다."); });

        BandApplication application = BandApplication.builder()
                .bandId(bandId)
                .userId(userId)
                .recruitId(request.getRecruitId())
                .position(request.getPosition())
                .introduction(request.getIntroduction())
                .status(BandApplication.ApplicationStatus.PENDING)
                .build();

        BandApplication savedApplication = bandApplicationRepository.save(application);
        String applyNickname = userRepository.findById(userId)
                .map(u -> u.getNickname()).orElse("알 수 없음");

        return new ApplicationResponse(
                savedApplication.getId(),
                savedApplication.getBandId(),
                savedApplication.getUserId(),
                applyNickname,
                savedApplication.getPosition(),
                savedApplication.getStatus(),
                savedApplication.getIntroduction(),
                savedApplication.getCreatedAt()
        );
    }

    // 지원 승인
    public ApplicationResponse approveApplication(Long bandId, Long applicationId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 지원을 승인할 수 있습니다.");
        }

        BandApplication application = bandApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("지원 신청을 찾을 수 없습니다."));

        BandRecruit recruit = bandRecruitRepository.findById(application.getRecruitId())
                .orElseThrow(() -> new NotFoundException("모집 정보를 찾을 수 없습니다."));

        if (recruit.getCurrentCount() >= recruit.getRequiredCount()) {
            throw new InvalidRequestException("해당 포지션의 정원이 다 찼습니다.");
        }

        application.setStatus(BandApplication.ApplicationStatus.APPROVED);
        bandApplicationRepository.save(application);

        BandMember member = BandMember.builder()
                .bandId(bandId)
                .userId(application.getUserId())
                .position(application.getPosition())
                .build();
        bandMemberRepository.save(member);

        recruit.setCurrentCount(recruit.getCurrentCount() + 1);
        bandRecruitRepository.save(recruit);

        String approveNickname = application.getUser() != null
                ? application.getUser().getNickname()
                : userRepository.findById(application.getUserId()).map(u -> u.getNickname()).orElse("알 수 없음");

        return new ApplicationResponse(
                application.getId(),
                application.getBandId(),
                application.getUserId(),
                approveNickname,
                application.getPosition(),
                application.getStatus(),
                application.getIntroduction(),
                application.getCreatedAt()
        );
    }

    // 지원 거절
    public ApplicationResponse rejectApplication(Long bandId, Long applicationId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 지원을 거절할 수 있습니다.");
        }

        BandApplication application = bandApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new NotFoundException("지원 신청을 찾을 수 없습니다."));

        application.setStatus(BandApplication.ApplicationStatus.REJECTED);
        bandApplicationRepository.save(application);

        String rejectNickname = application.getUser() != null
                ? application.getUser().getNickname()
                : userRepository.findById(application.getUserId()).map(u -> u.getNickname()).orElse("알 수 없음");

        return new ApplicationResponse(
                application.getId(),
                application.getBandId(),
                application.getUserId(),
                rejectNickname,
                application.getPosition(),
                application.getStatus(),
                application.getIntroduction(),
                application.getCreatedAt()
        );
    }

    // 밴드 삭제 (soft delete)
    public void deleteBand(Long bandId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 밴드를 삭제할 수 있습니다.");
        }

        band.softDelete();
        bandRepository.save(band);
    }

    // 밴드 모집 공고 목록 조회
    @Transactional(readOnly = true)
    public List<RecruitResponse> getBandRecruits(Long bandId) {
        bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));
        return bandRecruitRepository.findByBandId(bandId).stream()
                .map(r -> new RecruitResponse(
                        r.getId(),
                        r.getBandId(),
                        r.getPosition(),
                        r.getRequiredCount(),
                        r.getCurrentCount(),
                        r.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 밴드의 모든 지원 조회
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getBandApplications(Long bandId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new NotFoundException("밴드를 찾을 수 없습니다."));

        if (!band.getLeaderId().equals(leaderId)) {
            throw new UnauthorizedException("리더만 지원 목록을 조회할 수 있습니다.");
        }

        return bandApplicationRepository.findByBandId(bandId).stream()
                .map(app -> {
                    String nick = app.getUser() != null
                            ? app.getUser().getNickname()
                            : userRepository.findById(app.getUserId()).map(u -> u.getNickname()).orElse("알 수 없음");
                    return new ApplicationResponse(
                            app.getId(),
                            app.getBandId(),
                            app.getUserId(),
                            nick,
                            app.getPosition(),
                            app.getStatus(),
                            app.getIntroduction(),
                            app.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}
