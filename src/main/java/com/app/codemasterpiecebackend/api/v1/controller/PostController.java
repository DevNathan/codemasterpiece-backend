package com.app.codemasterpiecebackend.api.v1.controller;

import com.app.codemasterpiecebackend.api.v1.request.post.PostCreateRequest;
import com.app.codemasterpiecebackend.api.v1.request.post.PostToggleLikeRequest;
import com.app.codemasterpiecebackend.api.v1.request.post.PostUpdateRequest;
import com.app.codemasterpiecebackend.domain.dto.post.PostDetailDTO;
import com.app.codemasterpiecebackend.domain.dto.post.PostEditDTO;
import com.app.codemasterpiecebackend.domain.dto.post.PostLikeResultDTO;
import com.app.codemasterpiecebackend.domain.dto.response.SuccessPayload;
import com.app.codemasterpiecebackend.domain.types.ActorProvider;
import com.app.codemasterpiecebackend.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.service.post.PostLikeService;
import com.app.codemasterpiecebackend.service.post.PostService;
import com.app.codemasterpiecebackend.service.post.PostViewService;
import com.app.codemasterpiecebackend.service.post.cmd.PostDetailCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostLikeCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostSearchCmd;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.support.net.IpResolver;
import com.app.codemasterpiecebackend.util.ActorUtil;
import com.app.codemasterpiecebackend.util.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.app.codemasterpiecebackend.support.constant.HttpConstants.HEADER_CLIENT_KEY;
import static com.app.codemasterpiecebackend.util.Stringx.trimToNull;

/**
 * Post API (v1)
 *
 * <p><strong>Endpoint Prefix:</strong> /api/v1/posts</p>
 *
 * <p>
 * - Create:   POST   /api/v1/posts<br/>
 * - Read:     GET    /api/v1/posts (list), GET /api/v1/posts/{slug} (detail)<br/>
 * - Update:   PUT    /api/v1/posts/{postId}<br/>
 * - Delete:   DELETE /api/v1/posts/{postId}<br/>
 * </p>
 *
 * <p>Auxiliary:</p>
 * <ul>
 *   <li>POST /api/v1/posts/like — 토글 좋아요</li>
 *   <li>POST /api/v1/posts/view — 조회수 등록</li>
 *   <li>GET  /api/v1/posts/edit/{postId} — 편집용 상세 (AUTHOR 전용)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final PostLikeService likeService;
    private final PostViewService postViewService;
    private final IpResolver ipResolver;

    // ===== C: Create =====

    /**
     * 게시글 생성 (AUTHOR 전용)
     *
     * @param body 생성 요청 본문
     * @return 생성된 게시글의 slug
     */
    @PostMapping
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<String> createPost(@Valid @RequestBody PostCreateRequest body) {
        String slug = postService.create(body.toCmd());
        return SuccessPayload.of(slug, "success.post.created");
    }

    // ===== R: Read =====

    /**
     * 게시글 페이지 조회 (목록)
     *
     * @param pageable    페이지/정렬 파라미터 (기본: size=12, createdAt DESC)
     * @param link        선택: 카테고리 링크 필터
     * @param userDetails 인증 사용자
     * @return 페이지 응답 맵 (items, page, size, total 등)
     */
    @GetMapping
    public SuccessPayload<?> getPosts(
            @PageableDefault(size = 12, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(name = "link", required = false) String link,
            @RequestParam(name = "keyword", required = false) String keyword,
            @AuthenticationPrincipal AppUserDetails userDetails
    ) {
        boolean elevated = userDetails != null && userDetails.hasRole("AUTHOR");
        var page = postService.getAll(new PostSearchCmd(pageable, elevated, link, keyword));
        return SuccessPayload.of(PageUtil.toResponseMap(page));
    }

    /**
     * 게시글 상세 조회 (slug 기반)
     *
     * @param slug        게시글 슬러그
     * @param userDetails 인증 사용자(선택)
     * @param clientKey   클라이언트 식별자(선택)
     * @return 게시글 상세 DTO
     */
    @GetMapping("/{slug}")
    public SuccessPayload<PostDetailDTO> getPost(
            @PathVariable String slug,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var resolved = ActorUtil.resolve(userDetails, clientKey);
        boolean elevated = resolved.elevated();

        var cmd = new PostDetailCmd(
                trimToNull(slug),
                resolved.provider(),
                resolved.actorId(),
                elevated
        );
        var detail = postService.getDetail(cmd);
        return SuccessPayload.of(detail);
    }

    // ===== U: Update =====

    /**
     * 게시글 수정 (AUTHOR 전용)
     *
     * @param postId 수정 대상 게시글 ID
     * @param body   수정 요청 본문
     * @return slug 반환 (수정 후 접근 경로 보장)
     */
    @PutMapping("/{postId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<?> updatePost(
            @PathVariable String postId,
            @Valid @RequestBody PostUpdateRequest body
    ) {
        var result = postService.update(body.toCmd(postId));
        return SuccessPayload.of(Map.of("slug", result.slug()), "success.post.updated");
    }

    // ===== D: Delete =====

    /**
     * 게시글 삭제 (AUTHOR 전용)
     *
     * @param postId 삭제 대상 게시글 ID
     */
    @DeleteMapping("/{postId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public void deletePost(@PathVariable String postId) {
        postService.delete(postId);
    }

    // ===== Aux: Like / View / Edit =====

    /**
     * 게시글 좋아요 토글
     *
     * <p>익명 사용자는 clientKey가 필수.</p>
     *
     * @param body        토글 요청
     * @param userDetails 사용자 정보(선택)
     * @param clientKey   익명 클라이언트 키(선택)
     * @return 현재 좋아요 상태/카운트
     */
    @PostMapping("/like")
    public SuccessPayload<PostLikeResultDTO> toggleLike(
            @Valid @RequestBody PostToggleLikeRequest body,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var resolved = ActorUtil.resolve(userDetails, clientKey);

        if (resolved.provider() == ActorProvider.ANON && resolved.actorId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "error.like.need_client_key");
        }

        var result = likeService.toggle(new PostLikeCmd(
                trimToNull(body.postId()),
                resolved.provider(),
                resolved.actorId(),
                body.toggleLike()
        ));
        return SuccessPayload.of(result, "success.post.like.toggled");
    }

    /**
     * 게시글 조회수 기록
     *
     * @param body    {"postId": "..."}
     * @param request 원격 IP 추출용
     * @return {"counted": true/false}
     */
    @PostMapping("/view")
    public SuccessPayload<Map<String, Object>> registerView(
            @RequestBody Map<String, String> body,
            HttpServletRequest request
    ) {
        String postId = trimToNull(body.get("postId"));
        if (postId == null) throw new AppException(HttpStatus.BAD_REQUEST, "error.view.post_id_required");

        var ipInfo = ipResolver.resolve(request);
        boolean counted = postViewService.registerView(postId, ipInfo.maskedIp());

        return SuccessPayload.of(Map.of("counted", counted), "success.post.view.recorded");
    }

    /**
     * 편집용 상세 조회 (AUTHOR 전용)
     *
     * @param postId 편집 대상 게시글 ID
     * @return 편집용 DTO
     */
    @GetMapping("/edit/{postId}")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<PostEditDTO> getPostById(@PathVariable String postId) {
        var dto = postService.getEditById(postId);
        return SuccessPayload.of(dto);
    }
}
