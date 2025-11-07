package com.app.codemasterpiecebackend.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Stringx {

    /**
     * 공백 제거 후 빈 문자열이면 null로 변환.
     * ex) "  foo  " → "foo", "   " → null, null → null
     */
    public static String trimToNull(String str) {
        if (str == null) return null;
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String slugify(String str) {
        final int MAX = 200;

        if (str == null) str = "";
        String s = str.strip()
                .replaceAll("[\\s_]+", "-")
                .replaceAll("[^0-9A-Za-z가-힣-]+", "")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (s.length() > MAX) s = s.substring(0, MAX).replaceAll("^-|-$", "");
        return s.isEmpty() ? "post" : s;
    }
}
