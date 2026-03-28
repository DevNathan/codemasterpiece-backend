# Changelog

## 1.1.1 - 2026-03-28
### Fixed
- application.yml에 gitlab 프로바이더 명시 추가

## 1.1.0 - 2026-03-28
### Added
- GitLab OAuth2 로그인 연동 추가 (다중 소셜 프로바이더 지원)
- Spring Boot Actuator 도입으로 표준화된 시스템 헬스체크(/actuator/health) 구현 및 기존 수동 핑 컨트롤러 제거
- 관리자(AUTHOR) 전용 수동 가비지 컬렉션(GC) 및 레디스 조회수 키 정리 API 추가

### Changed
- **[Architectural Overhaul]** 계층형 아키텍처를 파괴하고 도메인 주도 패키징(core, media, ref, variant)으로 전면 재편성
- **[File Engine Refactoring]** OOM 방지를 위해 바이트 배열 방식에서 로컬 임시 파일 기반 스트리밍 I/O 파이프라인으로 완전 개조
- **[Variant Pipeline]** 트랜잭션 훅 기반의 `VariantDispatcher`와 Enum 프리셋을 도입하여 비동기 이미지 변환 로직의 응집도 극대화
- **[Request Unification]** 파편화된 DTO들을 도메인별 통합 Command/Request 레코드(`PostRequest`, `CategoryRequest` 등)로 응집시켜 코드 부채 청소
- `ImageV1Controller` 엔드포인트 수정: 클라이언트가 직접 변환 규격(Preset)을 지정할 수 있도록 파라미터화
- 방명록 및 댓글 도메인의 `Actor` 식별 로직과 프로필 데이터(Command) 분리 구조 개선
- 소셜 프로바이더 확장을 고려한 `ensureModifiable` 권한 검증 로직 일반화 적용
- 프론트엔드 렌더링 최적화를 위해 64x64 리사이징된 아바타 URL(`avatarUrlSmall`)을 백엔드에서 사전 생성하도록 개선

### Fixed
- **[Performance]** `FileRefService`의 N+1 쿼리 지옥을 배치 처리 및 메모리 내 Diff 알고리즘으로 해결하여 DB 커넥션 부하 제거
- **[Stability]** `FileHousekeepingService`에서 불필요한 트랜잭션을 제거하여 S3 I/O 대기 시 DB 커넥션 풀 고갈 현상 원천 차단
- 익명(ANON) 사용자의 댓글 및 방명록 삭제 시 403 Forbidden 예외 발생 버그 수정

## 1.0.11 - 2026-03-27
### Fixed
- 프리뷰 마크다운 파싱 페이로드 미스매칭 수정

## 1.0.10 - 2026-03-27
### Fixed
- 이미지 토큰 URL 변환 로직 미반영된 버그 수정

## 1.0.9
### Added
- 서버 사이드 마크다운 파서 및 보안 필터링 도입
- 댓글 수정을 위한 원본 데이터 조회(/raw) 엔드포인트 추가
- 게시글 파싱 결과 및 TOC 서버 메모리 캐싱 시스템 도입

## 1.0.8
### Added
- Support for Oracle Cloud ARM architecture

## 1.0.7
### Updated
- CORS 문제로 https://codemasterpiece.com 도메인 추가