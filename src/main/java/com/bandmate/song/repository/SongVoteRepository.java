package com.bandmate.song.repository;

import com.bandmate.song.entity.SongVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SongVoteRepository extends JpaRepository<SongVote, Long> {
    Optional<SongVote> findByBandSongIdAndUserId(Long bandSongId, Long userId);
    int countByBandSongId(Long bandSongId);
    int countByBandIdAndUserId(Long bandId, Long userId);
    int countByBandId(Long bandId);
}