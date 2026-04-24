package com.bandmate.band.service;

import com.bandmate.band.dto.*;
import com.bandmate.band.entity.*;
import com.bandmate.band.repository.*;
import com.bandmate.user.entity.User;
import com.bandmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * H2 in-memory DB를 사용하는 통합 테스트.
 * @Transactional → 각 테스트 후 자동 롤백되어 DB 격리 보장.
 */
@SpringBootTest
@Transactional
class BandIntegrationTest {

    @Autowired private BandService bandService;
    @Autowired private UserRepository userRepository;
    @Autowired private BandRepository bandRepository;
    @Autowired private BandMemberRepository bandMemberRepository;
    @Autowired private BandRecruitRepository bandRecruitRepository;
    @Autowired private BandApplicationRepository bandApplicationRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Long leaderId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        User leader = userRepository.save(User.builder()
                .email("leader_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("리더").build());
        leaderId = leader.getId();

        User member = userRepository.save(User.builder()
                .email("member_" + System.nanoTime() + "@test.com")
                .password(passwordEncoder.encode("pass"))
                .nickname("멤버").build());
        memberId = member.getId();
    }

    @Test
    @DisplayName("밴드 생성 후 DB에서 조회 가능")
    void createBand_persistsToDb() {
        CreateBandRequest request = new CreateBandRequest();
        request.setName("록밴드");
        request.setDescription("열정 있는 기타리스트 모집");

        BandResponse response = bandService.createBand(request, leaderId);

        assertThat(response.getId()).isNotNull();
        assertThat(response.getName()).isEqualTo("록밴드");
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
        assertThat(response.getMemberCount()).isEqualTo(1);

        // DB에 실제로 저장됐는지 확인
        assertThat(bandRepository.findById(response.getId())).isPresent();
        // 리더가 멤버로 등록됐는지 확인
        assertThat(bandMemberRepository.findByBandIdAndUserId(response.getId(), leaderId)).isPresent();
    }

    @Test
    @DisplayName("밴드 생성 → 모집 공고 등록 → 지원 → 승인 전체 플로우")
    void fullRecruitFlow_createApproveApplication() {
        // 밴드 생성
        CreateBandRequest bandReq = new CreateBandRequest();
        bandReq.setName("재즈밴드");
        bandReq.setDescription("재즈 좋아하는 사람");
        BandResponse band = bandService.createBand(bandReq, leaderId);
        Long bandId = band.getId();

        // 모집 공고 등록 (기타 2명)
        CreateRecruitRequest recruitReq = new CreateRecruitRequest();
        recruitReq.setBandId(bandId);
        recruitReq.setPosition(Position.GUITAR);
        recruitReq.setRequiredCount(2);
        RecruitResponse recruit = bandService.createRecruit(recruitReq, leaderId);

        assertThat(recruit.getPosition()).isEqualTo(Position.GUITAR);
        assertThat(recruit.getRequiredCount()).isEqualTo(2);
        assertThat(recruit.getCurrentCount()).isEqualTo(0);

        // 멤버 지원
        ApplyBandRequest applyReq = new ApplyBandRequest();
        applyReq.setRecruitId(recruit.getId());
        applyReq.setPosition(Position.GUITAR);
        ApplicationResponse application = bandService.applyBand(bandId, applyReq, memberId);

        assertThat(application.getStatus()).isEqualTo(BandApplication.ApplicationStatus.PENDING);

        // 리더가 승인
        ApplicationResponse approved = bandService.approveApplication(bandId, application.getId(), leaderId);

        assertThat(approved.getStatus()).isEqualTo(BandApplication.ApplicationStatus.APPROVED);

        // DB에서 멤버 추가 확인
        assertThat(bandMemberRepository.findByBandIdAndUserId(bandId, memberId)).isPresent();

        // 모집 공고 currentCount 증가 확인
        BandRecruit updatedRecruit = bandRecruitRepository.findById(recruit.getId()).orElseThrow();
        assertThat(updatedRecruit.getCurrentCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("포지션 정원 초과 시 지원 불가 (DB 검증)")
    void applyBand_capacityLimit_withDb() {
        CreateBandRequest bandReq = new CreateBandRequest();
        bandReq.setName("테스트밴드");
        BandResponse band = bandService.createBand(bandReq, leaderId);
        Long bandId = band.getId();

        // 정원 1명짜리 모집 공고
        CreateRecruitRequest recruitReq = new CreateRecruitRequest();
        recruitReq.setBandId(bandId);
        recruitReq.setPosition(Position.DRUM);
        recruitReq.setRequiredCount(1);
        RecruitResponse recruit = bandService.createRecruit(recruitReq, leaderId);

        // DB에 currentCount=1인 상태로 직접 설정 (정원 가득 참)
        BandRecruit recruitEntity = bandRecruitRepository.findById(recruit.getId()).orElseThrow();
        recruitEntity.setCurrentCount(1);
        bandRecruitRepository.save(recruitEntity);

        // 지원 시도 → 정원 초과 에러
        ApplyBandRequest applyReq = new ApplyBandRequest();
        applyReq.setRecruitId(recruit.getId());
        applyReq.setPosition(Position.DRUM);

        assertThatThrownBy(() -> bandService.applyBand(bandId, applyReq, memberId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 포지션의 정원이 다 찼습니다.");
    }

    @Test
    @DisplayName("같은 밴드 중복 지원 불가 (DB 검증)")
    void applyBand_duplicateCheck_withDb() {
        CreateBandRequest bandReq = new CreateBandRequest();
        bandReq.setName("중복테스트밴드");
        BandResponse band = bandService.createBand(bandReq, leaderId);
        Long bandId = band.getId();

        CreateRecruitRequest recruitReq = new CreateRecruitRequest();
        recruitReq.setBandId(bandId);
        recruitReq.setPosition(Position.BASS);
        recruitReq.setRequiredCount(2);
        RecruitResponse recruit = bandService.createRecruit(recruitReq, leaderId);

        ApplyBandRequest applyReq = new ApplyBandRequest();
        applyReq.setRecruitId(recruit.getId());
        applyReq.setPosition(Position.BASS);

        // 첫 번째 지원 성공
        bandService.applyBand(bandId, applyReq, memberId);

        // 두 번째 지원 실패
        assertThatThrownBy(() -> bandService.applyBand(bandId, applyReq, memberId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 이 밴드에 지원했습니다.");

        // DB에 지원서 1개만 존재하는지 확인
        List<BandApplication> apps = bandApplicationRepository.findByBandId(bandId);
        assertThat(apps).hasSize(1);
    }

    @Test
    @DisplayName("지원 거절 후 멤버로 등록되지 않음 (DB 검증)")
    void rejectApplication_doesNotAddMember_withDb() {
        CreateBandRequest bandReq = new CreateBandRequest();
        bandReq.setName("거절테스트밴드");
        BandResponse band = bandService.createBand(bandReq, leaderId);
        Long bandId = band.getId();

        CreateRecruitRequest recruitReq = new CreateRecruitRequest();
        recruitReq.setBandId(bandId);
        recruitReq.setPosition(Position.KEYBOARD);
        recruitReq.setRequiredCount(1);
        RecruitResponse recruit = bandService.createRecruit(recruitReq, leaderId);

        ApplyBandRequest applyReq = new ApplyBandRequest();
        applyReq.setRecruitId(recruit.getId());
        applyReq.setPosition(Position.KEYBOARD);
        ApplicationResponse application = bandService.applyBand(bandId, applyReq, memberId);

        // 거절
        ApplicationResponse rejected = bandService.rejectApplication(bandId, application.getId(), leaderId);

        assertThat(rejected.getStatus()).isEqualTo(BandApplication.ApplicationStatus.REJECTED);

        // 멤버로 등록되지 않았는지 확인
        assertThat(bandMemberRepository.findByBandIdAndUserId(bandId, memberId)).isEmpty();
    }

    @Test
    @DisplayName("같은 포지션으로 여러 모집 공고 등록 가능 (포지션 중복 허용)")
    void createRecruit_samePositionAllowed() {
        CreateBandRequest bandReq = new CreateBandRequest();
        bandReq.setName("포지션중복테스트밴드");
        BandResponse band = bandService.createBand(bandReq, leaderId);
        Long bandId = band.getId();

        CreateRecruitRequest req1 = new CreateRecruitRequest();
        req1.setBandId(bandId);
        req1.setPosition(Position.GUITAR);
        req1.setRequiredCount(1);

        CreateRecruitRequest req2 = new CreateRecruitRequest();
        req2.setBandId(bandId);
        req2.setPosition(Position.GUITAR);
        req2.setRequiredCount(1);

        RecruitResponse r1 = bandService.createRecruit(req1, leaderId);
        RecruitResponse r2 = bandService.createRecruit(req2, leaderId);

        assertThat(r1.getId()).isNotEqualTo(r2.getId());
        assertThat(bandRecruitRepository.findByBandId(bandId)).hasSize(2);
    }
}
