package com.app.codemasterpiecebackend.domain.dto.response;

/**
 * 컨트롤러가 응답 시 데이터를 직접 반환하지 않고,
 * 메시지 코드/인자를 함께 전달할 수 있도록 하는 중간 DTO.
 * {@link com.app.codemasterpiecebackend.support.web.SuccessWrappingAdvice}에서
 * 이 정보를 이용해 {@link SuccessResponse}로 래핑한다.
 *
 * <p>용례:</p>
 * <pre>{@code
 * // 데이터 + 메시지 코드
 * return SuccessPayload.of(data, "success.user.updated");
 *
 * // 데이터만
 * return SuccessPayload.of(data);
 *
 * // 메시지만
 * return SuccessPayload.msg("success.user.created");
 * }</pre>
 */
public record SuccessPayload<T>(T data, String messageCode, Object[] messageArgs) {

    /**
     * 데이터만 있는 경우
     */
    public static <T> SuccessPayload<T> of(T data) {
        return new SuccessPayload<>(data, null, null);
    }

    /**
     * 데이터와 메시지를 함께 포함하는 경우
     */
    public static <T> SuccessPayload<T> of(T data, String code, Object... args) {
        return new SuccessPayload<>(data, code, args);
    }

    /**
     * 메시지만 있는 경우
     */
    public static SuccessPayload<Void> msg(String code, Object... args) {
        return new SuccessPayload<>(null, code, args);
    }
}
