package com.bandmate.rehearsal.repository;

import com.bandmate.rehearsal.entity.RehearsalAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RehearsalAttendanceRepository extends JpaRepository<RehearsalAttendance, Long> {

    Optional<RehearsalAttendance> findByRehearsalIdAndUserId(Long rehearsalId, Long userId);

    List<RehearsalAttendance> findByRehearsalId(Long rehearsalId);

    int countByRehearsalId(Long rehearsalId);
}
