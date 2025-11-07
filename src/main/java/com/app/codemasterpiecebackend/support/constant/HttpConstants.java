package com.app.codemasterpiecebackend.support.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * HTTP 관련 공용 상수 모음.
 * <p>
 * API 헤더, 미디어 타입, 공통 키 등은 여기서 관리한다.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpConstants {

    /** 클라이언트 식별 키 헤더명 */
    public static final String HEADER_CLIENT_KEY = "X-Client-Key";
}
