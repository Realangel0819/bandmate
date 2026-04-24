package com.bandmate.band.repository;

import com.bandmate.band.entity.BandRecruit;
import com.bandmate.band.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BandRecruitRepository extends JpaRepository<BandRecruit, Long> {
    List<BandRecruit> findByBandId(Long bandId);
    Optional<BandRecruit> findByBandIdAndPosition(Long bandId, Position position);
}