package com.bandmate.band.entity;

import com.bandmate.user.entity.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@SQLRestriction("deleted_at IS NULL")
@Table(
    name = "band",
    indexes = @Index(name = "idx_band_leader_id", columnList = "leader_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Band {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "leader_id", nullable = false)
    private Long leaderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leader_id", insertable = false, updatable = false)
    @ToString.Exclude
    private User leader;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "max_votes_per_person", columnDefinition = "INT DEFAULT 1")
    private Integer maxVotesPerPerson = 1;

    @Column
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}