# 🎸 BandMate

> 밴드 팀원 모집부터 합주 일정 관리까지 — 밴드 활동을 위한 풀스택 협업 플랫폼

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![React](https://img.shields.io/badge/React-18-61DAFB)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [데이터베이스 설계](#데이터베이스-설계)
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
| 밴드 모집 | 포지션별 정원 설정 · 자기소개 포함 지원 · 리더 승인/거절 |
| 공연곡 선정 | 곡 후보 등록(유튜브 링크) → 멤버 투표 → 복수 선정 · 자동 선정 |
| 합주 일정 | 일정 생성 · 참여 신청 · 비관적 락 동시성 안전 정원 관리 |

**백엔드 구현 목표:** 정원 제한, 중복 방지, 비관적 락 동시성 처리, Soft Delete, 글로벌 예외 처리, JPA Entity 설계, DTO 분리 등 실무 백엔드 패턴을 직접 구현

---

## 기술 스택

### Backend

| 분류 | 기술 | 선택 이유 |
|------|------|----------|
| Language | Java 17 | Record, sealed class 등 최신 문법 활용 |
| Framework | Spring Boot 3.5 | 빠른 설정, 풍부한 생태계 |
| ORM | Spring Data JPA (Hibernate 6) | 객체-관계 매핑, JPQL 쿼리 |
| Security | Spring Security + JWT (jjwt 0.12.5) | Stateless 인증, Bearer 토큰 |
| Validation | Bean Validation (`@Valid`, `@NotBlank`) | 입력 검증 자동화 |
| Database | MySQL 8.0 | 트랜잭션, 인덱스, 비관적 락 지원 |
| Build | Gradle | 빌드 캐시, 멀티 모듈 확장성 |

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
| Deploy | AWS EC2 (단일 서버, docker-compose.prod.yml) |

---

## 주요 기능

### 👤 사용자 인증
- 회원가입 — 이메일·닉네임 중복 검사, BCrypt 비밀번호 암호화
- 로그인 → JWT 발급 (유효기간 24시간)
- 모든 인증 필요 API: `Authorization: Bearer {token}` 헤더

### 🎸 밴드 모집 시스템
- 밴드 생성 (생성자 = 리더, 자동으로 VOCAL 포지션 멤버 등록)
- 포지션별 모집 공고 등록 (포지션 중복 허용 — 기타 2명 가능)
- **자기소개 포함 지원** — 경력/연락처 등 자유 기입
- 리더의 지원서 검토 후 승인/거절
- 정원 초과 지원 차단 · 중복 지원 방지 · 기존 멤버 재지원 차단
- 밴드 Soft Delete (`deleted_at` 기반, 이후 모든 조회에서 자동 제외)

### 🎵 공연곡 선정 시스템
- 글로벌 곡 카탈로그에 곡 등록 (제목+아티스트 중복 방지, 유튜브 링크 포함)
- 리더가 후보곡 추가 (투표 기간 자동 설정: 등록일 기준 +7일)
- **인당 투표 수 설정** — 리더가 1~N표 사이로 변경 가능
- **밴드 멤버만 투표 가능** — 비멤버 투표 시도 차단
- 모든 멤버가 모든 투표권 소진 시 최다 득표곡 **자동 선정**
- 리더가 복수 곡 수동 선정/취소 가능
- 후보곡 전체 초기화 · 투표만 초기화 (리더 전용)

### 📅 합주 일정 관리
- 리더가 일정 생성 (제목, 날짜, 장소, 정원)
- 밴드 멤버만 참여 신청/취소 가능
- **비관적 락(Pessimistic Lock)** — 동시 신청 시 정원 초과 완전 차단
- 참여자 목록 조회 (밴드 멤버 공개)

### 🛡️ 공통
- 글로벌 예외 처리 (`@RestControllerAdvice`) — 404/403/409/400/500 명확히 분리
- DTO 입력 검증 (`@Valid`) — 모든 요청 바디의 필드 검증 자동화
- 전체 밴드 목록 공개 조회 (로그인 불필요)

---

## 데이터베이스 설계

### ERD 다이어그램

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────────────┐
│    users    │       │      band        │       │    band_member      │
├─────────────┤       ├──────────────────┤       ├─────────────────────┤
│ id (PK)     │◄──┐   │ id (PK)          │◄──┬───│ id (PK)             │
│ email UNIQ  │   │   │ name             │   │   │ band_id (FK)        │
│ nickname    │   └───│ leader_id (FK)   │   │   │ user_id (FK)        │
│ password    │       │ max_votes_person │   │   │ position            │
└─────────────┘       │ deleted_at       │   │   └─────────────────────┘
                      └──────────────────┘   │
                               │             │   ┌─────────────────────┐
                               │             │   │   band_recruit      │
                               │             └───│ id (PK)             │
                               │                 │ band_id (FK)        │
                               │                 │ position            │
                               │                 │ required_count      │
                               │                 │ current_count       │
                               │                 └─────────────────────┘
                               │                          │
                               │                 ┌─────────────────────┐
                               │                 │  band_application   │
                               │                 ├─────────────────────┤
                               │                 │ id (PK)             │
                               │                 │ band_id (FK)        │
                               │                 │ user_id (FK)        │
                               │                 │ recruit_id (FK)     │
                               │                 │ position            │
                               │                 │ status (ENUM)       │
                               │                 │ introduction (TEXT) │
                               │                 └─────────────────────┘
                               │
          ┌────────────────────┴────────────────────┐
          │                                         │
┌─────────────────┐                     ┌───────────────────┐
│    band_song    │                     │     rehearsal     │
├─────────────────┤                     ├───────────────────┤
│ id (PK)         │                     │ id (PK)           │
│ band_id (FK)    │                     │ band_id (FK)      │
│ song_id (FK)    │                     │ title             │
│ vote_start_date │                     │ rehearsal_date    │
│ vote_end_date   │                     │ location          │
│ vote_count      │                     │ max_capacity      │
│ is_selected     │                     │ current_count     │
└─────────────────┘                     └───────────────────┘
        │                                         │
┌───────────────┐                     ┌───────────────────────┐
│   song_vote   │                     │ rehearsal_attendance  │
├───────────────┤                     ├───────────────────────┤
│ id (PK)       │                     │ id (PK)               │
│ band_song_id  │◄── UNIQUE           │ rehearsal_id (FK)     │◄── UNIQUE
│ user_id (FK)  │    (band_song_id,   │ user_id (FK)          │    (rehearsal_id,
│ band_id (FK)  │     user_id)        └───────────────────────┘     user_id)
└───────────────┘

┌──────────────────────────────┐
│            song              │
├──────────────────────────────┤
│ id (PK)                      │
│ title                        │◄── UNIQUE (title, artist)
│ artist                       │
│ youtube_url (TEXT)           │
└──────────────────────────────┘
```

### 테이블 상세 설명

#### `band`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `leader_id` | BIGINT FK | 리더 = 밴드 생성자, 권한 검증에 사용 |
| `max_votes_per_person` | INT DEFAULT 1 | 리더가 설정하는 인당 최대 투표 수 |
| `deleted_at` | DATETIME NULL | NULL이면 활성 밴드, 값이 있으면 삭제됨 |

`@SQLRestriction("deleted_at IS NULL")` 어노테이션으로 JPA의 모든 쿼리에서 삭제된 밴드를 자동 제외.

#### `band_application`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `status` | ENUM | `PENDING` → `APPROVED` / `REJECTED` 단방향 상태 전이 |
| `introduction` | TEXT NULL | 지원자 자기소개 (자유 기입) |
| UNIQUE | `(band_id, user_id)` | 동일 밴드에 중복 지원 불가 |

#### `song_vote`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `band_id` | BIGINT FK | 밴드 기준 투표 수 집계를 위해 비정규화 보관 |
| UNIQUE | `(band_song_id, user_id)` | 같은 곡에 중복 투표 불가 |

`band_id`를 `song_vote`에 직접 보관해 `JOIN` 없이 `countByBandIdAndUserId()` 쿼리 최적화.

#### `rehearsal`
| 컬럼 | 타입 | 설명 |
|------|------|------|
| `current_count` | INT DEFAULT 0 | 비관적 락으로 보호되는 실시간 참여 인원 |

### 인덱스 전략

| 테이블 | 인덱스 | 쿼리 |
|--------|--------|------|
| `band` | `idx_band_leader_id` | `findByLeaderId()` — 내 밴드 목록 |
| `band_member` | `(band_id, user_id)` | 멤버 여부 확인 (투표/합주 권한 검사) |
| `band_application` | `(recruit_id, status)` | 포지션별 승인 인원 집계 |
| `band_song` | `(band_id, is_selected)` | 선정곡 / 후보곡 분리 조회 |
| `song_vote` | `(band_id, user_id)` | 인당 투표 수 조회 |
| `song_vote` | `band_song_id` | 곡별 득표 수 집계 |
| `rehearsal` | `rehearsal_date` | 날짜 기반 정렬 조회 |
| `rehearsal_attendance` | `rehearsal_id` | 참여자 목록 조회 |

---

## API 명세

### 인증 `/api/users`

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/users/signup` | 회원가입 |
| POST | `/api/users/login` | 로그인 → JWT 반환 |

```json
// POST /api/users/signup
{ "email": "user@example.com", "password": "pass1234!", "nickname": "기타리스트" }

// POST /api/users/login → Response
{ "token": "eyJ...", "userId": 1, "email": "user@example.com", "nickname": "기타리스트" }
```

---

### 밴드 `/api/bands`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| GET | `/` | - | **전체 밴드 목록** |
| POST | `/` | 로그인 | 밴드 생성 |
| GET | `/{bandId}` | - | 밴드 단건 조회 |
| GET | `/my-bands` | 로그인 | 내가 만든 밴드 목록 |
| DELETE | `/{bandId}` | 리더 | 밴드 삭제 (Soft Delete) |
| PUT | `/{bandId}/vote-settings` | 리더 | 인당 투표 수 변경 |
| POST | `/{bandId}/recruits` | 리더 | 모집 공고 등록 |
| GET | `/{bandId}/recruits` | - | 모집 공고 목록 |
| POST | `/{bandId}/apply` | 로그인 | 지원 (자기소개 포함) |
| GET | `/{bandId}/applications` | 리더 | 지원서 목록 조회 |
| PUT | `/{bandId}/applications/{id}/approve` | 리더 | 지원 승인 |
| PUT | `/{bandId}/applications/{id}/reject` | 리더 | 지원 거절 |

```json
// POST /api/bands
{ "name": "SilverRock", "description": "홍대 기반 록밴드" }

// Response
{
  "bandId": 1,
  "name": "SilverRock",
  "leaderId": 3,
  "memberCount": 1,
  "maxVotesPerPerson": 1,
  "createdAt": "2025-04-25T10:00:00"
}

// POST /api/bands/1/recruits
{ "position": "GUITAR", "requiredCount": 2 }

// POST /api/bands/1/apply
{ "recruitId": 1, "position": "GUITAR", "introduction": "기타 경력 5년, 연락처 010-xxxx" }

// PUT /api/bands/1/vote-settings
{ "maxVotesPerPerson": 3 }
```

**포지션:** `VOCAL` · `GUITAR` · `BASS` · `DRUM` · `KEYBOARD` · `ETC`

---

### 공연곡 `/api/bands/{bandId}/songs`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/` | - | 곡 등록 (글로벌 카탈로그) |
| POST | `/candidates` | 리더 | 후보곡 추가 |
| DELETE | `/candidates` | 리더 | **후보곡 전체 초기화** |
| POST | `/vote` | 멤버 | 투표 |
| DELETE | `/votes` | 리더 | **투표 전체 초기화** |
| PUT | `/{bandSongId}/select` | 리더 | 곡 선정 (복수 가능) |
| DELETE | `/{bandSongId}/select` | 리더 | 선정 취소 |
| GET | `/` | - | 전체 후보곡 목록 |
| GET | `/selected` | - | 선정된 곡 목록 |

```json
// POST /api/bands/1/songs (곡 등록)
{ "title": "Bohemian Rhapsody", "artist": "Queen", "youtubeUrl": "https://youtu.be/fJ9rUzIMcZQ" }
// → { "id": 1, "title": "Bohemian Rhapsody", "artist": "Queen", "youtubeUrl": "..." }

// POST /api/bands/1/songs/candidates (후보 등록)
{ "songId": 1, "voteStartDate": "2025-04-25T00:00:00", "voteEndDate": "2025-05-02T23:59:59" }

// POST /api/bands/1/songs/vote
{ "bandSongId": 1 }
// → "모든 멤버가 투표를 완료했습니다. 최다 득표곡이 자동으로 선정되었습니다."

// GET /api/bands/1/songs → Response 예시
[
  {
    "bandSongId": 1,
    "title": "Bohemian Rhapsody",
    "artist": "Queen",
    "youtubeUrl": "https://youtu.be/fJ9rUzIMcZQ",
    "voteCount": 3,
    "isSelected": true,
    "isVotingActive": false
  }
]
```

---

### 합주 일정 `/api/bands/{bandId}/rehearsals`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/` | 리더 | 일정 생성 |
| GET | `/` | - | 일정 목록 |
| GET | `/{rehearsalId}` | - | 일정 단건 조회 |
| POST | `/{rehearsalId}/join` | 멤버 | 참여 신청 |
| DELETE | `/{rehearsalId}/join` | 멤버 | 참여 취소 |
| GET | `/{rehearsalId}/attendances` | 멤버 | 참여자 목록 |

```json
// POST /api/bands/1/rehearsals
{
  "title": "정기 합주",
  "rehearsalDate": "2025-05-10T14:00:00",
  "location": "홍대 연습실 A",
  "maxCapacity": 5
}

// Response
{
  "rehearsalId": 1,
  "title": "정기 합주",
  "rehearsalDate": "2025-05-10T14:00:00",
  "location": "홍대 연습실 A",
  "maxCapacity": 5,
  "currentAttendees": 0
}
```

---

### 에러 응답 형식

```json
// 404 Not Found
{ "status": 404, "message": "밴드를 찾을 수 없습니다.", "timestamp": "2025-04-25T10:00:00" }

// 403 Unauthorized
{ "status": 403, "message": "리더만 이 작업을 수행할 수 있습니다.", "timestamp": "..." }

// 409 Conflict
{ "status": 409, "message": "이미 이 밴드에 지원했습니다.", "timestamp": "..." }

// 400 Bad Request (Validation)
{ "status": 400, "message": "email: 올바른 이메일 형식이 아닙니다.", "timestamp": "..." }
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
// RehearsalService.java
public AttendanceResponse joinRehearsal(Long bandId, Long rehearsalId, Long userId) {
    // SELECT FOR UPDATE — 이 시점부터 같은 row에 다른 트랜잭션 대기
    Rehearsal rehearsal = rehearsalRepository.findByIdWithLock(rehearsalId)
            .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다."));

    if (rehearsal.getCurrentCount() >= rehearsal.getMaxCapacity()) {
        throw new InvalidRequestException("정원이 초과되었습니다.");
    }

    attendanceRepository.save(attendance);
    rehearsal.setCurrentCount(rehearsal.getCurrentCount() + 1);
    // 트랜잭션 커밋 시 락 해제 → 다음 대기 트랜잭션이 갱신된 count를 읽음
}
```

**낙관적 락 대신 비관적 락을 선택한 이유**
합주 신청은 마감 직전에 요청이 집중되는 패턴이므로 충돌 빈도가 높습니다.  
낙관적 락의 재시도 오버헤드보다 비관적 락의 직렬화가 더 효율적입니다.

---

### 2. Soft Delete + @SQLRestriction 자동 필터링

밴드 삭제 시 DB 레코드를 물리적으로 제거하지 않고 `deleted_at` 타임스탬프를 기록합니다.  
`@SQLRestriction`으로 모든 JPA 쿼리에 `WHERE deleted_at IS NULL` 조건이 자동 추가됩니다.

```java
@Entity
@SQLRestriction("deleted_at IS NULL")  // 모든 JPQL/findById/findAll에 자동 적용
public class Band {

    private LocalDateTime deletedAt;

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

**장점:** Repository 코드 수정 없이 삭제된 밴드를 모든 조회에서 자동 제외.  
**주의:** 직접 JDBC나 Native Query 사용 시에는 조건을 명시해야 함.

---

### 3. 투표 시스템 — 인당 투표 수 제한 + 자동 선정

밴드별로 리더가 인당 최대 투표 수(`maxVotesPerPerson`)를 설정할 수 있습니다.  
모든 멤버가 투표권을 모두 소진하면 최다 득표곡이 자동으로 선정됩니다.

```java
// SongService.java
public VoteResponse vote(Long bandId, VoteRequest request, Long userId) {
    // ① 밴드 멤버 여부 확인
    bandMemberRepository.findByBandIdAndUserId(bandId, userId)
            .orElseThrow(() -> new UnauthorizedException("밴드 멤버만 투표할 수 있습니다."));

    // ② 인당 투표 수 제한 확인
    Band band = bandRepository.findById(bandId).orElseThrow(...);
    int votesUsed = songVoteRepository.countByBandIdAndUserId(bandId, userId);
    if (votesUsed >= band.getMaxVotesPerPerson()) {
        throw new InvalidRequestException(
            "투표 가능 횟수(" + band.getMaxVotesPerPerson() + "표)를 모두 사용했습니다.");
    }

    // ③ 같은 곡 중복 투표 방지 (DB UNIQUE 제약 + 서비스 레이어 이중 검사)
    if (songVoteRepository.findByBandSongIdAndUserId(request.getBandSongId(), userId).isPresent()) {
        throw new DuplicateException("이 곡에 이미 투표했습니다.");
    }

    songVoteRepository.save(vote);
    bandSong.setVoteCount(songVoteRepository.countByBandSongId(request.getBandSongId()));

    // ④ 전원 투표 완료 시 자동 선정
    int totalVotes = songVoteRepository.countByBandId(bandId);
    int totalMembers = bandMemberRepository.countByBandId(bandId);
    if (totalVotes >= totalMembers * band.getMaxVotesPerPerson()) {
        // 최다 득표 후보 → isSelected = true
        bandSongRepository.findByBandId(bandId).stream()
                .filter(bs -> !bs.getIsSelected() && bs.isVotingActive())
                .max(Comparator.comparingInt(BandSong::getVoteCount))
                .ifPresent(winner -> winner.setIsSelected(true));
    }
}
```

**중복 방지 이중 전략**
- DB 레벨: `song_vote` 테이블의 `UNIQUE KEY (band_song_id, user_id)`
- 서비스 레벨: `findByBandSongIdAndUserId()` 선조회 후 `DuplicateException` 던짐
  → DB 예외를 깔끔한 비즈니스 예외로 변환하여 클라이언트에 명확한 메시지 전달

---

### 4. JPA 이중 필드 패턴 — ID + @ManyToOne 공존

FK 컬럼을 순수 Long ID로 관리하면서 동시에 JPA 네비게이션 속성을 제공합니다.

```java
@Column(name = "band_id", nullable = false)
private Long bandId;                              // 쓰기/조회에 사용 (기존 코드 호환)

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "band_id", insertable = false, updatable = false)
@ToString.Exclude                                 // Lombok 순환 참조 방지
private Band band;                                // 읽기 전용 네비게이션
```

**장점:**
- 서비스/레포지토리는 `Long bandId`만 사용 → 단순하고 예측 가능한 코드
- 필요한 경우 `member.getBand().getName()` 같은 JPA 네비게이션도 가능
- `insertable = false, updatable = false`로 FK 중복 관리 방지

---

### 5. 글로벌 예외 처리 계층

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return ResponseEntity.status(404).body(new ErrorResponse(404, e.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)       // → 403
    @ExceptionHandler(DuplicateException.class)          // → 409
    @ExceptionHandler(InvalidRequestException.class)     // → 400

    // @Valid 실패 시 필드명 + 메시지 자동 포매팅
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(400, message));
    }
}
```

