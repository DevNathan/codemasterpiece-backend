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
