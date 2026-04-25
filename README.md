# 🎸 BandMate

> 밴드 팀원 모집부터 합주 일정 관리까지 — 밴드 활동을 위한 풀스택 협업 플랫폼

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Flyway](https://img.shields.io/badge/Flyway-Migration-CC0000)
![React](https://img.shields.io/badge/React-18-61DAFB)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)
![AWS](https://img.shields.io/badge/AWS-EC2%20%2F%20RDS-FF9900)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [데이터베이스 설계](#데이터베이스-설계)
- [스키마 버전 관리 (Flyway)](#스키마-버전-관리-flyway)
- [API 명세](#api-명세)
- [주요 구현 포인트](#주요-구현-포인트)
- [프로젝트 구조](#프로젝트-구조)
- [실행 방법](#실행-방법)
- [배포](#배포)

---

## 프로젝트 소개

BandMate는 밴드 팀원 모집, 포지션별 지원/승인, 공연곡 투표, 합주 일정 관리를 지원하는 **풀스택 웹 애플리케이션**입니다.

| 기능 | 설명 |
|------|------|
| 밴드 모집 | 포지션별 정원 설정 · 자기소개 포함 지원 · 리더 승인/거절 · 멤버 강퇴/탈퇴 |
| 공연곡 선정 | 곡 후보 등록(유튜브 링크) → 멤버 투표 → 복수 선정 · 자동 선정 |
| 합주 일정 | 일정 생성 · 참여 신청 · 비관적 락 동시성 안전 정원 관리 |

**백엔드 구현 목표:** 비관적 락 동시성 처리, Soft Delete, Flyway 마이그레이션, JPA Entity 설계, DTO 분리, 글로벌 예외 처리 등 실무 백엔드 패턴을 직접 구현

---

## 기술 스택

### Backend

| 분류 | 기술 | 선택 이유 |
|------|------|----------|
| Language | Java 17 | 최신 문법 활용 |
| Framework | Spring Boot 3.5 | 빠른 설정, 풍부한 생태계 |
| ORM | Spring Data JPA (Hibernate 6) | 객체-관계 매핑, JPQL 쿼리 |
| DB Migration | **Flyway** | 버전 관리되는 스키마 변경, 운영 DB 안전한 마이그레이션 |
| Security | Spring Security + JWT (jjwt 0.12.5) | Stateless 인증, Bearer 토큰 |
| Validation | Bean Validation (`@Valid`) | 입력 검증 자동화 |
| Database | MySQL 8.0 | 트랜잭션, 인덱스, 비관적 락 지원 |
| Build | Gradle | 빌드 캐시, 의존성 관리 |

### Frontend

| 분류 | 기술 |
|------|------|
| Framework | React 18 + TypeScript |
| Build Tool | Vite |
| 서버 상태 | TanStack Query (React Query v5) |
| 클라이언트 상태 | Zustand + persist |
| HTTP | Axios (인터셉터로 JWT 자동 첨부) |
| CSS | Tailwind CSS v4 |

### Infra

| 분류 | 기술 |
|------|------|
| Containerize | Docker + Docker Compose |
| Web Server | nginx (SPA 서빙 + /api 역방향 프록시) |
| Deploy | AWS EC2 (t2.micro, docker-compose.prod.yml) |
| Database | AWS RDS MySQL 8.0 (자동 백업, 다중 AZ 옵션) |
| Backup | mysqldump → S3 (cron 스케줄, 7일 보관) |

---

## 주요 기능

### 👤 사용자 인증
- 회원가입 — 이메일·닉네임 중복 검사, BCrypt 비밀번호 암호화
- 로그인 → JWT 발급 (유효기간 24시간)
- 모든 인증 필요 API: `Authorization: Bearer {token}` 헤더

### 🎸 밴드 관리
- 밴드 생성 (생성자 = 리더, 자동으로 VOCAL 포지션 멤버 등록)
- **전체 밴드 목록 공개 조회** + 내가 속한 밴드 목록 (리더/멤버 모두 포함)
- **멤버 목록 닉네임 표시** — userId 대신 실제 닉네임+포지션+리더 뱃지
- **멤버 강퇴/탈퇴** — 리더는 모든 멤버 강퇴 가능, 일반 멤버는 본인만 탈퇴 가능
- 밴드 Soft Delete (`deleted_at` 기반, 이후 모든 조회에서 자동 제외)
- 포지션별 모집 공고 등록 + **자기소개 포함 지원서** + 리더 승인/거절

### 🎵 공연곡 선정 시스템
- 글로벌 곡 카탈로그 등록 (제목+아티스트 중복 방지, 유튜브 링크)
- **밴드 멤버만 투표 가능** — 비멤버 차단
- **인당 투표 수 설정** — 리더가 1~N표 설정
- 모든 멤버 투표 완료 시 **최다 득표곡 자동 선정**
- 리더: 복수 곡 수동 선정/취소, 후보곡 초기화, 투표 초기화

### 📅 합주 일정 관리
- 리더가 일정 생성 (제목, 날짜, 장소, 정원)
- **비관적 락(SELECT FOR UPDATE)** — 동시 신청 시 정원 초과 완전 차단
- **참여자 목록 닉네임 표시** — 실제 닉네임으로 표시

### 🛡️ 공통
- 글로벌 예외 처리 (`@RestControllerAdvice`) — 404/403/409/400/500 명확히 분리
- DTO 입력 검증 (`@Valid`) — 모든 요청 바디 필드 검증 자동화

---

## 데이터베이스 설계

### ERD 다이어그램

```
┌─────────────┐       ┌──────────────────────┐       ┌─────────────────────┐
│    users    │       │         band         │       │    band_member      │
├─────────────┤       ├──────────────────────┤       ├─────────────────────┤
│ id (PK)     │◄──┐   │ id (PK)              │◄──┬───│ id (PK)             │
│ email UNIQ  │   │   │ name                 │   │   │ band_id (FK)        │
│ nickname    │   └───│ leader_id (FK)       │   │   │ user_id (FK)        │
│ password    │       │ max_votes_per_person │   │   │ position            │
└─────────────┘       │ deleted_at           │   │   └─────────────────────┘
                      └──────────────────────┘   │
                               │                 │   ┌─────────────────────┐
                               │                 │   │    band_recruit     │
                               │                 └───│ id (PK)             │
                               │                     │ band_id (FK)        │
                               │                     │ position            │
                               │                     │ required_count      │
                               │                     │ current_count       │
                               │                     └─────────────────────┘
                               │                              │
                               │                     ┌─────────────────────┐
                               │                     │  band_application   │
                               │                     ├─────────────────────┤
                               │                     │ band_id (FK)        │
                               │                     │ user_id (FK)        │
                               │                     │ recruit_id (FK)     │
                               │                     │ status (ENUM)       │
                               │                     │ introduction (TEXT) │
                               │                     └─────────────────────┘
                               │
          ┌────────────────────┴────────────────────┐
          │                                         │
┌─────────────────┐                     ┌───────────────────┐
│    band_song    │                     │     rehearsal     │
├─────────────────┤                     ├───────────────────┤
│ band_id (FK)    │                     │ band_id (FK)      │
│ song_id (FK)    │                     │ rehearsal_date    │
│ vote_count      │                     │ max_capacity      │
│ is_selected     │                     │ current_count     │◄── 비관적 락
└─────────────────┘                     └───────────────────┘
        │                                         │
┌───────────────┐                     ┌───────────────────────┐
│   song_vote   │                     │ rehearsal_attendance  │
├───────────────┤                     ├───────────────────────┤
│ band_song_id  │◄── UNIQUE           │ rehearsal_id (FK)     │◄── UNIQUE
│ user_id (FK)  │    (band_song_id,   │ user_id (FK)          │    (rehearsal_id,
│ band_id (FK)  │     user_id)        └───────────────────────┘     user_id)
└───────────────┘

┌──────────────────────────────┐   ┌──────────────────────────────┐
│            song              │   │      flyway_schema_history   │
├──────────────────────────────┤   ├──────────────────────────────┤
│ title                        │   │ installed_rank               │
│ artist                       │   │ version (V1, V2 ...)         │
│ youtube_url (TEXT)           │   │ checksum                     │
└──────────────────────────────┘   └──────────────────────────────┘
```

### 테이블 상세 설명

#### `band`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `leader_id` | BIGINT FK | 리더 = 밴드 생성자, 권한 검증에 사용 |
| `max_votes_per_person` | INT DEFAULT 1 | 리더가 설정하는 인당 최대 투표 수 |
| `deleted_at` | DATETIME NULL | NULL이면 활성 밴드 — `@SQLRestriction`으로 자동 필터링 |

#### `band_application`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `status` | ENUM | `PENDING` → `APPROVED` / `REJECTED` 단방향 상태 전이 |
| `introduction` | TEXT NULL | 지원자 자기소개 (자유 기입) |
| UNIQUE | `(band_id, user_id)` | 동일 밴드 중복 지원 불가 |

#### `song_vote`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `band_id` | BIGINT FK | JOIN 없이 `countByBandIdAndUserId()` 가능하도록 비정규화 보관 |
| UNIQUE | `(band_song_id, user_id)` | 같은 곡 중복 투표 불가 |

#### `rehearsal`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `current_count` | INT DEFAULT 0 | 비관적 락으로 보호되는 실시간 참여 인원 |

### 인덱스 전략

| 테이블 | 인덱스 | 대상 쿼리 |
|--------|--------|-----------|
| `band` | `idx_band_leader_id` | `findByLeaderId()` |
| `band_member` | `UNIQUE (band_id, user_id)` | 멤버 여부 확인 (투표/합주 권한) |
| `band_member` | `idx_band_member_user_id` | `findByUserId()` — 내 밴드 목록 |
| `band_application` | `(recruit_id, status)` | 포지션별 승인 인원 집계 |
| `band_song` | `(band_id, is_selected)` | 선정곡 / 후보곡 분리 조회 |
| `song_vote` | `(band_id, user_id)` | 인당 투표 수 조회 |
| `song_vote` | `band_song_id` | 곡별 득표 수 집계 |
| `rehearsal` | `rehearsal_date` | 날짜 기반 정렬 조회 |
| `rehearsal_attendance` | `rehearsal_id` | 참여자 목록 조회 |

---

## 스키마 버전 관리 (Flyway)

`ddl-auto: update` 대신 **Flyway**로 스키마를 코드로 관리합니다.

### 동작 방식

```
앱 시작
  │
  ▼
Flyway 실행
  │
  ├── flyway_schema_history 테이블 존재?
  │     ├── NO  → baseline-version(0) 베이스라인 생성 후 V1 적용
  │     └── YES → 미적용 마이그레이션 순서대로 적용
  │
  └── Hibernate validate — 엔티티와 실제 스키마 일치 확인
```

### 마이그레이션 파일 구조

```
src/main/resources/db/
├── migration/
│   ├── V1__init_schema.sql   ← 초기 전체 스키마 (10개 테이블)
│   └── V2__...sql            ← 다음 변경사항 (컬럼 추가 등)
└── schema.sql                ← 참고용 DDL (Flyway가 실제 실행)
```

### 기존 운영 DB에 Flyway 적용

```yaml
# application.yml / application-prod.yml
spring:
  flyway:
    baseline-on-migrate: true   # flyway_schema_history 없으면 자동 베이스라인 생성
    baseline-version: 0         # V0 베이스라인 후 V1부터 실행
```

`CREATE TABLE IF NOT EXISTS`로 작성된 V1은 기존 테이블이 있으면 건너뛰므로 **기존 운영 DB에 안전하게 적용** 가능합니다.

### 스키마 변경 예시 (V2)

```sql
-- src/main/resources/db/migration/V2__add_user_profile_image.sql
ALTER TABLE users ADD COLUMN profile_image_url VARCHAR(500) NULL AFTER nickname;
```

앱 재시작 시 Flyway가 자동으로 V2를 적용하고 체크섬을 기록합니다.

---

## API 명세

### 인증 `/api/users`

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/login` | 로그인 → JWT 반환 |

```json
// POST /api/users/login → Response
{ "token": "eyJ...", "userId": 1, "email": "user@example.com", "nickname": "기타리스트" }
```

---

### 밴드 `/api/bands`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| GET | `/` | - | 전체 밴드 목록 |
| POST | `/` | 로그인 | 밴드 생성 |
| GET | `/{bandId}` | - | 밴드 단건 조회 |
| GET | `/my-bands` | 로그인 | **내가 속한 밴드 목록** (리더·멤버 모두) |
| DELETE | `/{bandId}` | 리더 | 밴드 삭제 (Soft Delete) |
| PUT | `/{bandId}/vote-settings` | 리더 | 인당 투표 수 변경 |
| GET | `/{bandId}/members` | - | **현재 멤버 목록** (닉네임 포함) |
| DELETE | `/{bandId}/members/{memberId}` | 리더/본인 | **강퇴(리더) / 탈퇴(본인)** |
| POST | `/{bandId}/recruits` | 리더 | 모집 공고 등록 |
| GET | `/{bandId}/recruits` | - | 모집 공고 목록 |
| POST | `/{bandId}/apply` | 로그인 | 지원 (자기소개 포함) |
| GET | `/{bandId}/applications` | 리더 | 지원서 목록 (닉네임 포함) |
| PUT | `/{bandId}/applications/{id}/approve` | 리더 | 지원 승인 |
| PUT | `/{bandId}/applications/{id}/reject` | 리더 | 지원 거절 |

```json
// GET /api/bands/1/members → Response
[
  { "memberId": 1, "userId": 3, "nickname": "기타리스트", "position": "GUITAR", "joinedAt": "2025-04-25T10:00:00" },
  { "memberId": 2, "userId": 5, "nickname": "드러머김씨", "position": "DRUM",   "joinedAt": "2025-04-26T14:00:00" }
]

// DELETE /api/bands/1/members/2  (리더가 강퇴 또는 본인 탈퇴)
// → 204 No Content
// 리더는 탈퇴 불가 (밴드 삭제로 처리)
```

**포지션:** `VOCAL` · `GUITAR` · `BASS` · `DRUM` · `KEYBOARD` · `ETC`

---

### 공연곡 `/api/bands/{bandId}/songs`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/` | - | 곡 등록 (유튜브 링크 포함) |
| POST | `/candidates` | 리더 | 후보곡 추가 |
| DELETE | `/candidates` | 리더 | 후보곡 전체 초기화 |
| POST | `/vote` | **멤버** | 투표 (비멤버 차단) |
| DELETE | `/votes` | 리더 | 투표 전체 초기화 |
| PUT | `/{bandSongId}/select` | 리더 | 곡 선정 (복수 가능) |
| DELETE | `/{bandSongId}/select` | 리더 | 선정 취소 |
| GET | `/` | - | 전체 후보곡 목록 |
| GET | `/selected` | - | 선정된 곡 목록 |

---

### 합주 일정 `/api/bands/{bandId}/rehearsals`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/` | 리더 | 일정 생성 |
| GET | `/` | - | 일정 목록 |
| POST | `/{rehearsalId}/join` | 멤버 | 참여 신청 |
| DELETE | `/{rehearsalId}/join` | 멤버 | 참여 취소 |
| GET | `/{rehearsalId}/attendances` | 멤버 | 참여자 목록 **(닉네임 포함)** |

```json
// GET /api/bands/1/rehearsals/1/attendances → Response
[
  { "attendanceId": 1, "rehearsalId": 1, "userId": 3, "nickname": "기타리스트", "createdAt": "2025-05-01T10:00:00" }
]
```

### 에러 응답 형식

```json
{ "status": 404, "message": "밴드를 찾을 수 없습니다.", "timestamp": "2025-04-25T10:00:00" }
{ "status": 403, "message": "리더만 이 작업을 수행할 수 있습니다.", "timestamp": "..." }
{ "status": 409, "message": "이미 이 밴드에 지원했습니다.", "timestamp": "..." }
```

---

## 주요 구현 포인트

### 1. 비관적 락(Pessimistic Lock) — 합주 정원 동시성 처리

정원 5명인 합주에 10명이 동시에 신청할 때 정확히 5명만 성공해야 합니다.

```java
// RehearsalRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Rehearsal r WHERE r.id = :id")
Optional<Rehearsal> findByIdWithLock(@Param("id") Long id);
```

```java
public AttendanceResponse joinRehearsal(...) {
    // SELECT FOR UPDATE — 이 시점부터 같은 row에 다른 트랜잭션 대기
    Rehearsal rehearsal = rehearsalRepository.findByIdWithLock(rehearsalId)...;

    if (rehearsal.getCurrentCount() >= rehearsal.getMaxCapacity())
        throw new InvalidRequestException("정원이 초과되었습니다.");

    attendanceRepository.save(attendance);
    rehearsal.setCurrentCount(rehearsal.getCurrentCount() + 1);
    // 트랜잭션 커밋 → 락 해제 → 다음 대기 트랜잭션이 갱신된 count를 읽음
}
```

낙관적 락의 재시도 오버헤드보다 비관적 락의 직렬화가 마감 직전 집중 신청 패턴에 더 적합합니다.

---

### 2. Flyway — 코드로 관리하는 스키마

```
ddl-auto: update  →  예측 불가능한 자동 DDL, 컬럼 삭제 불가
ddl-auto: validate + Flyway  →  명시적 마이그레이션, 체크섬으로 변조 감지
```

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate          # 스키마 변경은 Flyway에게
  flyway:
    baseline-on-migrate: true     # 기존 DB 안전 적용
    baseline-version: 0
```

운영 DB에 컬럼 추가 시 `V2__.sql`만 추가하면 앱 재시작 시 자동 적용됩니다.

---

### 3. Soft Delete + @SQLRestriction 자동 필터링

```java
@Entity
@SQLRestriction("deleted_at IS NULL")   // 모든 JPA 쿼리에 자동 WHERE 추가
public class Band {
    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

Repository 코드 수정 없이 삭제된 밴드가 모든 조회에서 자동 제외됩니다.

---

### 4. 투표 시스템 — 인당 투표 수 제한 + 자동 선정

```java
public VoteResponse vote(Long bandId, VoteRequest request, Long userId) {
    // ① 밴드 멤버 여부 확인
    bandMemberRepository.findByBandIdAndUserId(bandId, userId)
            .orElseThrow(() -> new UnauthorizedException("밴드 멤버만 투표할 수 있습니다."));

    // ② 인당 투표 수 제한
    int votesUsed = songVoteRepository.countByBandIdAndUserId(bandId, userId);
    if (votesUsed >= band.getMaxVotesPerPerson())
        throw new InvalidRequestException("투표 가능 횟수를 모두 사용했습니다.");

    // ③ 같은 곡 중복 투표 방지 (DB UNIQUE + 서비스 레이어 이중 검사)
    songVoteRepository.save(vote);

    // ④ 전원 투표 완료 시 자동 선정
    int totalVotes = songVoteRepository.countByBandId(bandId);
    int totalMembers = bandMemberRepository.countByBandId(bandId);
    if (totalVotes >= totalMembers * band.getMaxVotesPerPerson()) {
        // 최다 득표 후보 → isSelected = true
    }
}
```

---

### 5. JPA 이중 필드 패턴 — FK Long ID + @ManyToOne 공존

```java
@Column(name = "band_id", nullable = false)
private Long bandId;                              // 쓰기/일반 조회에 사용

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "band_id", insertable = false, updatable = false)
@ToString.Exclude
private Band band;                                // 읽기 전용 네비게이션 (닉네임 등 조회)
```

서비스/레포지토리는 `Long bandId`만 사용하고, 닉네임 조회 등 조인이 필요한 경우에만 `member.getUser().getNickname()`으로 네비게이션합니다.

---

### 6. 비정규화를 통한 쿼리 최적화

`song_vote`에 `band_id`를 중복 보관해 JOIN 없이 인당 투표 수를 조회합니다.

```sql
-- JOIN이 필요한 방식 (비정규화 전)
SELECT COUNT(*) FROM song_vote sv
JOIN band_song bs ON sv.band_song_id = bs.id
WHERE bs.band_id = ? AND sv.user_id = ?

-- 단순 WHERE 절 (비정규화 후)
SELECT COUNT(*) FROM song_vote WHERE band_id = ? AND user_id = ?
```

투표 권한 체크는 모든 투표 요청마다 실행되므로 JOIN 제거가 실질적인 성능 개선으로 이어집니다.

---

### 7. 멤버 강퇴/탈퇴 — 역할 기반 권한 분리

```java
public void removeMember(Long bandId, Long memberId, Long requesterId) {
    // 리더는 탈퇴 불가 — 밴드 삭제로만 처리
    if (target.getUserId().equals(band.getLeaderId()))
        throw new InvalidRequestException("리더는 탈퇴할 수 없습니다.");

    boolean isLeader = band.getLeaderId().equals(requesterId);
    boolean isSelf   = target.getUserId().equals(requesterId);

    if (!isLeader && !isSelf)
        throw new UnauthorizedException("권한이 없습니다.");

    bandMemberRepository.delete(target);
}
```

단일 엔드포인트(`DELETE /members/{memberId}`)에서 요청자 역할에 따라 강퇴/탈퇴를 구분합니다.

---

## 프로젝트 구조

```
bandmate/
├── src/main/java/com/bandmate/
│   ├── common/
│   │   ├── exception/         # GlobalExceptionHandler + 커스텀 예외
│   │   └── util/JwtUtil.java
│   ├── config/SecurityConfig.java
│   ├── user/
│   ├── band/                  # BandService (getMyBands, removeMember, ...)
│   ├── song/                  # SongService (vote, autoSelect, reset, ...)
│   └── rehearsal/             # RehearsalService (비관적 락)
├── src/main/resources/
│   ├── application.yml
│   ├── application-prod.yml
│   └── db/
│       ├── migration/
│       │   └── V1__init_schema.sql    ← Flyway 관리 스키마
│       └── schema.sql                 ← 참고용 DDL
├── frontend/                   # React + TypeScript + Vite
│   ├── src/api/                # bands.ts, songs.ts, rehearsals.ts
│   ├── src/pages/              # HomePage, BandDetailPage, ...
│   ├── src/pages/tabs/         # MembersTab, SongsTab, RehearsalsTab
│   ├── nginx.conf
│   └── Dockerfile
├── scripts/
│   ├── backup.sh               # mysqldump → S3 자동 백업
│   └── setup-rds.sh            # EC2 RDS 연결 + cron 등록 헬퍼
├── Dockerfile
├── docker-compose.prod.yml     # EC2 배포 (Docker MySQL)
└── docker-compose.rds.yml      # EC2 배포 (AWS RDS 사용)
```

---

## 실행 방법

### 로컬 개발

```bash
git clone https://github.com/Realangel0819/bandmate.git
cd bandmate

# 백엔드 + MySQL 실행 (Flyway가 V1 자동 적용)
docker compose up -d --build

# 프론트엔드 개발 서버
cd frontend && npm install && npm run dev
# → http://localhost:5173
```

---

## 배포

### 아키텍처

```
[Browser]
    │  HTTP :80
    ▼
[nginx]  ←── frontend/dist (React SPA)
    │  /api/* 프록시
    ▼
[Spring Boot :8080]  ← Flyway 자동 마이그레이션
    │  JDBC
    ▼
[MySQL 8.0]  ←── Docker 컨테이너 또는 AWS RDS
```

---

### 옵션 A: Docker MySQL (EC2 단일 서버)

```bash
# .env 파일 생성
cat > .env << 'EOF'
JWT_SECRET=배포용-시크릿키-32자-이상
DB_USERNAME=bandmate
DB_PASSWORD=bandmate123
MYSQL_ROOT_PASSWORD=root_change_me
EOF

# 실행
docker compose -f docker-compose.prod.yml up -d --build

# 자동 백업 설정 (S3 업로드)
echo "S3_BUCKET=s3://your-bucket-name" >> .env
bash scripts/setup-rds.sh   # cron 등록 + AWS CLI 설치
```

---

### 옵션 B: AWS RDS + 자동 백업 (권장)

#### 1. RDS 인스턴스 생성

| 항목 | 설정 |
|------|------|
| 엔진 | MySQL 8.0 |
| 인스턴스 | db.t3.micro (Free Tier) |
| 스토리지 | gp2 20GB |
| **자동 백업** | **활성화, 보존 기간 7일** |
| 다중 AZ | 운영 환경 권장 |
| 퍼블릭 액세스 | 비활성화 (EC2 VPC 내부 통신) |

#### 2. 보안 그룹 설정

```
RDS 보안 그룹 인바운드:
  유형: MySQL/Aurora  포트: 3306  소스: EC2 보안 그룹 ID
```

#### 3. EC2에서 RDS로 배포

```bash
cat > .env << 'EOF'
JWT_SECRET=배포용-시크릿키-32자-이상
DB_HOST=bandmate.xxxx.ap-northeast-2.rds.amazonaws.com   # RDS 엔드포인트
DB_USERNAME=admin
DB_PASSWORD=RDS_패스워드
S3_BUCKET=s3://bandmate-backups
EOF

# RDS 전용 Compose 사용 (로컬 MySQL 컨테이너 없음)
docker compose -f docker-compose.rds.yml up -d --build

# 앱 시작 시 Flyway가 RDS에 V1 스키마 자동 생성
docker compose -f docker-compose.rds.yml logs app | grep -i flyway
```

#### 4. RDS 자동 백업 확인

AWS Console → RDS → 인스턴스 선택 → **유지 관리 및 백업** 탭:
- 자동 백업: 활성화됨
- 보존 기간: 7일
- 백업 시간대: 새벽 (UTC 기준)
- 특정 시점으로 복구(PITR): 지원됨

#### 5. mysqldump S3 백업 (추가 보장)

```bash
bash scripts/setup-rds.sh
# → cron 등록: 매일 새벽 3시 mysqldump → S3 자동 업로드 + 7일 보관
```

---

### 업데이트 배포

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
# Flyway가 새 마이그레이션(V2, V3 ...) 자동 적용 후 앱 시작
```
