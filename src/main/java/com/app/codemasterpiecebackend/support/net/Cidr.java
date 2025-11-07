package com.app.codemasterpiecebackend.support.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Predicate;

/**
 * CIDR 매칭 유틸.
 * <p>
 * IPv4/IPv6 주소에 대해 네트워크 프리픽스(prefix)로 포함 여부를 판정한다.
 * 불변 레코드이며, {@link #of(String)} 팩토리는 항상 {@code Predicate<InetAddress>}를 반환해
 * 제네릭 불공변 문제 없이 컬렉션에 담아 쓸 수 있게 한다.
 *
 * <pre>
 *   Predicate&lt;InetAddress&gt; m = Cidr.of("10.0.0.0/8");
 *   boolean ok = m.test(InetAddress.getByName("10.1.2.3"));
 * </pre>
 */
public record Cidr(byte[] network, int prefix) implements Predicate<InetAddress> {

    /**
     * 문자열 CIDR(예: {@code "192.168.0.0/16"}, {@code "2001:db8::/32"})을 파싱해
     * {@code Predicate<InetAddress>}를 반환한다.
     * 반환 타입을 인터페이스로 고정하여 {@code List<Predicate<InetAddress>>}에
     * 바로 담아 사용할 수 있다.
     *
     * @param cidr CIDR 표기 문자열
     * @return 주소 포함 여부 판정용 프리디케이트
     * @throws IllegalArgumentException 잘못된 CIDR 형식일 때
     */
    public static Predicate<InetAddress> of(String cidr) { // ← 인터페이스로 반환
        try {
            String[] sp = cidr.split("/");
            byte[] net = InetAddress.getByName(sp[0]).getAddress();
            int p = Integer.parseInt(sp[1]);
            return new Cidr(net, p);
        } catch (UnknownHostException | RuntimeException e) {
            throw new IllegalArgumentException("Invalid CIDR: " + cidr, e);
        }
    }

    @Override
    public boolean test(InetAddress addr) {
        byte[] a = addr.getAddress();
        if (a.length != network.length) return false; // IPv4/IPv6 길이 불일치
        int bits = prefix;
        for (int i = 0; i < a.length; i++) {
            int mask = (bits >= 8) ? 0xFF
                    : (bits <= 0 ? 0 : (0xFF << (8 - bits)) & 0xFF);
            if ((a[i] & mask) != (network[i] & mask)) return false;
            bits -= 8;
            if (bits <= 0) break;
        }
        return true;
    }
}
