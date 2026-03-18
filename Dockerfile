# ---------- RUNTIME STAGE ----------
FROM amazoncorretto:21-alpine3.20
WORKDIR /app

ENV TZ=Asia/Seoul
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

RUN set -eux; \
    apk add --no-cache \
      libwebp-tools \
      libavif-apps \
      imagemagick

RUN echo "Checking tools..." && \
    which cwebp && cwebp -version && \
    which avifenc && avifenc -V && \
    echo "All image tools are available."

COPY build/libs/*.jar app.jar
ENV SERVER_PORT=10100
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:MaxMetaspaceSize=256m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"

EXPOSE 10100

ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar app.jar"]