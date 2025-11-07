package com.app.codemasterpiecebackend.config.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.awscore.retry.AwsRetryStrategy;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.retries.StandardRetryStrategy;
import software.amazon.awssdk.retries.api.BackoffStrategy;
import software.amazon.awssdk.retries.api.RetryStrategy;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;

/**
 * # AWS S3 구성
 * <p>
 * - S3 SDK v2 기반의 {@link S3Client} 및 {@link S3Presigner} 스프링 빈을 제공한다.
 * - 리트라이는 신식 API({@link RetryStrategy}, {@link StandardRetryStrategy})로 설정한다.
 * - 자격 증명은 명시 키(환경/설정) → 기본 제공 체인(환경변수, 프로파일, EC2/ECS Role) 순으로 해석한다.
 *
 * <h2>특징</h2>
 * <ul>
 *   <li><b>Thread-safe</b>: S3Client/S3Presigner는 애플리케이션 전역에서 재사용 가능.</li>
 *   <li><b>Backoff</b>: half-jitter exponential backoff(권장) 적용.</li>
 *   <li><b>LocalStack/MinIO</b>: endpoint + path-style 옵션으로 호환.</li>
 * </ul>
 *
 * <h2>프로퍼티</h2>
 * 본 클래스는 {@link S3StorageProperties}를 통해 아래 프로퍼티를 주입받는다.
 * <pre>
 * file.s3.bucket, file.s3.region, file.s3.endpoint, file.s3.pathStyle,
 * file.s3.accessKeyId, file.s3.secretAccessKey, file.s3.sessionToken,
 * file.s3.readTimeoutMs, file.s3.maxRetries
 * </pre>
 *
 * <h2>주의</h2>
 * <ul>
 *   <li>멀티파트(5GB 초과 업로드)는 별도 전송 매니저 사용을 고려하라.</li>
 *   <li>Presigner는 AutoCloseable이므로 컨텍스트 종료 시 Spring이 자동 close한다.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(S3StorageProperties.class)
@RequiredArgsConstructor
public class AwsS3Config {

    private final S3StorageProperties props;

    /**
     * S3 동기 클라이언트 빈.
     *
     * <p>특징:
     * <ul>
     *   <li>HTTP 클라이언트: JDK URLConnection (가볍고 의존 적음)</li>
     *   <li>Retry: StandardRetryStrategy + half-jitter backoff</li>
     *   <li>Path-style 및 커스텀 endpoint 지원(LocalStack/MinIO)</li>
     * </ul>
     *
     * @return 구성된 {@link S3Client}
     */
    @Bean
    public S3Client s3Client() {
        // 경량 기본 HTTP 클라이언트
        var http = UrlConnectionHttpClient.builder().build();

        // 재시도 횟수: 시도 1회 + 재시도 N (props.maxRetries는 "재시도" 기준)
        int maxAttempts = Math.max(2, Optional.ofNullable(props.getMaxRetries()).orElse(3) + 1);

        // 신식 backoff: half-jitter exponential (권장: base=100ms, max=20s)
        BackoffStrategy commonBackoff = BackoffStrategy.exponentialDelayHalfJitter(
                Duration.ofMillis(100), Duration.ofSeconds(20));

        // AWS 전용 재시도 조건(스로틀링, 일시 오류 등)을 포함한 표준 전략
        RetryStrategy retryStrategy = AwsRetryStrategy.configure(
                StandardRetryStrategy.builder()
                        .maxAttempts(maxAttempts)
                        .backoffStrategy(commonBackoff)           // 일반 오류 backoff
                        .throttlingBackoffStrategy(commonBackoff) // 스로틀링 backoff
        ).build();

        // API 호출 타임아웃/리트라이 전략
        var override = ClientOverrideConfiguration.builder()
                // 한 번의 시도(재시도 1회 포함)당 타임아웃
                .apiCallAttemptTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
                // 전체 API 호출(모든 재시도 포함) 최대 타임아웃 (보수적 최솟값 가드)
                .apiCallTimeout(Duration.ofMillis(Math.max(props.getReadTimeoutMs(), 10_000)))
                // retryMode는 별도 지정이 없어도 retryStrategy가 우선 적용됨
                .retryStrategy(retryStrategy)
                .build();

        // S3 서비스 별도 구성 (path-style: LocalStack/MinIO에서 필요)
        var s3cfg = S3Configuration.builder()
                .pathStyleAccessEnabled(props.isPathStyle())
                .build();

        var builder = S3Client.builder()
                .httpClient(http)
                .overrideConfiguration(override)
                .serviceConfiguration(s3cfg)
                .region(Region.of(props.getRegion()))
                .credentialsProvider(resolveCredentials());

        // 커스텀 엔드포인트(LocalStack/MinIO) 사용 시 설정
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(props.getEndpoint()));
        }
        return builder.build();
    }

    /**
     * 프리사인드 URL 생성을 위한 {@link S3Presigner}.
     *
     * <p>특징:
     * <ul>
     *   <li>요청마다 AWS SigV4로 URL을 서명</li>
     *   <li>만료 시각, Content-Type/Disposition 헤더 서명 가능</li>
     * </ul>
     *
     * @return 구성된 {@link S3Presigner}
     */
    @Bean
    public S3Presigner s3Presigner() {
        var builder = S3Presigner.builder()
                .region(Region.of(props.getRegion()))
                .credentialsProvider(resolveCredentials());
        if (props.getEndpoint() != null && !props.getEndpoint().isBlank()) {
            builder = builder.endpointOverride(URI.create(props.getEndpoint()));
        }
        return builder.build();
    }

    /**
     * 자격 증명 프로바이더 결정을 수행한다.
     *
     * <p>우선순위:
     * <ol>
     *   <li>프로퍼티에 명시된 accessKey/secret(선택: sessionToken) → {@link StaticCredentialsProvider}</li>
     *   <li>그 외 → {@link DefaultCredentialsProvider} (환경변수, 프로파일, EC2/ECS Role 자동 탐색)</li>
     * </ol>
     *
     * @return {@link AwsCredentialsProvider}
     */
    private AwsCredentialsProvider resolveCredentials() {
        // 명시 키가 있으면 정적 자격증명으로 고정(로컬/특수 배포용)
        if (notBlank(props.getAccessKeyId()) && notBlank(props.getSecretAccessKey())) {
            if (notBlank(props.getSessionToken())) {
                return StaticCredentialsProvider.create(
                        AwsSessionCredentials.create(
                                props.getAccessKeyId(),
                                props.getSecretAccessKey(),
                                props.getSessionToken()
                        )
                );
            }
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(
                            props.getAccessKeyId(),
                            props.getSecretAccessKey()
                    )
            );
        }

        // 기본 체인: Env → System Props → Profile(~/.aws/credentials) → Container/IMDS
        return DefaultCredentialsProvider.builder().build();
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
