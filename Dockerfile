# ---------- BUILD STAGE ----------
FROM amazoncorretto:21-alpine3.20 AS build
WORKDIR /workspace

# 필수 빌드 도구
RUN apk add --no-cache bash curl tar

# Gradle Wrapper 복사
COPY gradlew ./
COPY gradle ./gradle
RUN chmod +x gradlew

# Gradle 설정 복사 및 의존성 캐시
COPY build.gradle settings.gradle ./
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# 소스 복사 후 빌드
COPY src ./src
RUN ./gradlew --no-daemon clean bootJar

# ---------- RUNTIME STAGE ----------
FROM amazoncorretto:21-alpine3.20
WORKDIR /app

# 타임존 설정
ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 빌드 산출물 복사
COPY --from=build /workspace/build/libs/*.jar app.jar

# JVM 최적화
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

EXPOSE 10100

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
