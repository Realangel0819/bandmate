package com.bandmate.band.service;

import com.bandmate.band.dto.*;
import com.bandmate.band.entity.*;
import com.bandmate.band.repository.*;
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

    // 밴드 생성
    public BandResponse createBand(CreateBandRequest request, Long leaderId) {
        Band band = Band.builder()
                .name(request.getName())
                .description(request.getDescription())
                .leaderId(leaderId)
                .build();

        Band savedBand = bandRepository.save(band);
        
        // 리더를 밴드 멤버로 추가
        BandMember leader = BandMember.builder()
                .bandId(savedBand.getId())
                .userId(leaderId)
                .position(Position.VOCAL) // 기본 포지션
                .build();
        bandMemberRepository.save(leader);

        return new BandResponse(
                savedBand.getId(),
                savedBand.getName(),
                savedBand.getDescription(),
                savedBand.getLeaderId(),
                1, // 리더 1명
                savedBand.getCreatedAt()
        );
    }

    // 밴드 조회
    public BandResponse getBand(Long bandId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        int memberCount = bandMemberRepository.countByBandId(bandId);

        return new BandResponse(
                band.getId(),
                band.getName(),
                band.getDescription(),
                band.getLeaderId(),
                memberCount,
                band.getCreatedAt()
        );
    }

    // 리더의 모든 밴드 조회
    public List<BandResponse> getLeaderBands(Long leaderId) {
        return bandRepository.findByLeaderId(leaderId).stream()
                .map(band -> new BandResponse(
                        band.getId(),
                        band.getName(),
                        band.getDescription(),
                        band.getLeaderId(),
                        bandMemberRepository.countByBandId(band.getId()),
                        band.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    // 모집 정보 추가
    public RecruitResponse createRecruit(CreateRecruitRequest request, Long leaderId) {
        Band band = bandRepository.findById(request.getBandId())
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        // 리더 확인
        if (!band.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("리더만 모집 정보를 추가할 수 있습니다.");
        }

        // 같은 포지션 중복 확인
        bandRecruitRepository.findByBandIdAndPosition(request.getBandId(), request.getPosition())
                .ifPresent(r -> { throw new RuntimeException("이미 해당 포지션의 모집이 진행 중입니다."); });

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
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        BandRecruit recruit = bandRecruitRepository.findById(request.getRecruitId())
                .orElseThrow(() -> new RuntimeException("모집 정보를 찾을 수 없습니다."));

        // 정원 초과 확인
        if (recruit.getCurrentCount() >= recruit.getRequiredCount()) {
            throw new RuntimeException("해당 포지션의 정원이 다 찼습니다.");
        }

        // 중복 지원 확인
        bandApplicationRepository.findByBandIdAndUserId(bandId, userId)
                .ifPresent(app -> { throw new RuntimeException("이미 이 밴드에 지원했습니다."); });

        // 이미 멤버인 경우 확인
        bandMemberRepository.findByBandIdAndUserId(bandId, userId)
                .ifPresent(member -> { throw new RuntimeException("이미 이 밴드의 멤버입니다."); });

        BandApplication application = BandApplication.builder()
                .bandId(bandId)
                .userId(userId)
                .recruitId(request.getRecruitId())
                .position(request.getPosition())
                .status(BandApplication.ApplicationStatus.PENDING)
                .build();

        BandApplication savedApplication = bandApplicationRepository.save(application);

        return new ApplicationResponse(
                savedApplication.getId(),
                savedApplication.getBandId(),
                savedApplication.getUserId(),
                savedApplication.getPosition(),
                savedApplication.getStatus(),
                savedApplication.getCreatedAt()
        );
    }

    // 지원 승인
    public ApplicationResponse approveApplication(Long bandId, Long applicationId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        // 리더 확인
        if (!band.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("리더만 지원을 승인할 수 있습니다.");
        }

        BandApplication application = bandApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("지원 신청을 찾을 수 없습니다."));

        BandRecruit recruit = bandRecruitRepository.findById(application.getRecruitId())
                .orElseThrow(() -> new RuntimeException("모집 정보를 찾을 수 없습니다."));

        // 정원 초과 확인
        if (recruit.getCurrentCount() >= recruit.getRequiredCount()) {
            throw new RuntimeException("해당 포지션의 정원이 다 찼습니다.");
        }

        application.setStatus(BandApplication.ApplicationStatus.APPROVED);
        bandApplicationRepository.save(application);

        // 밴드 멤버 추가
        BandMember member = BandMember.builder()
                .bandId(bandId)
                .userId(application.getUserId())
                .position(application.getPosition())
                .build();
        bandMemberRepository.save(member);

        // 모집 정보 업데이트
        recruit.setCurrentCount(recruit.getCurrentCount() + 1);
        bandRecruitRepository.save(recruit);

        return new ApplicationResponse(
                application.getId(),
                application.getBandId(),
                application.getUserId(),
                application.getPosition(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    // 지원 거절
    public ApplicationResponse rejectApplication(Long bandId, Long applicationId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        // 리더 확인
        if (!band.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("리더만 지원을 거절할 수 있습니다.");
        }

        BandApplication application = bandApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("지원 신청을 찾을 수 없습니다."));

        application.setStatus(BandApplication.ApplicationStatus.REJECTED);
        bandApplicationRepository.save(application);

        return new ApplicationResponse(
                application.getId(),
                application.getBandId(),
                application.getUserId(),
                application.getPosition(),
                application.getStatus(),
                application.getCreatedAt()
        );
    }

    // 밴드의 모든 지원 조회
    public List<ApplicationResponse> getBandApplications(Long bandId, Long leaderId) {
        Band band = bandRepository.findById(bandId)
                .orElseThrow(() -> new RuntimeException("밴드를 찾을 수 없습니다."));

        // 리더 확인
        if (!band.getLeaderId().equals(leaderId)) {
            throw new RuntimeException("리더만 지원 목록을 조회할 수 있습니다.");
        }

        return bandApplicationRepository.findByBandId(bandId).stream()
                .map(app -> new ApplicationResponse(
                        app.getId(),
                        app.getBandId(),
                        app.getUserId(),
                        app.getPosition(),
                        app.getStatus(),
                        app.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}