package com.bandmate.band.service;

import com.bandmate.band.dto.*;
import com.bandmate.band.entity.*;
import com.bandmate.band.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BandServiceTest {

    @Mock
    private BandRepository bandRepository;

    @Mock
    private BandRecruitRepository bandRecruitRepository;

    @Mock
    private BandApplicationRepository bandApplicationRepository;

    @Mock
    private BandMemberRepository bandMemberRepository;

    @InjectMocks
    private BandService bandService;

    @Test
    @DisplayName("밴드 생성 성공")
    void createBand_Success() {
        // given
        CreateBandRequest request = new CreateBandRequest();
        request.setName("Test Band");
        request.setDescription("Test Description");
        Long leaderId = 1L;

        Band savedBand = Band.builder()
                .id(1L)
                .name("Test Band")
                .description("Test Description")
                .leaderId(leaderId)
                .build();

        given(bandRepository.save(any(Band.class))).willReturn(savedBand);
        given(bandMemberRepository.save(any(BandMember.class))).willReturn(new BandMember());

        // when
        BandResponse response = bandService.createBand(request, leaderId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo("Test Band");
        assertThat(response.getLeaderId()).isEqualTo(leaderId);
        verify(bandRepository, times(1)).save(any(Band.class));
    }

    @Test
    @DisplayName("지원 시 정원 제한 체크")
    void applyBand_CheckCapacityLimit() {
        // given
        Long bandId = 1L;
        Long userId = 2L;
        Long recruitId = 1L;

        Band band = Band.builder().id(bandId).leaderId(1L).build();
        BandRecruit recruit = BandRecruit.builder()
                .id(recruitId)
                .bandId(bandId)
                .position(Position.VOCAL)
                .requiredCount(1)
                .currentCount(1) // 정원 가득찬 상태
                .build();

        ApplyBandRequest request = new ApplyBandRequest();
        request.setRecruitId(recruitId);
        request.setPosition(Position.VOCAL);

        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(bandRecruitRepository.findById(recruitId)).willReturn(Optional.of(recruit));

        // when & then
        assertThatThrownBy(() -> bandService.applyBand(bandId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("해당 포지션의 정원이 다 찼습니다.");
    }

    @Test
    @DisplayName("중복 지원 방지")
    void applyBand_PreventDuplicateApplication() {
        // given
        Long bandId = 1L;
        Long userId = 2L;
        Long recruitId = 1L;

        Band band = Band.builder().id(bandId).leaderId(1L).build();
        BandRecruit recruit = BandRecruit.builder()
                .id(recruitId)
                .bandId(bandId)
                .position(Position.VOCAL)
                .requiredCount(2)
                .currentCount(0)
                .build();

        BandApplication existingApp = BandApplication.builder()
                .bandId(bandId)
                .userId(userId)
                .build();

        ApplyBandRequest request = new ApplyBandRequest();
        request.setRecruitId(recruitId);
        request.setPosition(Position.VOCAL);

        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(bandRecruitRepository.findById(recruitId)).willReturn(Optional.of(recruit));
        given(bandApplicationRepository.findByBandIdAndUserId(bandId, userId))
                .willReturn(Optional.of(existingApp));

        // when & then
        assertThatThrownBy(() -> bandService.applyBand(bandId, request, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 이 밴드에 지원했습니다.");
    }

    @Test
    @DisplayName("지원 승인 성공")
    void approveApplication_Success() {
        // given
        Long bandId = 1L;
        Long applicationId = 1L;
        Long userId = 1L;
        Long recruitId = 1L;

        Band band = Band.builder().id(bandId).leaderId(userId).build();
        BandApplication application = BandApplication.builder()
                .id(applicationId)
                .bandId(bandId)
                .userId(2L)
                .recruitId(recruitId)
                .position(Position.VOCAL)
                .status(BandApplication.ApplicationStatus.PENDING)
                .build();

        BandRecruit recruit = BandRecruit.builder()
                .id(recruitId)
                .bandId(bandId)
                .position(Position.VOCAL)
                .requiredCount(2)
                .currentCount(0)
                .build();

        given(bandRepository.findById(bandId)).willReturn(Optional.of(band));
        given(bandApplicationRepository.findById(applicationId)).willReturn(Optional.of(application));
        given(bandRecruitRepository.findById(recruitId)).willReturn(Optional.of(recruit));
        given(bandApplicationRepository.save(any(BandApplication.class))).willReturn(application);
        given(bandMemberRepository.save(any(BandMember.class))).willReturn(new BandMember());
        given(bandRecruitRepository.save(any(BandRecruit.class))).willReturn(recruit);

        // when
        ApplicationResponse response = bandService.approveApplication(bandId, applicationId, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(BandApplication.ApplicationStatus.APPROVED);
        verify(bandApplicationRepository, times(1)).save(any(BandApplication.class));
        verify(bandMemberRepository, times(1)).save(any(BandMember.class));
    }
}