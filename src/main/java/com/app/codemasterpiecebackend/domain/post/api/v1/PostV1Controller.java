package com.app.codemasterpiecebackend.domain.post.api.v1;

import com.app.codemasterpiecebackend.domain.post.application.PostCommand;
import com.app.codemasterpiecebackend.domain.post.application.PostLikeService;
import com.app.codemasterpiecebackend.domain.post.application.PostService;
import com.app.codemasterpiecebackend.domain.post.application.PostViewService;
import com.app.codemasterpiecebackend.domain.post.dto.PostDetailDTO;
import com.app.codemasterpiecebackend.domain.post.dto.PostEditDTO;
import com.app.codemasterpiecebackend.domain.post.dto.PostResult;
import com.app.codemasterpiecebackend.domain.post.job.ViewKeysCleaner;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import com.app.codemasterpiecebackend.global.security.user.AppUserDetails;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import com.app.codemasterpiecebackend.global.support.net.IpResolver;
import com.app.codemasterpiecebackend.global.support.response.SuccessPayload;
import com.app.codemasterpiecebackend.global.util.ActorUtil;
import com.app.codemasterpiecebackend.global.util.MarkdownUtil;
import com.app.codemasterpiecebackend.global.util.PageUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static com.app.codemasterpiecebackend.global.support.constant.HttpConstants.HEADER_CLIENT_KEY;
import static com.app.codemasterpiecebackend.global.util.Stringx.trimToNull;

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
@Slf4j
@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class PostV1Controller {

    private final PostService postService;
    private final PostLikeService likeService;
    private final PostViewService postViewService;
    private final ViewKeysCleaner viewKeysCleaner;
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
    public SuccessPayload<String> createPost(@Valid @RequestBody PostRequest.Create body) {
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
        var page = postService.getAll(new PostCommand.Search(pageable, elevated, link, keyword));
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
            @RequestParam(name = "exclude-content", defaultValue = "false") boolean excludeContent,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var resolved = ActorUtil.resolve(userDetails, clientKey);
        boolean elevated = resolved.elevated();

        var cmd = new PostCommand.Detail(
                trimToNull(slug),
                resolved.provider(),
                resolved.actorId(),
                elevated,
                excludeContent
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
            @Valid @RequestBody PostRequest.Update body
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
    public SuccessPayload<PostResult.Like> toggleLike(
            @Valid @RequestBody PostRequest.ToggleLike body,
            @AuthenticationPrincipal @Nullable AppUserDetails userDetails,
            @RequestHeader(value = HEADER_CLIENT_KEY, required = false) @Nullable String clientKey
    ) {
        var resolved = ActorUtil.resolve(userDetails, clientKey);

        if (resolved.provider() == ActorProvider.ANON && resolved.actorId() == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "error.like.need_client_key");
        }

        var result = likeService.toggle(new PostCommand.Like(
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

    @PostMapping("/preview")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<Map<String, Object>> previewPostMarkdown(@RequestBody Map<String, String> payload) {
        String content = payload.get("content");

        if (content == null || content.isBlank()) {
            return SuccessPayload.of(Map.of("html", "", "toc", List.of()));
        }

        String html = MarkdownUtil.parsePostToHtml(content);
        var toc = MarkdownUtil.extractToc(content);

        return SuccessPayload.of(Map.of(
                "html", html,
                "toc", toc
        ));
    }

    /**
     * 조회수 캐시 키를 수동으로 일괄 정리합니다.
     * * @return 성공 메시지
     */
    @PostMapping("/admin/purge-views")
    @PreAuthorize("hasRole('AUTHOR')")
    public SuccessPayload<String> manualPurgeViewKeys() {
        log.warn("Manual view keys purge triggered by AUTHOR.");
        viewKeysCleaner.purgeViewKeys();
        return SuccessPayload.of("OK", "success.admin.views.purged");
    }
}
