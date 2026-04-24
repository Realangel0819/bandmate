package com.bandmate.rehearsal.entity;

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
    name = "rehearsal_attendance",
    uniqueConstraints = @UniqueConstraint(name = "uk_rehearsal_attendance", columnNames = {"rehearsal_id", "user_id"}),
    indexes = @Index(name = "idx_rehearsal_attendance_rehearsal_id", columnList = "rehearsal_id")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RehearsalAttendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rehearsal_id", nullable = false)
    private Long rehearsalId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rehearsal_id", insertable = false, updatable = false)
    @ToString.Exclude
    private Rehearsal rehearsal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @ToString.Exclude
    private User user;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
