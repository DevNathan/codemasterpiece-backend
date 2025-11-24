package com.app.codemasterpiecebackend.service.post;

import com.app.codemasterpiecebackend.domain.dto.post.*;
import com.app.codemasterpiecebackend.domain.entity.category.Category;
import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.domain.entity.post.Post;
import com.app.codemasterpiecebackend.domain.entity.post.Tag;
import com.app.codemasterpiecebackend.domain.repository.CategoryRepository;
import com.app.codemasterpiecebackend.domain.repository.PostLikeRepository;
import com.app.codemasterpiecebackend.domain.repository.PostRepository;
import com.app.codemasterpiecebackend.domain.repository.TagRepository;
import com.app.codemasterpiecebackend.domain.repository.file.FileVariantRepository;
import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.mapper.PostMapper;
import com.app.codemasterpiecebackend.service.filesystem.file.FileService;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.ref.FileRefService;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.AttachCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostCreateCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostDetailCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostSearchCmd;
import com.app.codemasterpiecebackend.service.post.cmd.PostUpdateCmd;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.support.exception.FieldValidationException;
import com.app.codemasterpiecebackend.util.CdnProperties;
import com.app.codemasterpiecebackend.util.FileUrlResolver;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.app.codemasterpiecebackend.util.Stringx.slugify;

