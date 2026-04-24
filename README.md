# 🎸 BandMate

밴드 팀원 모집부터 공연곡 선정까지, 밴드 활동을 위한 협업 플랫폼 REST API 서버

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [기술 스택](#기술-스택)
- [핵심 기능](#핵심-기능)
- [도메인 구조](#도메인-구조)
- [ERD](#erd)
- [API 명세](#api-명세)
- [주요 구현 포인트](#주요-구현-포인트)
- [실행 방법](#실행-방법)

---

## 프로젝트 소개

BandMate는 밴드 팀원 모집, 포지션별 지원/승인, 공연곡 투표 선정을 지원하는 백엔드 API 서버입니다.

| 기능 | 설명 |
|------|------|
| 밴드 관리 | 밴드 생성 및 조회, 리더 중심의 팀 운영 |
| 포지션 모집 | 드럼·기타·보컬 등 포지션별 정원 설정 및 지원 관리 |
| 공연곡 선정 | 곡 후보 등록 → 멤버 투표 → 최다 득표 곡 선정 |

> **개발 목표**: 정원 제한·중복 방지 로직, 투표 시스템, 동시성 처리 등 실무에서 자주 마주치는 백엔드 구현 포인트를 직접 다뤄보는 프로젝트

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.5 |
| ORM | Spring Data JPA (Hibernate) |
| Security | Spring Security + JWT (jjwt 0.12.5) |
| Database | H2 (개발), MySQL (운영) |
| Build | Gradle |
| Test | JUnit 5, Spring Security Test |

---

## 핵심 기능

### 🎸 밴드 모집 시스템

- 밴드 생성 (생성자 = 리더)
- 포지션별 모집 공고 등록 (예: 드럼 1명, 기타 2명)
- 멤버 지원 → 리더만 승인/거절 가능
- **정원 초과 지원 차단** — 승인 시점에 `requiredCount` 대비 `currentCount` 검증
- **중복 지원 방지** — 동일 밴드에 이미 지원한 유저는 재지원 불가, 이미 멤버인 경우도 차단

### 🎵 공연곡 선정 시스템 (핵심 차별화)

- 리더가 곡 후보 등록 (유튜브 링크 포함), 투표 기간(시작일/종료일) 설정
- 밴드 멤버들이 후보곡에 투표
- **1인 1표 제한** — `(bandSongId, userId)` unique 제약으로 중복 투표 방지
- 투표 종료 후 리더가 최다 득표 곡을 최종 선정 (`isSelected = true`)
- 현재 활성 후보 / 선정된 곡 별도 조회 API 제공

### 📅 합주 일정 관리 (예정)

- 리더의 일정 생성 및 참여 인원 정원 설정
- 멤버 참여 신청 — 동시 요청 시 **정원 초과 방지** (동시성 처리 포인트)

---

## 도메인 구조

```
com.bandmate
├── user/           # 회원가입 · 로그인 (JWT 인증)
├── band/           # 밴드 · 모집 · 지원 관리
├── song/           # 곡 등록 · 투표 · 선정
├── common/
│   └── util/       # JwtUtil
└── config/         # SecurityConfig
```

---

## ERD

```
User (1) ──── (n) Band              [리더 관계]
Band (1) ──── (n) BandMember        [멤버 목록]
Band (1) ──── (n) BandRecruit       [포지션별 모집 공고]
BandRecruit (1) ─ (n) BandApplication  [지원서]
Band (1) ──── (n) BandSong          [곡 후보]
BandSong (1) ─ (n) SongVote         [투표 기록]
Song (1) ──── (n) BandSong          [글로벌 곡 카탈로그]
```

### 주요 제약

| 테이블 | Unique 제약 |
|--------|------------|
| BandMember | (bandId, userId) |
| BandRecruit | (bandId, position) |
| BandApplication | (bandId, userId) |
| SongVote | (bandSongId, userId) |

---

## API 명세

### 인증 (`/api/users`)

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/signup` | - | 회원가입 |
| POST | `/login` | - | 로그인 → JWT 반환 |

<details>
<summary>Request / Response 예시</summary>

**POST /api/users/signup**
```json
// Request
{ "email": "user@example.com", "password": "pass1234", "nickname": "기타리스트" }

// Response
"회원가입 성공"
```

**POST /api/users/login**
```json
// Request
{ "email": "user@example.com", "password": "pass1234" }

// Response
{ "token": "eyJ...", "userId": 1, "email": "user@example.com", "nickname": "기타리스트" }
```
</details>

---

### 밴드 (`/api/bands`)

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/` | 필요 | 밴드 생성 |
| GET | `/{bandId}` | - | 밴드 조회 |
| GET | `/my-bands` | 필요 | 내가 리더인 밴드 목록 |
| POST | `/{bandId}/recruits` | 리더만 | 모집 공고 등록 |
| POST | `/{bandId}/apply` | 필요 | 지원 |
| PUT | `/{bandId}/applications/{appId}/approve` | 리더만 | 지원 승인 |
| PUT | `/{bandId}/applications/{appId}/reject` | 리더만 | 지원 거절 |
| GET | `/{bandId}/applications` | 리더만 | 지원서 목록 조회 |

<details>
<summary>Request / Response 예시</summary>

**POST /api/bands/**
```json
// Request
{ "name": "SilverRock", "description": "록밴드 모집 중" }

// Response
{ "id": 1, "name": "SilverRock", "leaderId": 1, "memberCount": 1, ... }
```

**POST /api/bands/1/recruits**
```json
// Request
{ "bandId": 1, "position": "GUITAR", "requiredCount": 2 }
```

**POST /api/bands/1/apply**
```json
// Request
{ "recruitId": 1, "position": "GUITAR" }
```
</details>

**포지션 목록**: `VOCAL` · `GUITAR` · `BASS` · `DRUM` · `KEYBOARD` · `PERCUSSION`

---

### 공연곡 (`/api/bands/{bandId}/songs`)

| Method | URL | Auth | 설명 |
|--------|-----|------|------|
| POST | `/` | - | 곡 등록 (글로벌 카탈로그) |
| POST | `/candidates` | 리더만 | 후보곡 추가 + 투표 기간 설정 |
| POST | `/vote` | 필요 | 투표 |
| PUT | `/{bandSongId}/select` | 리더만 | 최종 곡 선정 |
| GET | `/` | - | 전체 후보곡 목록 |
| GET | `/active` | - | 투표 진행 중인 후보곡 |
| GET | `/selected` | - | 선정된 곡 |

<details>
<summary>Request / Response 예시</summary>

**POST /api/bands/1/songs/candidates**
```json
// Request
{ "songId": 1, "voteStartDate": "2024-01-01", "voteEndDate": "2024-01-07" }
```

**POST /api/bands/1/songs/vote**
```json
// Request
{ "bandSongId": 1 }

// Response
{ "id": 1, "bandSongId": 1, "userId": 2, "message": "투표 완료" }
```
</details>

---

## 주요 구현 포인트

### 1. 정원 제한 + 중복 지원 방지

```java
// BandService.approveApplication()
// 1) 이미 정원 초과 여부 확인
int approvedCount = applicationRepository.countByRecruitIdAndStatus(recruitId, APPROVED);
if (approvedCount >= recruit.getRequiredCount()) {
    throw new IllegalStateException("정원이 초과되었습니다.");
}

// 2) 이미 멤버인지 확인
if (memberRepository.findByBandIdAndUserId(bandId, userId).isPresent()) {
    throw new IllegalStateException("이미 밴드 멤버입니다.");
}
```

- `BandApplication`에 `(bandId, userId)` unique 제약 → DB 레벨 중복 방지
- 승인 시 현재 승인 인원 재조회 → 동시 승인 시 정원 초과 방어

### 2. 투표 시스템 — 1인 1표 + 기간 검증

```java
// SongService.vote()
// 1) 투표 기간 확인
if (!bandSong.isVotingActive()) {
    throw new IllegalStateException("투표 기간이 아닙니다.");
}

// 2) 중복 투표 차단
if (voteRepository.findByBandSongIdAndUserId(bandSongId, userId).isPresent()) {
    throw new IllegalStateException("이미 투표했습니다.");
}
```

- `SongVote`에 `(bandSongId, userId)` unique 제약 → DB 레벨 중복 방지
- `BandSong.isVotingActive()` — 현재 날짜가 `voteStartDate ~ voteEndDate` 사이인지 검증

### 3. 동시성 처리 포인트 (합주 일정 예정)

합주 일정 참여 신청 API 구현 시 다수 유저가 동시에 마지막 자리에 신청하는 경우를 처리해야 합니다.

- **낙관적 락(Optimistic Lock)**: 충돌이 드문 경우, `@Version` 필드로 재시도 처리
- **비관적 락(Pessimistic Lock)**: 충돌이 잦은 경우, `@Lock(PESSIMISTIC_WRITE)`로 row-level lock
- **DB unique 제약**: 중복 참여 자체는 `(scheduleId, userId)` 제약으로 방어

---

## 실행 방법

### 요구사항

- Java 17+
- Gradle (Wrapper 포함)

### 개발 환경 실행 (H2 In-memory)

```bash
git clone https://github.com/your-repo/bandmate.git
cd bandmate

./gradlew bootRun
```

- 서버: `http://localhost:8080`
- H2 콘솔: `http://localhost:8080/h2-console`
  - JDBC URL: `jdbc:h2:mem:bandmatedb`
  - Username: `sa` / Password: (없음)

### 운영 환경 실행 (MySQL)

`application.yml`의 데이터소스를 아래와 같이 변경:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/bandmatedb?serverTimezone=UTC
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: your_username
    password: your_password
  jpa:
    hibernate:
      ddl-auto: validate
```

### 테스트 실행

```bash
./gradlew test
```

---

## 인증 방식

모든 인증 필요 API는 요청 헤더에 JWT를 포함:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

- 토큰 유효기간: 24시간
- 알고리즘: HMAC-SHA256

---

## 프로젝트 구조

```
src/main/java/com/bandmate/
├── BandmateApplication.java
├── user/
│   ├── controller/UserController.java
│   ├── service/UserService.java
│   ├── entity/User.java
│   ├── dto/  (SignupRequest, LoginRequest, LoginResponse)
│   └── repository/UserRepository.java
├── band/
│   ├── controller/BandController.java
│   ├── service/BandService.java
│   ├── entity/ (Band, BandMember, BandRecruit, BandApplication, Position)
│   ├── dto/   (CreateBandRequest, BandResponse, ...)
│   └── repository/ (BandRepository, BandMemberRepository, ...)
├── song/
│   ├── controller/SongController.java
│   ├── service/SongService.java
│   ├── entity/ (Song, BandSong, SongVote)
│   ├── dto/   (CreateSongRequest, VoteRequest, BandSongResponse, ...)
│   └── repository/ (SongRepository, BandSongRepository, SongVoteRepository)
├── common/util/JwtUtil.java
└── config/SecurityConfig.java
```