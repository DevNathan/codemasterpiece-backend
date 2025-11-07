package com.app.codemasterpiecebackend.service.category;

import com.app.codemasterpiecebackend.domain.dto.category.CategoryDTO;
import com.app.codemasterpiecebackend.domain.dto.category.CategorySitemapLinkDTO;
import com.app.codemasterpiecebackend.domain.entity.category.Category;
import com.app.codemasterpiecebackend.domain.entity.category.CategoryType;
import com.app.codemasterpiecebackend.domain.entity.file.FileOwnerType;
import com.app.codemasterpiecebackend.domain.entity.file.FilePurpose;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.app.codemasterpiecebackend.domain.repository.CategoryRepository;
import com.app.codemasterpiecebackend.domain.repository.PostRepository;
import com.app.codemasterpiecebackend.domain.repository.file.StoredFileRepository;
import com.app.codemasterpiecebackend.service.category.cmd.CategoryCreateCmd;
import com.app.codemasterpiecebackend.service.category.cmd.CategoryUpdateCmd;
import com.app.codemasterpiecebackend.service.category.cmd.MoveCategoryCmd;
import com.app.codemasterpiecebackend.service.filesystem.file.result.FileInfo;
import com.app.codemasterpiecebackend.service.filesystem.image.ImageService;
import com.app.codemasterpiecebackend.service.filesystem.ref.FileRefService;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.AttachCmd;
import com.app.codemasterpiecebackend.service.filesystem.ref.cmd.DetachCmd;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.MediaKind;
import com.app.codemasterpiecebackend.service.filesystem.support.upload.UploadCmd;
import com.app.codemasterpiecebackend.support.exception.AppException;
import com.app.codemasterpiecebackend.util.CdnProperties;
import com.app.codemasterpiecebackend.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    /** 간격 기반 정렬 갭. */
    private static final int GAP = 10;

    private final ImageService imageService;
    private final FileRefService fileRefService;
    private final CategoryRepository categoryRepository;
    private final StoredFileRepository fileRepository;
    private final CdnProperties cdnProperties;
    private final PostRepository postRepository;

    // =========================
    // C — CREATE
    // =========================

    /**
     * 카테고리 생성.
     * <ul>
     *   <li>정렬: {@code findMaxOrder(parent)} + GAP</li>
     *   <li>레벨: 부모레벨 + 1 (루트는 0)</li>
     *   <li>LINK 타입일 때만 link 유지, FOLDER는 link=null</li>
     *   <li>이미지 업로드가 있으면 파일 레퍼런스 attach</li>
     * </ul>
     */
    @Override
    public void create(CategoryCreateCmd cmd) {
        final String parentId = cmd.parentId();
        final String effectiveLink = (cmd.type() == CategoryType.LINK) ? cmd.link() : null;

        Category parentRef = null;
        int parentLevel = 0;
        if (parentId != null) {
            parentRef = categoryRepository.getReferenceById(parentId);
            parentLevel = categoryRepository.findLevelById(parentId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.parent_not_found"));
        }

        int nextOrder = categoryRepository.findMaxOrder(parentId) + GAP;
        int level = (parentRef == null) ? 0 : parentLevel + 1;

        Category entity = Category.builder()
                .name(cmd.name())
                .type(cmd.type())
                .parent(parentRef)
                .link(effectiveLink)
                .image(null)
                .sortOrder(nextOrder)
                .level(level)
                .build();

        try {
            categoryRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            final String msg = Optional.ofNullable(rootCause(e)).map(String::toLowerCase).orElse("");
            if (msg.contains("uq_category_parent_name"))
                throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_name");
            if (msg.contains("uq_category_link"))
                throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_link");
            if (msg.contains("fk_category_parent"))
                throw new AppException(HttpStatus.NOT_FOUND, "error.category.parent_not_found");
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "error.internal");
        }

        uploadAndAttachIconIfPresent(entity, cmd.image());
    }

    // =========================
    // R — READ
    // =========================

    /**
     * 전체 카테고리를 평탄화 조회 후, 부모-자식 트리로 재구성하여 레벨별 sortOrder 정렬을 보장한다.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getTree() {
        var flats = categoryRepository.findAllFlatOrderByLevelThenSort();
        if (flats.isEmpty()) return List.of();

        Map<String, CategoryDTO> nodeMap = new LinkedHashMap<>(flats.size());
        List<CategoryDTO> roots = new ArrayList<>();
        Map<String, String> urlCache = new HashMap<>(128); // fileId -> URL

        // 1) flat -> DTO (이미지 URL 포함)
        for (Category c : flats) {
            var dto = new CategoryDTO(
                    c.getId(),
                    c.getName(),
                    c.getType(),
                    c.getSortOrder(),
                    c.getLevel(),
                    c.getLink(),
                    resolveUrlCached(c.getImage(), urlCache),
                    new ArrayList<>()
            );
            nodeMap.put(c.getId(), dto);
        }

        // 2) 부모-자식 연결
        for (Category c : flats) {
            var node = nodeMap.get(c.getId());
            var parent = c.getParent();
            if (parent == null) {
                roots.add(node);
            } else {
                var pn = nodeMap.get(parent.getId());
                if (pn != null) pn.getChildren().add(node);
                else roots.add(node); // 방어
            }
        }

        // 3) 각 레벨 정렬
        Deque<List<CategoryDTO>> stack = new ArrayDeque<>();
        stack.push(roots);
        while (!stack.isEmpty()) {
            var layer = stack.pop();
            layer.sort(Comparator.comparingInt(CategoryDTO::getSortOrder));
            for (var n : layer) {
                var ch = n.getChildren();
                if (ch != null && !ch.isEmpty()) stack.push(ch);
            }
        }
        return roots;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategorySitemapLinkDTO> getSitemapLinks() {
        return categoryRepository.findSitemapLinks();
    }

    // =========================
    // U — UPDATE
    // =========================

    /**
     * 카테고리 수정.
     * <ul>
     *   <li>이름 변경: 동일 부모 내 중복 검사</li>
     *   <li>링크 변경: LINK 타입만, 중복 검사 후 포스트 link 축 일괄 동기화</li>
     *   <li>이미지 교체/삭제: detach → attach 흐름</li>
     * </ul>
     */
    @Override
    public void update(CategoryUpdateCmd cmd) {
        Category category = categoryRepository.findByIdForUpdate(cmd.categoryId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.not_found"));

        // 1) 이름
        if (cmd.name() != null && !cmd.name().equals(category.getName())) {
            String parentId = (category.getParent() != null) ? category.getParent().getId() : null;
            if (categoryRepository.existsByParentIdAndName(parentId, cmd.name(), category.getId())) {
                throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_name");
            }
            category.rename(cmd.name());
        }

        // 2) 링크
        if (category.getType() == CategoryType.LINK) {
            if (cmd.link() != null && !cmd.link().equals(category.getLink())) {
                if (categoryRepository.existsByLink(cmd.link())) {
                    throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_link");
                }
                category.changeLink(cmd.link());
                // 포스트 링크 축 동기화
                postRepository.bulkUpdateLinkByCategoryId(category.getId(), cmd.link());
            }
        } else {
            // FOLDER는 링크 사용 안 함
            category.changeLink(null);
        }

        // 3) 이미지
        if (cmd.removeImage()) {
            detachIconIfExists(category);
            return;
        }
        if (cmd.image() != null && !cmd.image().isEmpty()) {
            detachIconIfExists(category);
            uploadAndAttachIconIfPresent(category, cmd.image());
        }
    }

    // =========================
    // (특수) MOVE
    // =========================

    /**
     * 카테고리 이동.
     * <ul>
     *   <li>사이클 방지: 새 부모가 본인 서브트리면 409</li>
     *   <li>정렬: 인접 중간값 전략, 갭 없으면 재시퀀스 후 재계산</li>
     *   <li>부모 변경 시 서브트리 전체 level Δ 반영</li>
     *   <li>동일 부모 내 이름 중복 방지</li>
     * </ul>
     */
    @Override
    public void move(MoveCategoryCmd cmd) {
        Category cat = categoryRepository.findByIdForUpdate(cmd.categoryId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.not_found"));

        String newParentId = cmd.newParentId();

        // 1) 사이클 방지
        if (newParentId != null && categoryRepository.isDescendantOf(newParentId, cat.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.cycle");
        }

        // 2) 새 부모/레벨
        Category newParent = null;
        int newLevel = 0;
        if (newParentId != null) {
            newParent = categoryRepository.getReferenceById(newParentId);
            newLevel = categoryRepository.findLevelById(newParentId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.parent_not_found")) + 1;
        }

        // 3) 형제 조회 (본인 제외한 리스트 기준으로 목표 인덱스 계산)
        List<Category> siblings = categoryRepository.findSiblingsForUpdate(newParentId);
        int targetIndex = resolveTargetIndex(siblings, cmd, cat.getId());

        // 4) 정렬값 계산(갭 전략)
        int newOrder = computeOrderWithGap(siblings, targetIndex, GAP);
        if (newOrder == Integer.MIN_VALUE) {
            // 갭 없음 → 리시퀀스 후 재계산
            resequence(newParentId, siblings, GAP);
            siblings = categoryRepository.findSiblingsForUpdate(newParentId);
            targetIndex = resolveTargetIndex(siblings, cmd, cat.getId());
            newOrder = computeOrderWithGap(siblings, targetIndex, GAP);
        }

        // 5) level Δ 서브트리 반영
        int levelDelta = newLevel - cat.getLevel();
        if (levelDelta != 0) {
            var subtreeIds = categoryRepository.findSubtreeIds(cat.getId());
            categoryRepository.bulkBumpLevels(subtreeIds, levelDelta);
        }

        // 6) 동일 부모 내 이름 중복
        if (categoryRepository.existsByParentIdAndName(newParentId, cat.getName(), cat.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_name");
        }

        // 7) 반영
        cat.setParent(newParent);
        cat.setLevel(newLevel);
        cat.changeOrder(newOrder);
    }

    // =========================
    // D — DELETE
    // =========================

    /**
     * 카테고리 삭제.
     * <ul>
     *   <li>자식 있으면 409</li>
     *   <li>게시글이 참조 중이면 409</li>
     *   <li>이미지 연결돼 있으면 detach 후 삭제</li>
     * </ul>
     */
    @Override
    public void delete(String categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.not_found"));

        if (!category.getChildren().isEmpty()) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.has_children");
        }
        if (postRepository.existsByCategoryId(categoryId)) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.in_use");
        }

        detachIconIfExists(category);
        categoryRepository.delete(category);
    }

    // =========================
    // Private Helpers
    // =========================

    private String resolveUrlCached(StoredFile sf, Map<String, String> cache) {
        if (sf == null) return null;
        return cache.computeIfAbsent(sf.getId(),
                id -> FileUrlResolver.toFileUrl(cdnProperties, FileInfo.from(sf)));
    }

    private void uploadAndAttachIconIfPresent(Category entity, MultipartFile image) {
        if (image == null || image.isEmpty()) return;

        var info = imageService.upload(toUploadCmd(image));
        var fileEntity = fileRepository.getReferenceById(info.fileId());

        fileRefService.attach(AttachCmd.builder()
                .fileId(info.fileId())
                .ownerType(FileOwnerType.CATEGORY)
                .ownerId(entity.getId())
                .purpose(FilePurpose.ICON)
                .displayName(entity.getId() + " icon")
                .build());

        entity.connectImage(fileEntity);

        categoryRepository.saveAndFlush(entity);
    }

    private void detachIconIfExists(Category category) {
        if (category.getImage() == null) return;
        fileRefService.detach(new DetachCmd(category.getId(), category.getImage().getId()));
        category.clearImage();

        categoryRepository.saveAndFlush(category);
    }

    /** target index 산정: newIndex 우선, 없으면 before/after 기준, 기본 맨뒤. */
    private int resolveTargetIndex(List<Category> siblings, MoveCategoryCmd cmd, String selfId) {
        List<Category> list = siblings.stream()
                .filter(s -> !s.getId().equals(selfId))
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .toList();

        Integer newIndex = cmd.newIndex();
        if (newIndex != null) return clamp(newIndex, 0, list.size());

        String beforeId = cmd.beforeId();
        if (beforeId != null) {
            int i = indexOfId(list, beforeId);
            if (i >= 0) return i;
        }

        String afterId = cmd.afterId();
        if (afterId != null) {
            int i = indexOfId(list, afterId);
            if (i >= 0) return clamp(i + 1, 0, list.size());
        }

        return list.size(); // 기본: 맨 뒤
    }

    /**
     * 인접 형제 사이 중간값으로 sort_order 계산.
     * 갭이 없으면 {@code Integer.MIN_VALUE} 반환하여 리시퀀스 트리거.
     */
    private int computeOrderWithGap(List<Category> siblings, int idx, int gap) {
        List<Category> list = siblings.stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .toList();

        Integer left = (idx - 1 >= 0 && !list.isEmpty()) ? list.get(idx - 1).getSortOrder() : null;
        Integer right = (idx < list.size()) ? list.get(idx).getSortOrder() : null;

        if (left == null && right == null) return gap;       // 빈 부모 → 첫 값
        if (left == null)                 return right - gap; // 맨 앞 삽입
        if (right == null)                return left + gap;  // 맨 뒤 삽입

        int diff = right - left;
        if (diff <= 1) return Integer.MIN_VALUE;              // 갭 없음 → 리시퀀스 필요
        return left + diff / 2;
        // NOTE: diff 홀/짝 상관없이 중간값. 정수 나눗셈(내림)으로 안정적.
    }

    /** parent 기준 10,20,30…으로 리시퀀스. 실제 시퀀싱은 DB 레이어에 위임. */
    private void resequence(String parentId, List<Category> siblings, int gap) {
        categoryRepository.resequenceByParent(parentId, gap);
    }

    private UploadCmd toUploadCmd(MultipartFile file) {
        Supplier<InputStream> supplier = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return UploadCmd.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .content(supplier)
                .kind(MediaKind.IMAGE)
                .build();
    }

    private String rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return (cur.getMessage() != null) ? cur.getMessage() : t.getMessage();
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int indexOfId(List<Category> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), id)) return i;
        }
        return -1;
    }
}
