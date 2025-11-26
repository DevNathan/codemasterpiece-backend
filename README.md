# Code masterpiece (Backend)

[![Java](https://img.shields.io/badge/Java_21_(LTS)-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

> 확장 가능한 아키텍처와 데이터 무결성을 최우선으로 고려한, 모던 웹 애플리케이션을 위한 RESTful API 서버입니다.

🔗 [Code Masterpiece](https://www.codemasterpiece.com)

🔗 [Frontend Repo](https://github.com/DevNathan/codemasterpiece-front)

---

### 목차

1. 개발 주안점

<br>

## 1. 개발 주안점
단순한 기능 구현을 넘어, **사용자 데이터 보호, 시스템 성능, 그리고 운영 안정성**의 3박자를 갖춘 엔지니어링을 목표로 했습니다.

### 1) Privacy-First: 사용자 신뢰를 위한 보안 아키텍처
개인 블로그 서비스가 가질 수 있는 보안 우려를 불식시키기 위해, **'데이터 최소 수집'**과 **'엔터프라이즈급 보안 적용'**을 원칙으로 삼았습니다.

- **Zero-Knowledge 인증 전략 (GitHub OAuth2)**
  > 자체 회원가입 기능을 과감히 배제하고 **GitHub OAuth2** 단일 인증을 채택했습니다. 민감한 개인정보(비밀번호, 이메일 등)를 DB에 저장하지 않는 **Privacy-First** 설계를 통해, 해킹이나 유출 사고의 위험을 원천 차단했습니다.

- **Spring Security Hardening**
  > 프레임워크의 기본 설정에 안주하지 않고 보안을 강화했습니다. **세션 고정 보호(Session Fixation Protection)**로 하이재킹을 방지하고, **엄격한 CORS 정책**으로 신뢰할 수 있는 오리진만 허용했습니다. 또한, 인증 후 사용자가 보던 페이지로 정확히 돌아오도록 리다이렉트 로직(`RuriSupport`)을 커스터마이징하여 보안과 UX의 균형을 맞췄습니다.

### 2) 표준화된 응답 아키텍처와 생산성 향상 (Response Standardization)
프론트엔드와의 협업 효율을 극대화하고 백엔드 비즈니스 로직에만 집중할 수 있는 환경을 만들기 위해, **API 응답의 횡단 관심사(Cross-Cutting Concerns)를 AOP 수준에서 처리**했습니다.

* **Global Response Wrapping (Auto-Wrapping)**
  > 모든 컨트롤러 응답을 가로채어(Intercept) 표준 포맷(`SuccessResponse`)으로 자동 변환하는 `ResponseBodyAdvice`를 구현했습니다.
  > 개발자는 컨트롤러 단에서 `return "ok"`나 `return data`와 같이 핵심 데이터만 반환하면 되며, 상태 코드/메시지/트레이스 ID 등이 포함된 표준 응답 껍데기를 신경 쓸 필요가 없어 **보일러플레이트 코드를 100% 제거**했습니다.

* **Centralized Exception Handling**
  > 애플리케이션 전역에서 발생하는 예외를 `GlobalExceptionHandler` 한 곳에서 포착하여 표준 에러 포맷(`ErrorResponse`)으로 변환합니다.
  > 특히 `MethodArgumentNotValidException`과 같은 검증(Validation) 에러를 자동으로 파싱하여 필드별 에러 메시지로 가공함으로써, 프론트엔드가 별도의 파싱 로직 없이 즉시 UI에 반영할 수 있도록 설계했습니다.

* **Trace ID Integration**
  > 마이크로서비스 환경이나 분산 로그 추적을 대비하여, 요청 시점부터 응답까지 이어지는 고유한 `Trace ID`를 모든 성공/실패 응답에 자동으로 주입합니다. 이는 운영 단계에서 이슈 추적(Troubleshooting) 시간을 획기적으로 단축시킵니다.
  
### 3) 엄격한 계층 분리와 Command 패턴 (Layered Architecture)
소프트웨어의 복잡도를 낮추고 유지보수성을 높이기 위해, 각 계층의 **책임과 역할**을 명확히 규정하고 의존성 방향을 철저히 관리했습니다.

* **Layered Responsibility (역할의 격리)**
  > **Controller**: 웹 요청의 진입점으로, 엔드포인트 정의, 파라미터 바인딩, 1차적인 권한 검사(Security Context)에만 집중합니다. 비즈니스 로직을 포함하지 않습니다.<br>
  > **Service**: 순수한 비즈니스 로직의 집합체입니다. HTTP 의존성(`HttpServletRequest` 등)을 완전히 배제하여 테스트 용이성을 확보했습니다.<br>
  > **Repository/Mapper**: 데이터베이스 접근과 영속성 처리에만 집중합니다.

* **Command Pattern을 통한 데이터 정규화**
  > 컨트롤러의 요청 DTO가 서비스 계층으로 그대로 침투하는 것을 막기 위해 **Command(Cmd) 객체**를 도입했습니다.
  > `Record` 타입을 활용하여 불변성(Immutable)을 보장하며, **생성자 내부에서 데이터 정제(Trim, Null 처리)**를 강제했습니다. 덕분에 서비스 계층은 항상 정제된 '깨끗한 데이터'만을 입력받아 로직 수행에만 집중할 수 있는 구조를 완성했습니다.

---
<br>

## 2. 백엔드 엔지니어링 표준
안정적인 서비스 운영과 데이터 무결성을 보장하기 위해, 각 기술의 장점을 극대화하는 하이브리드 아키텍처를 적용했습니다.

### 1) Hybrid ORM 전략: JPA의 한계를 넘는 실용적 접근
JPA를 메인으로 사용하되, 복잡한 연산이나 특정 데이터베이스 기능이 필요한 경우 MyBatis를 도입하여 **기술적 유연성(Flexibility)**을 확보했습니다.

- **Domain-Centric (JPA)**
  > 대부분의 비즈니스 로직과 일반적인 조회(Read) 및 변경(Write) 작업에는 **JPA**를 사용하여 객체 지향적인 도메인 모델을 유지했습니다. 이를 통해 생산성을 높이고 스키마 변경에 유연하게 대처했습니다.

- **Advanced Querying (MyBatis)**
  > JPA(JPQL)만으로는 표현하기 어려운 **계층형 쿼리(`WITH RECURSIVE`)**나 데이터베이스 고유 함수 활용, 복잡한 통계성 쿼리를 처리하기 위해 **MyBatis**를 병행 사용했습니다.
  > QueryDSL 없이도 복잡한 동적 쿼리를 직관적인 XML로 관리하며, ORM의 추상화 비용을 제거하고 쿼리 튜닝의 제어권을 확보했습니다.
  
### 2) Schema as Code (데이터베이스 형상 관리)
데이터베이스 스키마 변경을 수동으로 관리할 때 발생하는 휴먼 에러와 환경 간 불일치를 방지하기 위해 형상 관리 도구를 필수적으로 적용했습니다.

- **Flyway Migration**
  > 모든 DDL(Create, Alter) 변경 사항을 버전 관리 시스템(Git)으로 추적하고, 애플리케이션 구동 시점에 자동으로 마이그레이션을 수행합니다. 이를 통해 로컬-개발-운영 환경의 데이터베이스 스키마를 **100% 일치**시키고 배포 안정성을 확보했습니다.
  
### 3) 고가용성 파일 시스템 아키텍처 (Enterprise-Grade File System)
확장성과 데이터 정합성을 최우선으로 고려하여, 단순 파일 저장을 넘어선 **독자적인 파일 관리 파이프라인**을 설계했습니다.

- **Storage Abstraction (IoManager 분리)**
  > 파일의 메타데이터 관리(DB)와 물리적 저장소 제어(I/O)의 책임을 엄격히 분리했습니다. `IoManager` 인터페이스를 통해 S3, 로컬 디스크 등 저장소 구현체가 변경되더라도 비즈니스 로직(`FileService`)에는 영향을 주지 않는 **유연한 아키텍처**를 구축했습니다.

- **Transaction-Aware Async Processing**
  > 이미지 리사이징과 같은 무거운 작업은 비동기로 처리하되, 데이터 정합성을 위해 **트랜잭션 커밋(After Commit)** 이후에만 이벤트를 발행하도록 제어했습니다. 이를 통해 "DB에는 데이터가 없는데 변환 작업이 시작되는" 레이스 컨디션을 방지하고, 아웃박스 패턴 도입 전 단계의 실용적인 정합성 모델을 구현했습니다.

- **Reference Counting & Integrity (RefService)**
  > DB 데이터와 실제 파일의 실존 여부를 동기화하기 위해 **참조 카운팅(Reference Counting)** 시스템을 도입했습니다. 게시글, 댓글 등 도메인 엔티티와 파일 간의 연결을 `FileRef`로 관리하여, 고아 파일(Orphan File)을 추적하고 삭제하는 GC(Garbage Collection)의 기반을 마련했습니다.
