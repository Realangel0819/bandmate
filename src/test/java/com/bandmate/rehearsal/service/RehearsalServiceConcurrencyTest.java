package com.bandmate.rehearsal.service;

import com.bandmate.band.entity.Band;
import com.bandmate.band.entity.BandMember;
import com.bandmate.band.entity.Position;
import com.bandmate.band.repository.BandMemberRepository;
import com.bandmate.band.repository.BandRepository;
import com.bandmate.rehearsal.dto.CreateRehearsalRequest;
import com.bandmate.rehearsal.entity.Rehearsal;
import com.bandmate.rehearsal.repository.RehearsalRepository;
import com.bandmate.user.entity.User;
import com.bandmate.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RehearsalServiceConcurrencyTest {

    @Autowired
    private RehearsalService rehearsalService;

    @Autowired
    private RehearsalRepository rehearsalRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BandRepository bandRepository;

    @Autowired
    private BandMemberRepository bandMemberRepository;

    private Long bandId;
    private Long rehearsalId;
    private Long leaderId;
    private final List<Long> memberUserIds = new ArrayList<>();

    private static final int MAX_CAPACITY = 5;
    private static final int THREAD_COUNT = 10;

    @BeforeEach
    void setUp() {
        // 리더 생성
        User leader = userRepository.save(User.builder()
                .email("leader_" + System.nanoTime() + "@test.com")
                .password("pass")
                .nickname("리더_" + System.nanoTime())
                .build());
        leaderId = leader.getId();

        // 밴드 생성
        Band band = bandRepository.save(Band.builder()
                .name("TestBand")
                .description("테스트 밴드")
                .leaderId(leaderId)
                .build());
        bandId = band.getId();

        // 리더를 밴드 멤버로 추가
        bandMemberRepository.save(BandMember.builder()
                .bandId(bandId)
                .userId(leaderId)
                .position(Position.VOCAL)
                .build());

        // 일반 멤버 9명 생성
        for (int i = 0; i < THREAD_COUNT - 1; i++) {
            User member = userRepository.save(User.builder()
                    .email("member_" + System.nanoTime() + "_" + i + "@test.com")
                    .password("pass")
                    .nickname("멤버_" + System.nanoTime() + "_" + i)
                    .build());
            bandMemberRepository.save(BandMember.builder()
                    .bandId(bandId)
                    .userId(member.getId())
                    .position(Position.GUITAR)
                    .build());
            memberUserIds.add(member.getId());
        }

        // 합주 일정 생성 (정원 5명)
        CreateRehearsalRequest request = new CreateRehearsalRequest(
                "정기 합주",
                "주간 합주 일정",
                LocalDateTime.now().plusDays(7),
                "홍대 연습실",
                MAX_CAPACITY
        );
        rehearsalId = rehearsalService.createRehearsal(bandId, request, leaderId).getRehearsalId();
    }

    @Test
    @DisplayName("정원 5명인 합주에 10명이 동시에 신청하면 정확히 5명만 성공해야 한다")
    void joinRehearsal_whenConcurrent_shouldNotExceedCapacity() throws InterruptedException {
        // 리더 포함 전체 참여자 (리더 1 + 멤버 9 = 10명)
        List<Long> allUserIds = new ArrayList<>();
        allUserIds.add(leaderId);
        allUserIds.addAll(memberUserIds);

        CountDownLatch startLatch = new CountDownLatch(1);   // 동시 출발 신호
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            final Long userId = allUserIds.get(i);
            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 준비될 때까지 대기
                    rehearsalService.joinRehearsal(bandId, rehearsalId, userId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 동시 실행 시작
        doneLatch.await();
        executor.shutdown();

        // 검증
        assertThat(successCount.get()).isEqualTo(MAX_CAPACITY);
        assertThat(failCount.get()).isEqualTo(THREAD_COUNT - MAX_CAPACITY);

        // DB currentCount가 정확히 5인지 확인
        Rehearsal rehearsal = rehearsalRepository.findById(rehearsalId).orElseThrow();
        assertThat(rehearsal.getCurrentCount()).isEqualTo(MAX_CAPACITY);
    }
}
