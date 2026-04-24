package com.bandmate.band.repository;

import com.bandmate.band.entity.BandApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BandApplicationRepository extends JpaRepository<BandApplication, Long> {
    List<BandApplication> findByBandId(Long bandId);
    List<BandApplication> findByUserId(Long userId);
    Optional<BandApplication> findByBandIdAndUserId(Long bandId, Long userId);
    int countByRecruitIdAndStatus(Long recruitId, BandApplication.ApplicationStatus status);
}