모든 예외를 한 곳에서 처리해 컨트롤러가 비즈니스 로직에만 집중할 수 있도록 합니다.

---

### 6. 비정규화를 통한 쿼리 최적화

`song_vote` 테이블에 `band_id`를 중복 보관합니다.

```sql
-- band_id 없는 경우: JOIN이 필요
SELECT COUNT(*) FROM song_vote sv
JOIN band_song bs ON sv.band_song_id = bs.id
WHERE bs.band_id = ? AND sv.user_id = ?

-- band_id 보관 시: 단순 WHERE 절
SELECT COUNT(*) FROM song_vote
WHERE band_id = ? AND user_id = ?
```

투표 권한 체크는 모든 투표 요청마다 실행되므로 JOIN 제거가 실질적인 성능 개선으로 이어집니다.

---

## 프로젝트 구조

```
bandmate/
├── src/main/java/com/bandmate/
│   ├── BandmateApplication.java
│   ├── common/
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java   # @RestControllerAdvice
│   │   │   ├── NotFoundException.java        # → 404
│   │   │   ├── UnauthorizedException.java    # → 403
│   │   │   ├── DuplicateException.java       # → 409
│   │   │   └── InvalidRequestException.java  # → 400
│   │   └── util/JwtUtil.java
│   ├── config/
│   │   └── SecurityConfig.java              # CORS · JWT · permitAll
│   ├── user/
│   │   ├── entity/User.java
│   │   ├── dto/  (SignupRequest, LoginRequest, LoginResponse)
│   │   ├── repository/UserRepository.java
│   │   ├── service/UserService.java
│   │   └── controller/UserController.java
│   ├── band/
│   │   ├── entity/  (Band, BandMember, BandRecruit, BandApplication)
│   │   ├── dto/     (BandResponse, RecruitResponse, ApplicationResponse ...)
│   │   ├── repository/
│   │   ├── service/BandService.java
│   │   └── controller/BandController.java
│   ├── song/
│   │   ├── entity/  (Song, BandSong, SongVote)
│   │   ├── dto/     (BandSongResponse, VoteRequest, AddSongCandidateRequest ...)
│   │   ├── repository/
│   │   ├── service/SongService.java
│   │   └── controller/SongController.java
│   └── rehearsal/
│       ├── entity/  (Rehearsal, RehearsalAttendance)
│       ├── dto/     (RehearsalResponse, AttendanceResponse ...)
│       ├── repository/  (findByIdWithLock — SELECT FOR UPDATE)
│       ├── service/RehearsalService.java    # 비관적 락
│       └── controller/RehearsalController.java
├── src/main/resources/
│   ├── application.yml                      # MySQL 연결 설정
│   └── db/schema.sql                        # 전체 DDL (인덱스 포함)
├── frontend/                                # React + TypeScript + Vite
│   ├── src/
│   │   ├── api/         (bands.ts, songs.ts, rehearsals.ts, client.ts)
│   │   ├── pages/       (HomePage, BandDetailPage, LoginPage, SignupPage)
│   │   ├── pages/tabs/  (MembersTab, SongsTab, RehearsalsTab)
│   │   └── store/       (authStore.ts — Zustand persist)
│   ├── nginx.conf                           # SPA 라우팅 + /api 프록시
│   └── Dockerfile                           # node build → nginx
├── Dockerfile                               # Spring Boot 빌드 이미지
├── docker-compose.yml                       # 로컬 개발 (백엔드 + MySQL)
└── docker-compose.prod.yml                  # 프로덕션 (백엔드 + MySQL + nginx)
```

