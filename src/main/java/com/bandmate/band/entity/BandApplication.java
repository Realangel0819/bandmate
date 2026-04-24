package com.bandmate.band.entity;

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
    name = "band_application",
    uniqueConstraints = @UniqueConstraint(name = "uk_band_application", columnNames = {"band_id", "user_id"}),
    indexes = {
        @Index(name = "idx_band_application_band_id", columnList = "band_id"),
        @Index(name = "idx_band_application_user_id", columnList = "user_id"),
        @Index(name = "idx_band_application_recruit_status", columnList = "recruit_id, status")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BandApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "band_id", nullable = false)
    private Long bandId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "recruit_id", nullable = false)
    private Long recruitId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "band_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Band band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruit_id", insertable = false, updatable = false)
    @ToString.Exclude
    private BandRecruit recruit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Position position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum ApplicationStatus {
        PENDING("대기중"),
        APPROVED("승인됨"),
        REJECTED("거절됨");

        private final String description;

        ApplicationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}