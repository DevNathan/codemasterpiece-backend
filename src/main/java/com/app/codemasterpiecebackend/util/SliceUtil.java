package com.app.codemasterpiecebackend.util;

import org.springframework.data.domain.Slice;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SliceUtil {

    /**
     * 커서 기반 Slice 응답 맵 변환 유틸.
     *
     * @param slice         Slice 데이터
     * @param cursorEncoder 마지막 요소 → 커서 변환 함수 (없으면 null)
     */
    public static <T> Map<String, Object> toResponseMap(
            Slice<T> slice,
            Function<T, String> cursorEncoder
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("content", slice.getContent());
        response.put("size", slice.getSize());
        response.put("first", slice.isFirst());
        response.put("last", slice.isLast());
        response.put("hasNext", slice.hasNext());

        if (slice.hasNext() && cursorEncoder != null && !slice.getContent().isEmpty()) {
            T tail = slice.getContent().get(slice.getNumberOfElements() - 1);
            response.put("nextCursor", cursorEncoder.apply(tail));
        } else {
            response.put("nextCursor", null);
        }

        return response;
    }
}
