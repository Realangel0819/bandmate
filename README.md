# 🎸 BandMate

> 밴드 팀원 모집부터 합주 일정 관리까지 — 밴드 활동을 위한 협업 플랫폼 REST API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Tests](https://img.shields.io/badge/Tests-52_passing-success)

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [ERD](#erd)
- [API 명세](#api-명세)
- [주요 구현 포인트](#주요-구현-포인트)
- [실행 방법](#실행-방법)
- [배포](#배포)
- [프론트엔드 가이드라인](#프론트엔드-가이드라인)
- [프로젝트 구조](#프로젝트-구조)

---

## 프로젝트 소개

BandMate는 밴드 팀원 모집, 포지션별 지원/승인, 공연곡 투표, 합주 일정 관리를 지원하는 백엔드 REST API 서버입니다.

| 기능 | 설명 |
|------|------|
| 밴드 모집 | 포지션별 정원 설정 · 지원 · 승인/거절 |
| 공연곡 선정 | 곡 후보 등록 → 멤버 투표 → 자동/수동 선정 |
| 합주 일정 | 일정 생성 · 참여 신청 · 동시성 안전 정원 관리 |

**개발 목표:** 정원 제한, 중복 방지, 비관적 락 동시성 처리, 글로벌 예외 처리, JPA Entity 매핑 등 실무 백엔드 구현 포인트를 직접 다루는 프로젝트

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| ORM | Spring Data JPA (Hibernate 6) |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Validation | Spring Validation (`@Valid`, `@NotBlank`, `@Email`) |
| Database | H2 (개발/테스트) · MySQL 8.0 (운영) |
| Build | Gradle |
| Test | JUnit 5 · Mockito · MockMvc (52 tests) |
| Deploy | Docker · AWS EC2 · Amazon ECR · Amazon RDS |

---

## 주요 기능

### 👤 사용자 인증
- 회원가입 (이메일 중복 방지, 닉네임 유일성 보장)
- 로그인 → JWT 발급 (유효기간 24시간)
- 모든 인증 필요 API: `Authorization: Bearer {token}`

### 🎸 밴드 모집 시스템
- 밴드 생성 (생성자 = 리더, 자동으로 VOCAL 포지션 멤버 등록)
- 포지션별 모집 공고 등록 (포지션 중복 허용 — 기타 2명 등 가능)
- 멤버 지원 → 리더 승인/거절
- 정원 초과 지원 차단 · 중복 지원 방지 · 기존 멤버 재지원 차단
- 밴드 소프트 삭제 (`deleted_at` 기반, 삭제된 밴드 자동 필터링)

### 🎵 공연곡 선정 시스템
- 글로벌 곡 카탈로그에 곡 등록 (제목+아티스트 중복 방지)
- 리더가 후보곡 추가 + 투표 기간 설정
- **1인 1표** — 같은 밴드에서 어떤 후보에도 한 번만 투표 가능
- 모든 멤버 투표 완료 시 최다 득표곡 **자동 선정**
- 투표 종료 후 리더가 수동 선정도 가능

### 📅 합주 일정 관리
- 리더가 일정 생성 (제목, 날짜, 장소, 정원)
- 밴드 멤버만 참여 신청/취소 가능
- **동시성 처리** — 비관적 락으로 정원 초과 완전 차단

### 🛡️ 공통
- 글로벌 예외 처리 (`@RestControllerAdvice`) — 404/403/409/400/500 명확히 분리
- DTO 입력 검증 (`@Valid`) — 빈 값, 이메일 형식, 최소 길이 등 검증

---

## ERD

```
users
 ├── id (PK)
 ├── email        UNIQUE
 ├── nickname     UNIQUE
 └── password

band
 ├── id (PK)
 ├── leader_id    FK → users.id
 └── deleted_at   (soft delete)

band_member
 ├── (band_id, user_id)  UNIQUE
 └── position     ENUM

band_recruit
 └── band_id      FK → band.id

band_application
 ├── (band_id, user_id)  UNIQUE
 └── status       ENUM  PENDING|APPROVED|REJECTED

song
 └── (title, artist)     UNIQUE

band_song
 ├── band_id, song_id    FK
 └── is_selected  BOOLEAN

song_vote
 └── (band_song_id, user_id)  UNIQUE

rehearsal
 └── band_id      FK → band.id

rehearsal_attendance
 └── (rehearsal_id, user_id)  UNIQUE
```

### 주요 인덱스

| 테이블 | 인덱스 | 용도 |
|--------|--------|------|
| band | `leader_id` | 내 밴드 목록 조회 |
| band_member | `band_id`, `user_id` | 멤버 조회 |
| band_application | `(recruit_id, status)` | 승인 인원 집계 |
| band_song | `(band_id, is_selected)` | 선정곡 조회 |
| rehearsal | `rehearsal_date` | 날짜 기반 조회 |

---

## API 명세

### 인증 `POST /api/users`

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
| POST | `/` | 로그인 | 밴드 생성 |
| GET | `/{bandId}` | - | 밴드 조회 |
| GET | `/my-bands` | 로그인 | 내 밴드 목록 |
| DELETE | `/{bandId}` | 리더 | 밴드 삭제 (soft) |
| POST | `/{bandId}/recruits` | 리더 | 모집 공고 등록 |
| POST | `/{bandId}/apply` | 로그인 | 지원 |
| PUT | `/{bandId}/applications/{id}/approve` | 리더 | 지원 승인 |
| PUT | `/{bandId}/applications/{id}/reject` | 리더 | 지원 거절 |
| GET | `/{bandId}/applications` | 리더 | 지원서 목록 |

```json
// POST /api/bands
{ "name": "SilverRock", "description": "록밴드 모집 중" }

// POST /api/bands/1/recruits
{ "position": "GUITAR", "requiredCount": 2 }

// POST /api/bands/1/apply
{ "recruitId": 1, "position": "GUITAR" }
```

**포지션:** `VOCAL` · `GUITAR` · `BASS` · `DRUM` · `KEYBOARD` · `PERCUSSION`

---

### 공연곡 `/api/bands/{bandId}/songs`

| Method | URL | 권한 | 설명 |
|--------|-----|------|------|
| POST | `/` | - | 곡 등록 (글로벌) |
| POST | `/candidates` | 리더 | 후보곡 추가 + 투표 기간 |
| POST | `/vote` | 로그인 | 투표 |
| PUT | `/{bandSongId}/select` | 리더 | 수동 곡 선정 |
| GET | `/` | - | 전체 후보곡 |
| GET | `/active` | - | 투표 진행 중인 후보곡 |
| GET | `/selected` | - | 선정된 곡 |

```json
// POST /api/bands/1/songs
{ "title": "Bohemian Rhapsody", "artist": "Queen", "youtubeUrl": "https://youtube.com/..." }

// POST /api/bands/1/songs/candidates
{ "songId": 1, "voteStartDate": "2024-01-01T00:00:00", "voteEndDate": "2024-01-07T23:59:59" }

// POST /api/bands/1/songs/vote
{ "bandSongId": 1 }
// → "모든 멤버가 투표를 완료했습니다. 최다 득표곡이 자동으로 선정되었습니다."
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
| GET | `/{rehearsalId}/attendances` | 리더 | 참여자 목록 |

```json
// POST /api/bands/1/rehearsals
{
  "title": "정기 합주",
  "description": "주간 합주",
  "rehearsalDate": "2024-02-01T14:00:00",
  "location": "홍대 연습실",
  "maxCapacity": 5
}
```

---

### 에러 응답 형식

```json
// 404 Not Found
{ "status": 404, "message": "밴드를 찾을 수 없습니다.", "timestamp": "2024-01-01T12:00:00" }

// 409 Conflict
{ "status": 409, "message": "이미 이 밴드에 지원했습니다.", "timestamp": "..." }

// 400 Bad Request (validation)
{ "status": 400, "message": "email: 올바른 이메일 형식이 아닙니다.", "timestamp": "..." }
```

---

## 주요 구현 포인트

### 1. 동시성 처리 — 합주 정원 초과 방지

정원 5명인 합주에 10명이 동시에 신청하면 정확히 5명만 성공해야 합니다.

```java
// RehearsalRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT r FROM Rehearsal r WHERE r.id = :id")
Optional<Rehearsal> findByIdWithLock(@Param("id") Long id);

// RehearsalService.joinRehearsal()
Rehearsal rehearsal = rehearsalRepository.findByIdWithLock(rehearsalId); // SELECT FOR UPDATE
if (rehearsal.getCurrentCount() >= rehearsal.getMaxCapacity()) {
    throw new InvalidRequestException("정원이 초과되었습니다.");
}
attendanceRepository.save(attendance);
rehearsal.setCurrentCount(rehearsal.getCurrentCount() + 1); // 락 해제는 트랜잭션 커밋 시
```

낙관적 락 대신 비관적 락을 선택한 이유: 합주 신청은 마감 직전 요청이 몰리는 패턴이므로 충돌 빈도가 높아 재시도 비용이 더 큼.

테스트: 10개 스레드 동시 신청 → `successCount == 5`, `currentCount == 5` 검증 (`CountDownLatch` 활용)

---

### 2. 투표 자동 마감 — 1인 1표 + 전원 투표 시 자동 선정

```java
// 밴드 전체 기준 1인 1표
if (songVoteRepository.countByBandIdAndUserId(bandId, userId) > 0) {
    throw new DuplicateException("이미 투표했습니다.");
}

// 전원 투표 완료 시 자동 선정
int totalVotes = songVoteRepository.countByBandId(bandId);
int totalMembers = bandMemberRepository.countByBandId(bandId);
if (totalVotes >= totalMembers) {
    // 최다 득표 후보 → isSelected = true
}
```

---

### 3. Soft Delete

밴드 삭제 시 DB에서 실제로 지우지 않고 `deleted_at` 타임스탬프를 기록합니다.

```java
@SQLRestriction("deleted_at IS NULL")  // 모든 조회에서 자동 필터링
public class Band {
    private LocalDateTime deletedAt;

    public void softDelete() { this.deletedAt = LocalDateTime.now(); }
}
```

`@SQLRestriction` 덕분에 `bandRepository.findAll()`, `findById()` 등 모든 조회에서 삭제된 밴드는 자동으로 제외됩니다.

---

### 4. 글로벌 예외 처리

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)      // → 404
    @ExceptionHandler(UnauthorizedException.class)  // → 403
    @ExceptionHandler(DuplicateException.class)     // → 409
    @ExceptionHandler(InvalidRequestException.class)// → 400
    @ExceptionHandler(MethodArgumentNotValidException.class) // @Valid 실패 → 400
}
```

---

### 5. JPA Entity 매핑

Long ID 필드를 유지하면서 `@ManyToOne` 네비게이션 속성을 추가합니다 (읽기 전용, 기존 코드 영향 없음).

```java
@Column(name = "band_id", nullable = false)
private Long bandId;                              // 쓰기용 (기존)

@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "band_id", insertable = false, updatable = false)
@ToString.Exclude
private Band band;                                // 네비게이션용 (신규)
```

이 패턴으로 `member.getBand().getName()` 같은 JPA 네비게이션이 가능하면서, 기존 서비스·레포지토리 코드는 변경 없이 유지됩니다.

---

## 실행 방법

### 로컬 개발 (H2 In-memory)

```bash
git clone https://github.com/your-repo/bandmate.git
cd bandmate
./gradlew bootRun
```

- 서버: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:bandmatedb`, Username: `sa`)

### Docker Compose (MySQL 포함 로컬 테스트)

```bash
docker compose up --build
```

- MySQL + Spring Boot 동시 실행
- 최초 실행 시 `schema.sql` 자동 적용

### 테스트

```bash
./gradlew test
# 52 tests: 단위 테스트 + 통합 테스트(H2) + 동시성 테스트
```

---

## 배포

### 아키텍처

```
GitHub push → GitHub Actions
               ├── [test]   52개 테스트 실행
               └── [deploy] Docker 빌드 → ECR push → EC2 배포
```

### 필요한 AWS 리소스

| 리소스 | 용도 | 예상 비용 |
|--------|------|----------|
| EC2 t3.small | 앱 서버 | ~$15/월 |
| RDS MySQL db.t3.micro | DB | ~$15/월 |
| Amazon ECR | Docker 이미지 저장소 | ~$1/월 |

### GitHub Secrets 설정

```
AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
EC2_HOST / EC2_SSH_KEY
DB_HOST / DB_USERNAME / DB_PASSWORD
JWT_SECRET  (32자 이상)
```

상세 배포 절차는 [.env.example](.env.example) 참고.

---

## 프론트엔드 가이드라인

### 권장 스택

| 항목 | 권장 | 이유 |
|------|------|------|
| 프레임워크 | React 18 + TypeScript | SPA, 타입 안전성 |
| 상태 관리 | Zustand 또는 React Query | 서버 상태는 React Query, 클라이언트 상태는 Zustand |
| HTTP 클라이언트 | Axios | 인터셉터로 JWT 자동 첨부 |
| UI | Tailwind CSS + shadcn/ui | 빠른 개발 |
| 라우팅 | React Router v6 | |

### 인증 처리

```typescript
// Axios 인터셉터로 JWT 자동 첨부
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// 401 응답 시 로그인 페이지로 리다이렉트
axios.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) navigate('/login');
    return Promise.reject(err);
  }
);
```

### 에러 처리

```typescript
// GlobalExceptionHandler가 내려주는 형식에 맞춰 처리
interface ApiError { status: number; message: string; }

catch (err) {
  const error = err as AxiosError<ApiError>;
  if (error.response?.data?.message) {
    toast.error(error.response.data.message); // "이미 이 밴드에 지원했습니다." 등
  }
}
```

### 주요 페이지 구성 (예시)

```
/                   메인 (밴드 목록)
/login              로그인
/signup             회원가입
/bands/:bandId      밴드 상세 (멤버, 모집 공고, 합주 일정)
/bands/:bandId/songs   공연곡 투표
/bands/create       밴드 생성
```

### CORS 설정 필요 사항

Spring Boot에 아래 설정 추가 필요:

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(List.of("http://localhost:3000", "https://your-domain.com"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    // ...
}
```

---

## 프로젝트 구조

```
src/
├── main/java/com/bandmate/
│   ├── BandmateApplication.java
│   ├── common/
│   │   ├── exception/           # GlobalExceptionHandler + 커스텀 예외 4종
│   │   └── util/JwtUtil.java
│   ├── config/SecurityConfig.java
│   ├── user/                    # 회원가입 · 로그인
│   ├── band/                    # 밴드 · 모집 · 지원
│   ├── song/                    # 공연곡 · 투표
│   └── rehearsal/               # 합주 일정 (비관적 락)
├── main/resources/
│   ├── application.yml          # 개발 (H2)
│   ├── application-prod.yml     # 운영 (MySQL)
│   └── db/schema.sql            # MySQL DDL
└── test/                        # 52개 테스트
    ├── 단위 테스트 (Mockito)
    ├── 통합 테스트 (H2 + @Transactional 롤백)
    └── 동시성 테스트 (CountDownLatch 10 스레드)
```
