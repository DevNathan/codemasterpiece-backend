package com.app.codemasterpiecebackend.util;

import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@code ActorUtil} — 요청 주체(Actor)를 표준 방식으로 식별하는 유틸리티.
 *
 * <p>이 클래스는 인증 정보({@link AppUserDetails}) 또는 클라이언트 식별 키를 기반으로
 * 현재 요청을 수행하는 "행위 주체(Actor)"를 해석한다.
 * 인증된 사용자는 {@link ActorProvider#GITHUB} 등으로 구분되며,
 * 인증되지 않은 익명 요청은 {@link ActorProvider#ANON} 으로 분류된다.
 *
 * <p>모든 컨트롤러, 서비스, 도메인 계층에서 일관된 방식으로
 * "누가 이 요청을 수행했는가?"를 판단하기 위해 사용한다.
 *
 * <p><b>규칙 요약:</b>
 * <ul>
 *     <li>로그인 유저: {@code AppUserDetails != null}</li>
 *     <li>익명 유저: {@code AppUserDetails == null && clientKey 존재}</li>
 *     <li>완전 무명: {@code AppUserDetails == null && clientKey 없음}</li>
 * </ul>
 *
 * <p>예시 사용:
 * <pre>{@code
 * Actor actor = ActorUtil.resolve(userDetails, clientKey);
 * if (ActorUtil.isGuest(actor)) {
 *     // 게스트 처리
 * } else if (ActorUtil.isAuthenticated(actor)) {
 *     // 인증 사용자 처리
 * }
 * }</pre>
 *
 * @author DevNathan
 * @since 2025-10
 */
public final class ActorUtil {

    /**
     * 인스턴스화 방지
     */
    private ActorUtil() {
    }

    /**
     * 현재 요청의 주체(Actor)를 식별한다.
     *
     * <p>인증된 사용자 정보가 존재하면 {@link ActorProvider#GITHUB} 또는
     * 향후 확장된 provider로 지정된다.
     * 인증 정보가 없지만 클라이언트 키가 존재하면 {@link ActorProvider#ANON} 으로 분류한다.
     * 두 정보 모두 없으면 완전 무명 요청으로 간주한다.
     *
     * @param userDetails 인증된 사용자 정보 (없으면 {@code null})
     * @param clientKey   클라이언트 식별 키 (익명 사용자의 브라우저/세션 구분용)
     * @return 요청을 수행한 {@link Actor} 인스턴스 (null 반환 없음)
     */
    public static Actor resolve(@Nullable AppUserDetails userDetails, @Nullable String clientKey) {
        if (userDetails != null) {
            return new Actor(
                    ActorProvider.GITHUB,
                    userDetails.getAppUser().userId(),      // 내부 User 식별자
                    userDetails.hasRole("AUTHOR")           // 권한 레벨 플래그
            );
        }
        // 익명 (clientKey가 있으면 익명 식별자, 없으면 완전 무명)
        if (StringUtils.hasText(clientKey)) {
            return new Actor(ActorProvider.ANON, clientKey, false);
        }
        return new Actor(ActorProvider.ANON, null, false);
    }

    /**
     * 해당 {@link Actor}가 익명(비로그인) 사용자인지 여부를 반환한다.
     *
     * @param actor 검사 대상
     * @return {@code true} if provider == ANON
     */
    public static boolean isGuest(Actor actor) {
        return actor.provider() == ActorProvider.ANON;
    }

    /**
     * 해당 {@link Actor}가 인증된 사용자(GITHUB/LOCAL 등)인지 여부를 반환한다.
     *
     * @param actor 검사 대상
     * @return {@code true} if provider != ANON and actorId != null
     */
    public static boolean isAuthenticated(Actor actor) {
        return actor.provider() != ActorProvider.ANON && actor.actorId() != null;
    }

    /**
     * 두 {@link Actor}가 동일 주체인지 비교한다.
     *
     * <p>provider와 actorId가 모두 일치하면 동일 주체로 간주한다.
     *
     * @param a 첫 번째 Actor
     * @param b 두 번째 Actor
     * @return {@code true} if same provider and same actorId
     */
    public static boolean sameActor(Actor a, Actor b) {
        if (a == null || b == null) return false;
        if (a.provider() != b.provider()) return false;
        return a.actorId() != null && a.actorId().equals(b.actorId());
    }

    /**
     * 요청 주체를 표현하는 불변 객체(Value Object).
     *
     * <p>{@code provider}는 주체의 공급자 유형(GITHUB, LOCAL, ANON 등),
     * {@code actorId}는 내부 식별자(유저ID 또는 클라이언트키),
     * {@code elevated}는 상위 권한(예: AUTHOR, ADMIN 등)을 나타낸다.
     *
     * <p>엔티티가 아닌 단순 식별 객체이므로, 영속성 계층과는 무관하게 사용된다.
     *
     * @param provider 주체 제공자
     * @param actorId  내부 식별자
     * @param elevated 상위 권한 보유 여부
     */
    public record Actor(ActorProvider provider, String actorId, boolean elevated) {
    }
}