---

## 실행 방법

### 로컬 개발 (백엔드 + MySQL)

```bash
git clone https://github.com/Realangel0819/bandmate.git
cd bandmate

# 백엔드 + MySQL 실행
docker compose up -d --build

# 프론트엔드 개발 서버 (별도 터미널)
cd frontend
npm install
npm run dev
# → http://localhost:5173  (Vite가 /api를 localhost:8080으로 프록시)
```

### 전체 프로덕션 빌드 테스트

```bash
# .env 파일 생성
cat > .env << 'EOF'
JWT_SECRET=your-secret-key-must-be-32-chars-minimum
DB_USERNAME=bandmate
DB_PASSWORD=bandmate123
MYSQL_ROOT_PASSWORD=root_change_me
EOF

# 빌드 및 실행 (백엔드 + MySQL + nginx + 프론트엔드 빌드 포함)
docker compose -f docker-compose.prod.yml up -d --build
# → http://localhost
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
[Spring Boot :8080]
    │  JDBC
    ▼
[MySQL 8.0]
```

단일 EC2 인스턴스에서 Docker Compose로 3개 컨테이너를 운영합니다.  
nginx가 80포트를 단독 점유하며 프론트엔드 정적 파일 서빙과 백엔드 API 역방향 프록시를 동시에 처리합니다.

### EC2 배포 순서

```bash
# 1. EC2 접속 후 Docker 설치
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker ubuntu && newgrp docker
sudo apt-get install -y git

# 2. 클론 및 환경 변수 설정
git clone https://github.com/Realangel0819/bandmate.git
cd bandmate
cat > .env << 'EOF'
JWT_SECRET=배포용-시크릿키-32자-이상
DB_USERNAME=bandmate
DB_PASSWORD=bandmate123
MYSQL_ROOT_PASSWORD=root_change_me
EOF

# 3. 실행 (첫 실행 시 빌드 포함 5~10분 소요)
docker compose -f docker-compose.prod.yml up -d --build

# 4. 확인
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs app --tail 30
```

EC2 보안 그룹: **인바운드 22(SSH), 80(HTTP)** 오픈 필요.
