package com.bandmate.song.repository;

import com.bandmate.song.entity.BandSong;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BandSongRepository extends JpaRepository<BandSong, Long> {
    List<BandSong> findByBandId(Long bandId);
    Optional<BandSong> findByBandIdAndSongId(Long bandId, Long songId);
    
    @Query("SELECT bs FROM BandSong bs WHERE bs.bandId = :bandId AND bs.isSelected = false ORDER BY bs.voteEndDate DESC")
    List<BandSong> findActiveCandidates(Long bandId);

    @Query("SELECT bs FROM BandSong bs WHERE bs.bandId = :bandId AND bs.isSelected = true ORDER BY bs.createdAt DESC LIMIT 1")
    Optional<BandSong> findSelectedSong(Long bandId);

    @Query("SELECT bs FROM BandSong bs WHERE bs.bandId = :bandId AND bs.isSelected = true ORDER BY bs.voteCount DESC")
    List<BandSong> findSelectedSongs(Long bandId);

    void deleteByBandId(Long bandId);
}