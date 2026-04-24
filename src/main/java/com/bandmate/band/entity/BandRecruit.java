package com.bandmate.band.entity;

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
    name = "band_recruit",
    indexes = @Index(name = "idx_band_recruit_band_id", columnList = "band_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandRecruit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Band band;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Column(nullable = false)
    private Integer requiredCount;

    @Builder.Default
    @Column(nullable = false)
    private Integer currentCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}