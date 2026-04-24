-- ================================================================
-- BandMate 데이터베이스 스키마 (MySQL 8.0+)
-- ================================================================
-- 실행 순서: FK 의존 순서에 맞게 작성되어 있음
-- 운영 적용: mysql -u {user} -p bandmatedb < schema.sql
-- ================================================================

CREATE DATABASE IF NOT EXISTS bandmatedb
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE bandmatedb;

-- ----------------------------------------------------------------
-- 1. users
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_users_email     (email),
    UNIQUE KEY uk_users_nickname  (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 2. band
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS band (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    leader_id   BIGINT       NOT NULL,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at  DATETIME(6)  NULL     DEFAULT NULL,

    PRIMARY KEY (id),

    -- 리더 조회 (내 밴드 목록)
    INDEX idx_band_leader_id (leader_id),

    CONSTRAINT fk_band_leader FOREIGN KEY (leader_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 3. band_member
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS band_member (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    band_id   BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    position  VARCHAR(20) NOT NULL,       -- VOCAL | GUITAR | BASS | DRUM | KEYBOARD | PERCUSSION
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- 중복 멤버십 방지: 한 유저는 밴드당 1개 멤버십만
    UNIQUE KEY uk_band_member (band_id, user_id),

    INDEX idx_band_member_band_id (band_id),
    INDEX idx_band_member_user_id (user_id),

    CONSTRAINT fk_band_member_band FOREIGN KEY (band_id) REFERENCES band (id),
    CONSTRAINT fk_band_member_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 4. band_recruit  (포지션 중복 허용 — unique 제약 없음)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS band_recruit (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    band_id        BIGINT      NOT NULL,
    position       VARCHAR(20) NOT NULL,
    required_count INT         NOT NULL,
    current_count  INT         NOT NULL DEFAULT 0,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- 밴드별 모집 공고 조회
    INDEX idx_band_recruit_band_id (band_id),

    CONSTRAINT fk_band_recruit_band FOREIGN KEY (band_id) REFERENCES band (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 5. band_application
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS band_application (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    band_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    recruit_id BIGINT      NOT NULL,
    position   VARCHAR(20) NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',   -- PENDING | APPROVED | REJECTED
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- ★ 중복 지원 방지: 같은 밴드에 동일 유저는 지원서 1개만
    UNIQUE KEY uk_band_application (band_id, user_id),

    INDEX idx_band_application_band_id (band_id),
    INDEX idx_band_application_user_id (user_id),
    -- 승인 인원 집계 쿼리 최적화 (countByRecruitIdAndStatus)
    INDEX idx_band_application_recruit_status (recruit_id, status),

    CONSTRAINT fk_band_application_band    FOREIGN KEY (band_id)    REFERENCES band (id),
    CONSTRAINT fk_band_application_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_band_application_recruit FOREIGN KEY (recruit_id) REFERENCES band_recruit (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 6. song  (글로벌 곡 카탈로그)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS song (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    artist      VARCHAR(255) NOT NULL,
    youtube_url TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    UNIQUE KEY uk_song_title_artist (title, artist)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 7. band_song  (밴드별 후보곡 + 투표 정보)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS band_song (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    band_id         BIGINT      NOT NULL,
    song_id         BIGINT      NOT NULL,
    vote_start_date DATETIME(6) NOT NULL,
    vote_end_date   DATETIME(6) NOT NULL,
    vote_count      INT         NOT NULL DEFAULT 0,
    is_selected     TINYINT(1)  NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- 밴드별 후보곡 조회
    INDEX idx_band_song_band_id (band_id),
    -- 선정곡 / 활성 후보 조회 커버링 인덱스 (findActiveCandidates, findSelectedSong)
    INDEX idx_band_song_band_selected (band_id, is_selected),

    CONSTRAINT fk_band_song_band FOREIGN KEY (band_id) REFERENCES band (id),
    CONSTRAINT fk_band_song_song FOREIGN KEY (song_id) REFERENCES song (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 8. song_vote
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS song_vote (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    band_song_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    band_id      BIGINT      NOT NULL,
    voted_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- ★ 중복 투표 방지: 한 유저는 후보곡당 1표만
    UNIQUE KEY uk_song_vote (band_song_id, user_id),

    -- 투표 수 집계 (countByBandSongId)
    INDEX idx_song_vote_band_song_id (band_song_id),
    -- 밴드 내 투표 여부 확인 (countByBandIdAndUserId, countByBandId)
    INDEX idx_song_vote_band_user (band_id, user_id),
    INDEX idx_song_vote_band_id (band_id),

    CONSTRAINT fk_song_vote_band_song FOREIGN KEY (band_song_id) REFERENCES band_song (id),
    CONSTRAINT fk_song_vote_user      FOREIGN KEY (user_id)      REFERENCES users (id),
    CONSTRAINT fk_song_vote_band      FOREIGN KEY (band_id)      REFERENCES band (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 9. rehearsal  (합주 일정)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rehearsal (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    band_id        BIGINT       NOT NULL,
    title          VARCHAR(255) NOT NULL,
    description    TEXT,
    rehearsal_date DATETIME(6)  NOT NULL,
    location       VARCHAR(255),
    max_capacity   INT          NOT NULL,
    current_count  INT          NOT NULL DEFAULT 0,
    created_at     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    INDEX idx_rehearsal_band_id (band_id),
    INDEX idx_rehearsal_date    (rehearsal_date),

    CONSTRAINT fk_rehearsal_band FOREIGN KEY (band_id) REFERENCES band (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ----------------------------------------------------------------
-- 10. rehearsal_attendance
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rehearsal_attendance (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    rehearsal_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),

    -- ★ 중복 참여 방지: 한 유저는 합주당 1회만 신청
    UNIQUE KEY uk_rehearsal_attendance (rehearsal_id, user_id),

    -- 참여자 목록 조회 (findByRehearsalId)
    INDEX idx_rehearsal_attendance_rehearsal_id (rehearsal_id),

    CONSTRAINT fk_rehearsal_attendance_rehearsal FOREIGN KEY (rehearsal_id) REFERENCES rehearsal (id),
    CONSTRAINT fk_rehearsal_attendance_user      FOREIGN KEY (user_id)      REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
