package com.app.codemasterpiecebackend.util;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ULID(Universally Unique Lexicographically Sortable Identifier) 생성기.
 * <p>
 * 특징:
 * <ul>
 *   <li>시간 정렬 가능: 상위 48비트에 밀리초 타임스탬프 사용</li>
 *   <li>랜덤 80비트: 충돌 위험을 극도로 낮춤</li>
 *   <li>단조 증가 모드 제공: 동일 밀리초 내에서 랜덤부를 증가시켜 정렬 안정성 확보</li>
 *   <li>접두사(prefix) 지원: {@code PREFIX-ULID} 형태 반환</li>
 * </ul>
 *
 * <h3>형식</h3>
 * <pre>
 *   26자 Crockford Base32 (0-9, A-Z, I/L/O/U 제외)
 *   예) 01JCT2P3GV8Q8W8M2N9R2QX7ZC
 *   예) TK-01JCT2P3GV8Q8W8M2N9R2QX7ZC
 * </pre>
 *
 * <h3>스레드 안전성</h3>
 * <ul>
 *   <li>{@link #newUlid()} 계열: lock-free, 일반용</li>
 *   <li>{@link #newMonotonicUlid()} 계열: synchronized, 동일 ms 내 단조증가 보장</li>
 * </ul>
 *
 * <h3>사용 예</h3>
 * <pre>{@code
 * String id1 = ULIDs.newUlid();               // 기본 ULID
 * String id2 = ULIDs.newUlid("TK");           // "TK-..." 형태
 * String id3 = ULIDs.newMonotonicUlid();      // 단조 증가 ULID
 * String id4 = ULIDs.newMonotonicUlid("ORD"); // "ORD-..." 형태
 * }</pre>
 *
 * <h3>주의</h3>
 * <ul>
 *   <li>단조 증가 모드에서 동일 ms 내 생성량이 2^80개를 넘으면(사실상 불가능), 다음 ms까지 바쁜 대기 후 재개</li>
 *   <li>접두사는 공백/빈 문자열이면 무시됨</li>
 * </ul>
 */
public final class ULIDs {
    private ULIDs() {
    }

    /**
     * Crockford Base32 알파벳 (I, L, O, U 제외).
     */
    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();

    /**
     * 최근 생성 시각(ms). 단조 증가 모드에서만 사용.
     */
    private static final AtomicLong LAST_TIME = new AtomicLong(-1L);

    /**
     * 최근 랜덤 상위 16비트. 단조 증가 모드에서만 사용.
     */
    private static volatile int lastRandHi = 0;

    /**
     * 최근 랜덤 하위 64비트. 단조 증가 모드에서만 사용.
     */
    private static volatile long lastRandLo = 0L;

    /**
     * 보안용 난수(상위 16비트 시드 강화).
     */
    private static final SecureRandom SECURE = new SecureRandom();

    // ----------------------------------------------------------------------
    // Public API: Non-monotonic
    // ----------------------------------------------------------------------

    /**
     * 일반 ULID 생성 (단조 증가 미보장).
     *
     * @return 26자 ULID 문자열 (예: {@code 01JCT2P3GV8Q8W8M2N9R2QX7ZC})
     */
    public static String newUlid() {
        return newUlid(null);
    }

    /**
     * 일반 ULID 생성 + 접두사.
     *
     * @param prefix 접두사(예: {@code "TK"}). {@code null} 또는 공백/빈 문자열이면 무시.
     * @return 접두사가 있으면 {@code prefix + "-" + ULID}, 아니면 ULID 원문
     */
    public static String newUlid(String prefix) {
        long time = System.currentTimeMillis();                  // 상위 48비트
        int randHi = ThreadLocalRandom.current().nextInt() & 0xFFFF; // 랜덤 상위 16비트
        long randLo = ThreadLocalRandom.current().nextLong();         // 랜덤 하위 64비트
        String ulid = encode(time, randHi, randLo);
        return decorate(prefix, ulid);
    }

    // ----------------------------------------------------------------------
    // Public API: Monotonic
    // ----------------------------------------------------------------------

    /**
     * 단조 증가 ULID 생성.
     * <p>
     * 동일한 밀리초(ms) 내에서 다수 생성 시, 랜덤부(80비트)를 +1씩 증가시켜
     * 사전식 정렬이 안정적으로 시간 순서를 반영하도록 보장한다.
     *
     * @return 26자 ULID 문자열
     */
    public static String newMonotonicUlid() {
        return newMonotonicUlid(null);
    }

    /**
     * 단조 증가 ULID 생성 + 접두사.
     * <p>
     * synchronized로 보호되어 동일 ms 내 생성 순서를 보장한다.
     *
     * @param prefix 접두사(예: {@code "ORD"}). {@code null} 또는 공백/빈 문자열이면 무시.
     * @return 접두사가 있으면 {@code prefix + "-" + ULID}, 아니면 ULID 원문
     */
    public static synchronized String newMonotonicUlid(String prefix) {
        long now = System.currentTimeMillis();
        long last = LAST_TIME.get();

        int randHi;
        long randLo;

        if (now > last) {
            // 시각이 전진 → 새로운 랜덤 시드
            LAST_TIME.set(now);
            randHi = secureHi16();
            randLo = ThreadLocalRandom.current().nextLong();
        } else {
            // 동일 ms → 80비트 랜덤부 증가
            long lo = lastRandLo + 1;
            int hi = lastRandHi;

            // 하위 64비트에서 캐리 발생
            if (lo == 0) {
                hi = (hi + 1) & 0xFFFF;
                // 80비트 overflow → 다음 ms까지 대기
                if (hi == 0) {
                    do {
                        now = System.currentTimeMillis();
                    } while (now <= last);
                    LAST_TIME.set(now);
                    hi = secureHi16();
                    lo = ThreadLocalRandom.current().nextLong();
                }
            }
            randHi = hi;
            randLo = lo;
        }

        lastRandHi = randHi;
        lastRandLo = randLo;

        String ulid = encode(now, randHi, randLo);
        return decorate(prefix, ulid);
    }

    // ----------------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------------

    /**
     * 128비트 값을 26자 Crockford Base32로 인코딩.
     * <p>
     * 레이아웃: {@code [48-bit time][16-bit randHi][64-bit randLo]} → 130비트 슬롯 중 상위 2비트는 0으로 패딩.
     *
     * @param timeMs   밀리초(하위 48비트 사용)
     * @param randHi16 랜덤 상위 16비트
     * @param randLo64 랜덤 하위 64비트
     * @return 26자 Base32 문자열
     */

    private static String encode(long timeMs, int randHi16, long randLo64) {
        // 128bit big-endian buffer: [48b time][16b randHi][64b randLo]
        byte[] b = new byte[16];

        // time 48-bit
        b[0] = (byte) ((timeMs >>> 40) & 0xFF);
        b[1] = (byte) ((timeMs >>> 32) & 0xFF);
        b[2] = (byte) ((timeMs >>> 24) & 0xFF);
        b[3] = (byte) ((timeMs >>> 16) & 0xFF);
        b[4] = (byte) ((timeMs >>>  8) & 0xFF);
        b[5] = (byte) ( timeMs         & 0xFF);

        // randHi 16-bit
        b[6] = (byte) ((randHi16 >>> 8) & 0xFF);
        b[7] = (byte) ( randHi16        & 0xFF);

        // randLo 64-bit
        b[8]  = (byte) ((randLo64 >>> 56) & 0xFF);
        b[9]  = (byte) ((randLo64 >>> 48) & 0xFF);
        b[10] = (byte) ((randLo64 >>> 40) & 0xFF);
        b[11] = (byte) ((randLo64 >>> 32) & 0xFF);
        b[12] = (byte) ((randLo64 >>> 24) & 0xFF);
        b[13] = (byte) ((randLo64 >>> 16) & 0xFF);
        b[14] = (byte) ((randLo64 >>>  8) & 0xFF);
        b[15] = (byte) ( randLo64         & 0xFF);

        final char[] ALPH = ALPHABET; // "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
        char[] out = new char[26];

        // 상위 2비트 0-padding을 반영하기 위해 bitPos = -2 에서 시작
        int bitPos = -2; // 0이면 b[0]의 MSB. -2는 그보다 2비트 앞(가상의 0비트)
        for (int i = 0; i < 26; i++) {
            int v = 0;
            for (int k = 0; k < 5; k++) {
                int idx = bitPos + k;
                int bit;
                if (idx < 0) {
                    // padding 0
                    bit = 0;
                } else {
                    int byteIdx = idx / 8;
                    int bitInByte = 7 - (idx % 8);
                    bit = (b[byteIdx] >>> bitInByte) & 0x01;
                }
                v = (v << 1) | bit;
            }
            out[i] = ALPH[v & 0x1F];
            bitPos += 5;
        }
        return new String(out);
    }


    /**
     * 접두사를 적용해 최종 문자열을 구성한다.
     *
     * @param prefix 접두사(무시될 수 있음)
     * @param ulid   26자 ULID
     * @return {@code "prefix-ULID"} 또는 {@code ULID}
     */
    private static String decorate(String prefix, String ulid) {
        return (prefix == null || prefix.isBlank()) ? ulid : prefix + "-" + ulid;
    }

    /**
     * 상위 16비트 보안 난수.
     *
     * @return 0~65535 범위 정수
     */
    private static int secureHi16() {
        return SECURE.nextInt() & 0xFFFF;
    }
}
