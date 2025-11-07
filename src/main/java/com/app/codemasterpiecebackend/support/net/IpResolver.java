package com.app.codemasterpiecebackend.support.net;

import com.app.codemasterpiecebackend.config.net.NetProps;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 클라이언트 실제 IP를 추정/정규화/비식별화하는 리졸버.
 * <p>
 * 신뢰 가능한 프록시 체인을 설정(예: Nginx, Cloudflare)해두면
 * 요청 헤더(Forwarded/X-Forwarded-For/CF-Connecting-IP/True-Client-IP)를
 * 역순으로 훑어 첫 번째 <b>비신뢰 홉</b>을 클라이언트 IP로 간주한다.
 * <ul>
 *   <li>IPv4/IPv6 모두 지원</li>
 *   <li>IPv4-mapped IPv6(::ffff:1.2.3.4) 정규화</li>
 *   <li>/24(IPv4), /48(IPv6) 마스킹 제공</li>
 *   <li>솔트를 이용한 안정적 해시 제공(로그/집계용)</li>
 *   <li>체인/출처 헤더 정보까지 반환하여 디버깅 용이</li>
 * </ul>
 *
 * <p><b>주의</b>: 이 컴포넌트는 PII 최소화를 위해 <i>원시 IP 저장을 권장하지 않는다</i>.
 * 저장이 필요한 경우 {@link IpInfo#maskedIp()} 또는 {@link IpInfo#hashedIp()}를 사용하라.
 *
 * <p>설정은 {@link NetProps}를 통해 주입:
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
 */
@Component
@RequiredArgsConstructor
public class IpResolver {

    /**
     * 네트워크 설정(신뢰 프록시 CIDR, 해시 솔트 등)
     */
    private final NetProps props;

    /**
     * RFC 7239 Forwarded 헤더의 for= 값 추출 정규식 (대괄호/따옴표 허용)
     */
    private static final Pattern FORWARDED_PAIR =
            Pattern.compile("(?i)for=\"?\\[?([^;\\s\\\"]+)\\]?\"?");

    /**
     * 요청에서 클라이언트 IP를 추정하고 마스킹/해시/체인 정보를 함께 반환한다.
     *
     * <p>동작 순서:
     * <ol>
     *   <li>Forwarded → X-Forwarded-For → CF-Connecting-IP → True-Client-IP → RemoteAddr 순으로 체인을 수집</li>
     *   <li>가장 가까운 홉부터 역순으로 훑되, {@link NetProps#trustedMatchers()}에 포함되지 않는 첫 IP를
     *   <b>클라이언트 IP</b>로 채택</li>
     *   <li>IPv4-mapped IPv6 정규화, 마스킹(/24, /48), 해시(SHA-256+salt) 생성</li>
     *   <li>체인은 최신 프록시 → 원거리 순으로 정렬하여 반환</li>
     * </ol>
     *
     * @param req 현재 HTTP 요청
     * @return 클라이언트 IP 정보 레코드
     */
    public IpInfo resolve(HttpServletRequest req) {
        // 1) 우선순위 헤더 수집
        List<String> chain = new ArrayList<>();

        // RFC 7239: Forwarded: for=1.2.3.4;proto=https;by=...
        String fwd = req.getHeader("Forwarded");
        if (fwd != null) {
            var m = FORWARDED_PAIR.matcher(fwd);
            while (m.find()) chain.add(clean(m.group(1)));
        }

        // X-Forwarded-For: "client, proxy1, proxy2" (좌측이 원 클라)
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            for (String s : xff.split(",")) chain.add(clean(s));
        }

        // Cloudflare / Akamai 계열 헤더
        pushIfPresent(chain, req.getHeader("CF-Connecting-IP"));
        pushIfPresent(chain, req.getHeader("True-Client-IP"));

        // 마지막으로 직접 연결 소켓 주소(프록시 미구성 시 폴백)
        chain.add(clean(req.getRemoteAddr()));

        // 2) trusted proxy 제거 후 첫 untrusted를 클라 IP로 채택
        Collections.reverse(chain); // 가장 가까운 홉부터 검사
        String client = null;
        for (String ipStr : chain) {
            InetAddress ip = parse(ipStr);
            if (ip == null) continue;
            if (!isTrusted(ip)) {
                client = toNormalized(ip);
                break;
            }
        }
        if (client == null) { // 모든 홉이 신뢰 프록시거나 파싱 실패 시 폴백
            client = clean(req.getRemoteAddr());
        }

        // 3) 보강 정보 생성
        String masked = mask(client);
        boolean isPrivate = isPrivate(parse(client));
        String source = sourceHeader(req);
        String hash = hashIp(client, props.getHashSalt());

        // 체인은 보기 좋게 최신 프록시 → 원거리 순서로 되돌림
        Collections.reverse(chain);

        return new IpInfo(client, masked, hash, isPrivate, source, List.copyOf(chain));
    }

    /* ───────────────────────── helpers ───────────────────────── */

    /**
     * null/공백 아닌 값만 체인에 추가 (대괄호 제거 포함)
     */
    private static void pushIfPresent(List<String> list, String v) {
        if (v != null && !v.isBlank()) list.add(clean(v));
    }

    /**
     * IP 문자열 공백 정리 + 대괄호 제거([::1] → ::1)
     */
    private static String clean(String s) {
        return s == null ? null : s.trim().replaceAll("^\\[|\\]$", "");
    }

    /**
     * 문자열을 InetAddress로 파싱 (실패 시 null)
     */
    private static InetAddress parse(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    /**
     * IPv4-mapped IPv6(::ffff:192.0.2.1) → 순수 IPv4("192.0.2.1")로 정규화.
     * 일반 주소는 getHostAddress() 그대로 반환.
     */
    private static String toNormalized(InetAddress ip) {
        byte[] a = ip.getAddress();
        if (a.length == 16) {
            boolean mapped = true;
            for (int i = 0; i < 10; i++)
                if (a[i] != 0) {
                    mapped = false;
                    break;
                }
            if (a[10] != (byte) 0xFF || a[11] != (byte) 0xFF) mapped = false;
            if (mapped) {
                return ((a[12] & 0xFF) + "." + (a[13] & 0xFF) + "." + (a[14] & 0xFF) + "." + (a[15] & 0xFF));
            }
        }
        return ip.getHostAddress();
    }

    /**
     * 설정된 신뢰 프록시 CIDR 목록과 일치 여부
     */
    private boolean isTrusted(InetAddress ip) {
        return props.trustedMatchers().stream().anyMatch(m -> m.test(ip));
    }

    /**
     * 사설/루프백/링크로컬 여부
     */
    private static boolean isPrivate(InetAddress ip) {
        if (ip == null) return false;
        return ip.isAnyLocalAddress()
                || ip.isLoopbackAddress()
                || ip.isLinkLocalAddress()
                || ip.isSiteLocalAddress();
    }

    /**
     * /24(IPv4) 또는 /48(IPv6)로 마스킹된 문자열 반환.
     * <p>예) 203.0.113.25 → 203.0.113.0, 2001:db8:abcd:1234::1 → 2001:db8:abcd::</p>
     */
    public static String mask(String ip) {
        InetAddress addr = parse(ip);
        if (addr == null) return null;
        byte[] b = addr.getAddress();
        if (b.length == 4) return (b[0] & 0xFF) + "." + (b[1] & 0xFF) + "." + (b[2] & 0xFF) + ".0";
        // IPv6: 앞 3 hextet 유지, 나머지 0 → /48 근사
        String s = addr.getHostAddress();
        return s.replaceAll("(^([0-9a-fA-F]{0,4}:){3})([0-9a-fA-F:]+)$", "$1::");
    }

    /**
     * 솔트를 섞은 SHA-256 해시를 URL-safe Base64로 단축 인코딩.
     * <p>동일 IP는 동일 해시가 나오므로 유저 카운팅/중복 억제에 사용.</p>
     */
    private static String hashIp(String ip, String salt) {
        if (ip == null || salt == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] dig = md.digest(ip.getBytes());
            // 18바이트로 잘라 URL-safe Base64 (길이≈24자)
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(dig, 18));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 어떤 헤더를 신뢰 소스로 사용했는지 식별 문자열 반환
     */
    private static String sourceHeader(HttpServletRequest req) {
        if (has(req, "CF-Connecting-IP")) return "cf-connecting-ip";
        if (has(req, "True-Client-IP")) return "true-client-ip";
        if (has(req, "Forwarded")) return "forwarded";
        if (has(req, "X-Forwarded-For")) return "x-forwarded-for";
        return "remote-addr";
    }

    private static boolean has(HttpServletRequest r, String h) {
        return r.getHeader(h) != null;
    }

    /**
     * IP 정보 DTO.
     * <ul>
     *   <li>{@code clientIp}: 추정된 실제 클라이언트 IP(정규화됨). 저장 지양.</li>
     *   <li>{@code maskedIp}: /24(IPv4)/48(IPv6) 마스킹. 집계/중복 억제 추천 키.</li>
     *   <li>{@code hashedIp}: salt 기반 해시. 장기 집계/로그 용도.</li>
     *   <li>{@code privateRange}: 사설/루프백 등 여부 플래그.</li>
     *   <li>{@code source}: 사용된 우선 헤더 식별자(ex. x-forwarded-for).</li>
     *   <li>{@code chain}: 프록시 체인(최신 프록시 → 원거리) 스냅샷.</li>
     * </ul>
     */
    public record IpInfo(
            String clientIp,
            String maskedIp,
            String hashedIp,
            boolean privateRange,
            String source,
            List<String> chain
    ) {
    }
}
