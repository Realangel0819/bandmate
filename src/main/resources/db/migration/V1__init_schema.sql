-- ================================================================
-- BandMate V1 초기 스키마
-- Flyway가 관리 — 직접 수정하지 말 것. 변경은 V2__ 이후 파일로.
-- ================================================================

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    email      VARCHAR(255) NOT NULL,
    password   VARCHAR(255) NOT NULL,
    nickname   VARCHAR(100) NOT NULL,
    created_at DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email    (email),
    UNIQUE KEY uk_users_nickname (nickname)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS band (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    leader_id            BIGINT       NOT NULL,
    max_votes_per_person INT          NOT NULL DEFAULT 1,
    created_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at           DATETIME(6)  NULL     DEFAULT NULL,

    PRIMARY KEY (id),
    INDEX idx_band_leader_id (leader_id),
    CONSTRAINT fk_band_leader FOREIGN KEY (leader_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS band_member (
    id        BIGINT      NOT NULL AUTO_INCREMENT,
    band_id   BIGINT      NOT NULL,
    user_id   BIGINT      NOT NULL,
    position  VARCHAR(20) NOT NULL,
    joined_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_band_member (band_id, user_id),
    INDEX idx_band_member_band_id (band_id),
    INDEX idx_band_member_user_id (user_id),
    CONSTRAINT fk_band_member_band FOREIGN KEY (band_id) REFERENCES band (id),
    CONSTRAINT fk_band_member_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS band_recruit (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    band_id        BIGINT      NOT NULL,
    position       VARCHAR(20) NOT NULL,
    required_count INT         NOT NULL,
    current_count  INT         NOT NULL DEFAULT 0,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    INDEX idx_band_recruit_band_id (band_id),
    CONSTRAINT fk_band_recruit_band FOREIGN KEY (band_id) REFERENCES band (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS band_application (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    band_id      BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    recruit_id   BIGINT      NOT NULL,
    position     VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    introduction TEXT,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_band_application (band_id, user_id),
    INDEX idx_band_application_band_id (band_id),
    INDEX idx_band_application_user_id (user_id),
    INDEX idx_band_application_recruit_status (recruit_id, status),
    CONSTRAINT fk_band_application_band    FOREIGN KEY (band_id)    REFERENCES band (id),
    CONSTRAINT fk_band_application_user    FOREIGN KEY (user_id)    REFERENCES users (id),
    CONSTRAINT fk_band_application_recruit FOREIGN KEY (recruit_id) REFERENCES band_recruit (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS song (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(255) NOT NULL,
    artist      VARCHAR(255) NOT NULL,
    youtube_url TEXT,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_song_title_artist (title, artist)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

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
    INDEX idx_band_song_band_id       (band_id),
    INDEX idx_band_song_band_selected (band_id, is_selected),
    CONSTRAINT fk_band_song_band FOREIGN KEY (band_id) REFERENCES band (id),
    CONSTRAINT fk_band_song_song FOREIGN KEY (song_id) REFERENCES song (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS song_vote (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    band_song_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    band_id      BIGINT      NOT NULL,
    voted_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_song_vote (band_song_id, user_id),
    INDEX idx_song_vote_band_song_id (band_song_id),
    INDEX idx_song_vote_band_user    (band_id, user_id),
    INDEX idx_song_vote_band_id      (band_id),
    CONSTRAINT fk_song_vote_band_song FOREIGN KEY (band_song_id) REFERENCES band_song (id),
    CONSTRAINT fk_song_vote_user      FOREIGN KEY (user_id)      REFERENCES users (id),
    CONSTRAINT fk_song_vote_band      FOREIGN KEY (band_id)      REFERENCES band (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

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

CREATE TABLE IF NOT EXISTS rehearsal_attendance (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    rehearsal_id BIGINT      NOT NULL,
    user_id      BIGINT      NOT NULL,
    created_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_rehearsal_attendance (rehearsal_id, user_id),
    INDEX idx_rehearsal_attendance_rehearsal_id (rehearsal_id),
    CONSTRAINT fk_rehearsal_attendance_rehearsal FOREIGN KEY (rehearsal_id) REFERENCES rehearsal (id),
    CONSTRAINT fk_rehearsal_attendance_user      FOREIGN KEY (user_id)      REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
