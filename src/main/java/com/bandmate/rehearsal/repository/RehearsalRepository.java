package com.bandmate.rehearsal.repository;

import com.bandmate.rehearsal.entity.Rehearsal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RehearsalRepository extends JpaRepository<Rehearsal, Long> {

    List<Rehearsal> findByBandId(Long bandId);

    // 참여 신청 시 정원 체크 + 업데이트를 위한 비관적 락
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Rehearsal r WHERE r.id = :id")
    Optional<Rehearsal> findByIdWithLock(@Param("id") Long id);
}
