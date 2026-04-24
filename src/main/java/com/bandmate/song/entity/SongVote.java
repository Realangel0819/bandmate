package com.bandmate.song.entity;

import com.bandmate.band.entity.Band;
import com.bandmate.user.entity.User;
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
    name = "song_vote",
    uniqueConstraints = @UniqueConstraint(name = "uk_song_vote", columnNames = {"band_song_id", "user_id"}),
    indexes = {
        @Index(name = "idx_song_vote_band_song_id", columnList = "band_song_id"),
        @Index(name = "idx_song_vote_band_user", columnList = "band_id, user_id"),
        @Index(name = "idx_song_vote_band_id", columnList = "band_id")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SongVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_song_id", nullable = false)
    private Long bandSongId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_song_id", insertable = false, updatable = false)
    @ToString.Exclude
    private BandSong bandSong;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Band band;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime votedAt;
}
