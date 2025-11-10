# ---------- BUILD STAGE ----------
FROM amazoncorretto:21-alpine3.20 AS build
WORKDIR /workspace

# 필수 빌드 도구
RUN apk add --no-cache bash curl tar

# Gradle Wrapper & 캐시
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# 설정 복사 및 의존성 프리페치
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---------- RUNTIME STAGE ----------
FROM amazoncorretto:21-alpine3.20
WORKDIR /app

# 타임존
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 이미지 처리 도구 설치
RUN set -eux; \
    apk add --no-cache \
      libwebp-tools \
      libavif-apps \
      imagemagick

# 빌드타임에서 툴 정상 확인
RUN echo "Checking tools..." && \
    which cwebp && cwebp -version && \
    which avifenc && avifenc -V && \
    echo "All image tools are available."

# 빌드 산출물
COPY --from=build /workspace/build/libs/*.jar app.jar

# JVM 최적화 및 서버 포트
ENV SERVER_PORT=10100
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

EXPOSE 10100

# exec form로 신호 전달 확실히
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar app.jar"]