/**
 * PostService 구현체.
 * <p>
 * 메서드 순서는 CRUD → 액션 → 내부 유틸 순으로 정리했다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class PostServiceImpl implements PostService {

    // ------------------------------- Dependencies -------------------------------
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final PostLikeRepository postLikeRepository;
    private final TagRepository tagRepository;
    private final CategoryRepository categoryRepository;
    private final StoredFileRepository storedFileRepository;
    private final FileVariantRepository fileVariantRepository;
    private final FileRefService fileRefService;
    private final FileService fileService;

    private final EntityManager em;
    private final CdnProperties cdnProperties;

    // ------------------------------- Constants ----------------------------------
    /**
     * 본문 내 파일 토큰: ![...](file://FL-XXXXXXXX... [ ?t=kind ])
     */
    private static final Pattern FILE_TOKEN = Pattern.compile(
            "!\\[[^]]*]\\(file://(FL-[A-Z0-9]{26})(?:\\?t=([\\w\\-_.]+))?\\)"
    );

    /**
     * CDN URL 내부에서 FL-... 추출
     */
    private static final Pattern CDN_FL_IN_URL = Pattern.compile(
            "https?://[^)\\s]*/(?:\\d{4}/\\d{2}/\\d{2}/)?(FL-[A-Z0-9]{26})/"
    );

    /**
     * 변환 우선순위: AVIF > WEBP > 512 > 256
     */
    private static final List<String> DEFAULT_ORDER = List.of("AVIF", "WEBP", "THUMB_512", "THUMB_256");

    // --------------------------------- Create -----------------------------------

    @Override
    public String create(PostCreateCmd cmd) {
        // 0) 필수 가드: headImageId blank 금지
        if (cmd.headImageId() == null || cmd.headImageId().isBlank()) {
            throw new FieldValidationException(Map.of("headImageId", "validation.post.headImage.required"));
        }

        // 1) 카테고리 확인
        Category category = categoryRepository.findById(cmd.categoryId()).orElseThrow(
                () -> new AppException(HttpStatus.BAD_REQUEST, "error.category.not_found")
        );

        // 2) 슬러그 유니크 보장
        String baseSlug = slugify(cmd.title());
        String slug = baseSlug;
        int seq = 2;
        while (postRepository.existsBySlug(slug)) {
            if (seq > 1000) throw new FieldValidationException(Map.of("title", "validation.post.title.unavailable"));
            slug = baseSlug + "-" + seq++;
        }

        // 3) 태그 처리 (Cmd에서 이미 trim/lowercase 완료)
        List<String> inputTags = (cmd.tags() == null) ? List.of() : cmd.tags();

        Map<String, Tag> tagMap = inputTags.isEmpty()
                ? Map.of()
                : tagRepository.findByNameIn(inputTags)
                .stream()
                .collect(Collectors.toMap(Tag::getName, t -> t));

        // 없는 태그 일괄 생성
        if (!inputTags.isEmpty()) {
            List<String> missing = new ArrayList<>();
            for (String name : inputTags) if (!tagMap.containsKey(name)) missing.add(name);
            if (!missing.isEmpty()) {
                List<Tag> created = tagRepository.saveAll(
                        missing.stream().map(Tag::new).toList()
                );
                for (Tag t : created) tagMap.put(t.getName(), t);
            }
        }

        // 입력 순서 보존
        List<Tag> orderedTags = new ArrayList<>(inputTags.size());
        for (String name : inputTags) orderedTags.add(tagMap.get(name));

        // 4) 본문 토큰 리라이트 + 파일 ID 추출
        String mainContentRaw = (cmd.mainContent() == null) ? "" : cmd.mainContent();
        TokenRewriteResult rewrite = rewriteFileTokens(mainContentRaw);
        String rewritten = rewrite.rewrittenMarkdown();
        List<String> contentIds = extractFileIdsFromMarkdown(rewritten);

        // 5) 헤드이미지 존재 확인 후 로드
        StoredFile headRef = storedFileRepository.findById(cmd.headImageId())
                .orElseThrow(() -> new FieldValidationException(Map.of("headImageId", "validation.file.not_found")));

        // 6) 엔티티 생성/저장 (헤드이미지 무조건 세팅)
        Post entity = Post.builder()
                .slug(slug)
                .title(cmd.title())
                .category(category)
                .headImage(headRef)
                .headContent(cmd.headContent())
                .mainContent(rewritten)
                .link(category.getLink())
                .published(cmd.published())
                .build();

        entity.setTagsInOrder(orderedTags);

        Post saved = postRepository.save(entity);

        // 7) 파일 ref 연결(대표 이미지)
        fileRefService.attach(
                AttachCmd.builder()
                        .fileId(cmd.headImageId())
                        .ownerType(FileOwnerType.POST)
                        .ownerId(saved.getId())
                        .purpose(FilePurpose.HEAD_IMAGE)
                        .displayName(saved.getId() + " headimage")
                        .build()
        );

        // 8) 파일 ref 연결(본문)
        int order = 1;
        for (String fid : contentIds) {
            if (fid == null || fid.isBlank()) continue;
            fileRefService.attach(
                    AttachCmd.builder()
                            .fileId(fid)
                            .ownerType(FileOwnerType.POST)
                            .ownerId(saved.getId())
                            .purpose(FilePurpose.CONTENT)
                            .sortOrder(order++)
                            .displayName(saved.getId() + " content")
                            .build()
            );
        }

        return saved.getSlug();
    }

    // ---------------------------------- Read ------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PostDetailDTO getDetail(PostDetailCmd cmd) {
        PostDetailDTO dto = postMapper.findPostDetail(
                cmd.slug(),
                cmd.actorProvider() != null ? cmd.actorProvider().name() : null,
                cmd.actorId(),
                cmd.elevated()
        ).orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found"));

        dto.setHeadImage(FileUrlResolver.toCdnUrl(cdnProperties, dto.getHeadImage()));

        if (dto.getMorePosts() != null) {
            for (PostListDTO p : dto.getMorePosts()) {
                if (p.getHeadImage() != null) {
                    p.setHeadImage(FileUrlResolver.toCdnUrl(cdnProperties, p.getHeadImage()));
                }
            }
        }
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PostListDTO> getAll(PostSearchCmd cmd) {
        String[] sort = resolveSort(cmd.pageable()); // [sortKey, sortDir]

        long total = postMapper.countPostPage(
                cmd.link(),
                cmd.keyword(),
                cmd.elevated()
        );
        if (total == 0) return Page.empty(cmd.pageable());

        var rows = postMapper.findPostPage(
                cmd.link(),
                cmd.keyword(),
                cmd.elevated(),
                sort[0],
                sort[1],
                cmd.pageable().getPageSize(),
                (int) cmd.pageable().getOffset()
        );

        var content = rows.stream()
                .peek(dto -> {
                    String cdnUrl = (dto.getHeadImage() != null)
                            ? FileUrlResolver.toCdnUrl(cdnProperties, dto.getHeadImage())
                            : null;
                    dto.setHeadImage(cdnUrl);
                })
                .toList();

        return new PageImpl<>(content, cmd.pageable(), total);
    }

    @Override
    @Transactional(readOnly = true)
    public PostEditDTO getEditById(String postId) {
        var post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found"));

        String headId = (post.getHeadImage() != null) ? post.getHeadImage().getId() : null;
        FileInfo file = fileService.getFile(headId).orElseThrow(
                () -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found")
        );

        return PostEditDTO.builder()
                .id(post.getId())
                .title(post.getTitle())
                .categoryId(post.getCategory().getId())
                .headImage(headId)
                .headImageUrl(FileUrlResolver.toFileUrl(cdnProperties, file))
                .headContent(post.getHeadContent())
                .tags(post.getTagsInOrder().stream().map(Tag::getName).toList())
                .mainContent(post.getMainContent())
                .published(post.isPublished())
                .build();
    }

    @Override
    public List<PostSitemapDTO> getSitemaps() {
        return postRepository.findSitemaps();
    }

    // --------------------------------- Update -----------------------------------

    @Override
    public PostUpdateResultDTO update(PostUpdateCmd cmd) {
        Post post = postRepository.findById(cmd.postId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found"));
        Category category = categoryRepository.findById(cmd.categoryId())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "validation.post.categoryId.notNull"));

        // 제목/슬러그
        if (cmd.title() != null && !cmd.title().equals(post.getTitle())) {
            String base = slugify(cmd.title());
            String newSlug = base;
            for (int i = 2; postRepository.existsBySlug(newSlug) && !newSlug.equals(post.getSlug()); i++) {
                if (i > 1000) throw new FieldValidationException(Map.of("title", "사용 불가능한 제목입니다."));
                newSlug = base + "-" + i;
            }
            post.renameTo(cmd.title(), newSlug);
        } else if (cmd.title() != null) {
            post.renameTo(cmd.title(), post.getSlug());
        }

        // 태그
        List<String> inputTags = (cmd.tags() == null ? List.<String>of() : cmd.tags()).stream()
                .map(t -> t.trim().toLowerCase())
                .filter(t -> !t.isBlank())
                .distinct()
                .toList();

        List<Tag> existing = inputTags.isEmpty() ? List.of() : tagRepository.findByNameIn(inputTags);
        Map<String, Tag> map = existing.stream().collect(Collectors.toMap(Tag::getName, t -> t));

        List<Tag> ordered = new ArrayList<>(inputTags.size());
        for (String name : inputTags) {
            ordered.add(map.computeIfAbsent(name, n -> tagRepository.save(new Tag(n))));
        }
        post.setTagsInOrder(ordered);

        // 본문 리라이트
        String mainRaw = (cmd.mainContent() == null) ? "" : cmd.mainContent();
        List<String> contentIds = extractFileIdsFromMarkdown(mainRaw);
        TokenRewriteResult rewrite = rewriteFileTokens(mainRaw);
        post.rewriteBody(rewrite.rewrittenMarkdown());

        // 헤더/카테고리/링크/공개
        StoredFile head = (cmd.headImageId() == null || cmd.headImageId().isBlank())
                ? null : storedFileRepository.getReferenceById(cmd.headImageId());
        post.reviseHead(head, cmd.headContent());
        post.moveTo(category, category.getLink());
        post.publish(cmd.published());

        // 파일 참조 원자 교체
        fileRefService.replaceAll(new FileRefService.ReplaceAllCmd(
                FileOwnerType.POST, post.getId(), FilePurpose.HEAD_IMAGE,
                head == null ? List.of() : List.of(head.getId()),
                post.getId() + " headimage"
        ));
        fileRefService.replaceAll(new FileRefService.ReplaceAllCmd(
                FileOwnerType.POST, post.getId(), FilePurpose.CONTENT,
                contentIds,
                post.getId() + " content"
        ));

        postRepository.save(post);
        return PostUpdateResultDTO.builder()
                .postId(post.getId())
                .slug(post.getSlug())
                .build();
    }

    // --------------------------------- Delete -----------------------------------

    @Override
    public void delete(String postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.post.not_found"));

        // 1) 파일 REF 모두 해제 (refCount 감소)
        fileRefService.detachAll(FileOwnerType.POST, post.getId());

        // 2) 좋아요 일괄 삭제
        postLikeRepository.deleteAllByPostId(postId);

        // 3) 태그/퀴즈: cascade + orphanRemoval
        // 4) 댓글: 엔티티에서 @OnDelete(CASCADE) → DB 레벨 제거

        // 5) 본체 삭제
        postRepository.delete(post);
    }

    // ------------------------------ Internal utils ------------------------------

    private static String[] resolveSort(org.springframework.data.domain.Pageable pageable) {
        if (pageable.getSort().isUnsorted()) return new String[]{"createdAt", "DESC"};
        var o = pageable.getSort().iterator().next();
        String key = switch (o.getProperty()) {
            case "updatedAt" -> "updatedAt";
            case "viewCount" -> "viewCount";
            case "likeCount" -> "likeCount";
            case "title" -> "title";
            default -> "createdAt";
        };
        String dir = o.getDirection().isAscending() ? "ASC" : "DESC";
        return new String[]{key, dir};
    }

    /**
     * 본문에서 file 토큰 + CDN URL 모두에서 FL-...를 뽑아 중복 제거(선착순 유지)
     */
    private static List<String> extractFileIdsFromMarkdown(String markdown) {
        if (markdown == null || markdown.isBlank()) return List.of();

        List<String> ids = new ArrayList<>();

        Matcher m1 = FILE_TOKEN.matcher(markdown);
        while (m1.find()) ids.add(m1.group(1));

        Matcher m2 = CDN_FL_IN_URL.matcher(markdown);
        while (m2.find()) ids.add(m2.group(1));

        LinkedHashSet<String> orderedUnique = new LinkedHashSet<>(ids);
        return new ArrayList<>(orderedUnique);
    }

    private TokenRewriteResult rewriteFileTokens(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return new TokenRewriteResult("", List.of());
        }

        // 수집
        List<String> idsInOrder = new ArrayList<>();
        Matcher m = FILE_TOKEN.matcher(markdown);
        while (m.find()) {
            String id = m.group(1);
            if (id != null) idsInOrder.add(id);
        }
        if (idsInOrder.isEmpty()) return new TokenRewriteResult(markdown, List.of());

        // 메타/variants 로드
        List<StoredFile> files = storedFileRepository.findAllById(idsInOrder);
        Map<String, StoredFile> fileById = new HashMap<>(files.size());
        for (StoredFile f : files) fileById.put(f.getId(), f);

        var variants = fileVariantRepository.findActiveByFileIdIn(idsInOrder);
        Map<String, Map<String, String>> vmap = new HashMap<>();
        variants.forEach(v -> {
            String fid = v.getOriginal().getId();
            vmap.computeIfAbsent(fid, k -> new HashMap<>())
                    .put(v.getKind().name(), v.getStorageKey());
        });

        // 치환
        StringBuilder out = new StringBuilder();
        m.reset();
        List<String> usedInOrder = new ArrayList<>();

        while (m.find()) {
            String id = m.group(1);
            String req = normalizeKind(m.group(2));
            StoredFile f = fileById.get(id);

            if (f == null) {
                m.appendReplacement(out, Matcher.quoteReplacement(m.group()));
                continue;
            }

            String ct = safeLower(f.getContentType());
            String base = ensureTrailingSlash(FileUrlResolver.toBaseDirUrl(cdnProperties, f.getStoragePath()));

            String finalUrl;
            if ("image/svg+xml".equals(ct)) {
                finalUrl = base + "original";
            } else {
                Map<String, String> kinds = vmap.getOrDefault(id, Map.of());
                String pickedKey = null;

                if (req != null) pickedKey = kinds.get(req);
                if (pickedKey == null) {
                    for (String k : DEFAULT_ORDER) {
                        if (kinds.containsKey(k)) {
                            pickedKey = kinds.get(k);
                            break;
                        }
                    }
                }
                finalUrl = (pickedKey != null)
                        ? FileUrlResolver.toCdnUrl(cdnProperties, pickedKey)
                        : base + "original";
            }

            usedInOrder.add(id);

            String needle = (m.group(2) != null)
                    ? "file://" + id + "\\?t=" + Pattern.quote(m.group(2))
                    : "file://" + id;
            String replaced = m.group().replaceFirst(needle, Matcher.quoteReplacement(finalUrl));
            m.appendReplacement(out, Matcher.quoteReplacement(replaced));
        }
        m.appendTail(out);

        return new TokenRewriteResult(out.toString(), usedInOrder);
    }

    private static String normalizeKind(String t) {
        if (t == null || t.isBlank()) return null;
        String x = t.trim().toUpperCase().replace('-', '_');
        return switch (x) {
            case "THUMB_512", "THUMB_256", "WEBP", "AVIF" -> x;
            default -> null;
        };
    }

    private static String ensureTrailingSlash(String s) {
        if (s == null || s.isBlank()) return s;
        return s.endsWith("/") ? s : (s + "/");
    }

    private static String safeLower(String s) {
        return (s == null) ? "" : s.toLowerCase();
    }

    private record TokenRewriteResult(String rewrittenMarkdown, List<String> usedFileIdsInOrder) {
    }
}
