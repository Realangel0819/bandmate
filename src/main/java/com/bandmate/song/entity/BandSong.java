package com.bandmate.song.entity;

import com.bandmate.band.entity.Band;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "band_song",
    indexes = {
        @Index(name = "idx_band_song_band_id", columnList = "band_id"),
        @Index(name = "idx_band_song_band_selected", columnList = "band_id, is_selected")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(name = "song_id", nullable = false)
    private Long songId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "song_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Song song;

    @Column(nullable = false)
    private LocalDateTime voteStartDate;

    @Column(nullable = false)
    private LocalDateTime voteEndDate;

    @Builder.Default
    @Column(nullable = false)
    private Integer voteCount = 0;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isSelected = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean isVotingActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(voteStartDate) && now.isBefore(voteEndDate);
    }

    public boolean isVotingEnded() {
        return LocalDateTime.now().isAfter(voteEndDate);
    }
}