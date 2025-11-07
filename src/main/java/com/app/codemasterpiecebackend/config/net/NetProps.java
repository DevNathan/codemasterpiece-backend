package com.app.codemasterpiecebackend.config.net;

import com.app.codemasterpiecebackend.support.net.Cidr;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 네트워크 관련 애플리케이션 설정.
 *
 * <p>application.yml 예시:
 * <pre>
 * app:
 *   net:
 *     trusted-proxies:
 *       - 127.0.0.1/32
 *       - 10.0.0.0/8
 *       - 172.16.0.0/12
 *       - 192.168.0.0/16
 *       - ::1/128
 *       - fc00::/7
 *     hash-salt: "change-me-strong-salt"
 * </pre>
 *
 * <p>사용 예:
 * <pre>
 * List&lt;Predicate&lt;InetAddress&gt;&gt; matchers = netProps.trustedMatchers();
 * boolean isTrusted = matchers.stream().anyMatch(m -&gt; m.test(addr));
 * </pre>
 */
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.net")
public class NetProps {

    /**
     * 신뢰할 프록시/엣지의 CIDR 목록
     */
    private List<String> trustedProxies = new ArrayList<>();

    /**
     * IP 해시에 사용할 솔트(로그·분석용 비식별화)
     */
    private String hashSalt = "";

    /**
     * {@code trustedProxies}를 파싱하여
     * {@code Predicate<InetAddress>} 목록으로 변환한다.
     * <p>주의: 제네릭 불공변 문제를 피하기 위해 {@link Cidr#of(String)}는
     * 항상 {@code Predicate<InetAddress>}를 반환한다.
     */
    public List<Predicate<InetAddress>> trustedMatchers() {
        return trustedProxies.stream()
                .map(Cidr::of)            // 반환 타입이 Predicate<InetAddress>
                .toList();
    }
}
