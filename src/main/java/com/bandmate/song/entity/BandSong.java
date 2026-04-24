package com.bandmate.song.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "band_song")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandSong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long bandId;

    @Column(nullable = false)
    private Long songId;

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