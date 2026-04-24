package com.bandmate.rehearsal.entity;

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
    name = "rehearsal",
    indexes = {
        @Index(name = "idx_rehearsal_band_id", columnList = "band_id"),
        @Index(name = "idx_rehearsal_date", columnList = "rehearsal_date")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rehearsal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime rehearsalDate;

    @Column
    private String location;

    @Column(nullable = false)
    private int maxCapacity;

    @Column(nullable = false)
    @Builder.Default
    private int currentCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Band band;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
