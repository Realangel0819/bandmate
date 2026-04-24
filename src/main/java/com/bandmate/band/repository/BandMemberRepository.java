package com.bandmate.band.repository;

import com.bandmate.band.entity.BandMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BandMemberRepository extends JpaRepository<BandMember, Long> {
    List<BandMember> findByBandId(Long bandId);
    List<BandMember> findByUserId(Long userId);
    Optional<BandMember> findByBandIdAndUserId(Long bandId, Long userId);
    int countByBandId(Long bandId);
}