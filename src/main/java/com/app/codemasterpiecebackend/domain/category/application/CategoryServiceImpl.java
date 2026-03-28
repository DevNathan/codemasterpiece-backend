package com.app.codemasterpiecebackend.domain.category.application;

import com.app.codemasterpiecebackend.domain.category.dto.CategoryDTO;
import com.app.codemasterpiecebackend.domain.category.dto.CategorySitemapLinkDTO;
import com.app.codemasterpiecebackend.domain.category.entity.Category;
import com.app.codemasterpiecebackend.domain.category.entity.CategoryType;
import com.app.codemasterpiecebackend.domain.category.repository.CategoryRepository;
import com.app.codemasterpiecebackend.domain.file.ref.application.FileRefCommand;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FileOwnerType;
import com.app.codemasterpiecebackend.domain.file.ref.entity.FilePurpose;
import com.app.codemasterpiecebackend.domain.file.core.entity.StoredFile;
import com.app.codemasterpiecebackend.domain.post.repository.PostRepository;
import com.app.codemasterpiecebackend.domain.file.core.repository.StoredFileRepository;
import com.app.codemasterpiecebackend.global.support.exception.AppException;
import com.app.codemasterpiecebackend.global.util.CdnProperties;
import com.app.codemasterpiecebackend.global.util.FileUrlResolver;
import com.app.codemasterpiecebackend.domain.file.core.dto.FileInfo;
import com.app.codemasterpiecebackend.domain.file.media.application.ImageService;
import com.app.codemasterpiecebackend.domain.file.ref.application.FileRefService;
import com.app.codemasterpiecebackend.domain.file.core.application.StoreCmd;
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
public class CategoryServiceImpl implements CategoryService {

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

    @Override
    @Transactional
    public void create(CategoryCommand.Create cmd) {
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

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getTree() {
        var flats = categoryRepository.findAllFlatOrderByLevelThenSort();
        if (flats.isEmpty()) return List.of();

        Map<String, CategoryDTO> nodeMap = new LinkedHashMap<>(flats.size());
        List<CategoryDTO> roots = new ArrayList<>();
        Map<String, String> urlCache = new HashMap<>(128);

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

        for (Category c : flats) {
            var node = nodeMap.get(c.getId());
            var parent = c.getParent();
            if (parent == null) {
                roots.add(node);
            } else {
                var pn = nodeMap.get(parent.getId());
                if (pn != null) pn.getChildren().add(node);
                else roots.add(node);
            }
        }

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

    @Override
    @Transactional
    public void update(CategoryCommand.Update cmd) {
        Category category = categoryRepository.findByIdForUpdate(cmd.categoryId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.not_found"));

        if (cmd.name() != null && !cmd.name().equals(category.getName())) {
            String parentId = (category.getParent() != null) ? category.getParent().getId() : null;
            if (categoryRepository.existsByParentIdAndName(parentId, cmd.name(), category.getId())) {
                throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_name");
            }
            category.rename(cmd.name());
        }

        if (category.getType() == CategoryType.LINK) {
            if (cmd.link() != null && !cmd.link().equals(category.getLink())) {
                if (categoryRepository.existsByLink(cmd.link())) {
                    throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_link");
                }
                category.changeLink(cmd.link());
                postRepository.bulkUpdateLinkByCategoryId(category.getId(), cmd.link());
            }
        } else {
            category.changeLink(null);
        }

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

    @Override
    @Transactional
    public void move(CategoryCommand.Move cmd) {
        Category cat = categoryRepository.findByIdForUpdate(cmd.categoryId())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.not_found"));

        String newParentId = cmd.newParentId();

        if (newParentId != null && (newParentId.equals(cat.getId()) || categoryRepository.isDescendantOf(newParentId, cat.getId()))) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.cycle");
        }

        Category newParent = null;
        int newLevel = 0;
        if (newParentId != null) {
            newParent = categoryRepository.getReferenceById(newParentId);
            newLevel = categoryRepository.findLevelById(newParentId)
                    .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "error.category.parent_not_found")) + 1;
        }

        if (categoryRepository.existsByParentIdAndName(newParentId, cat.getName(), cat.getId())) {
            throw new AppException(HttpStatus.CONFLICT, "error.category.duplicate_name");
        }

        List<Category> siblings = categoryRepository.findSiblingsForUpdate(newParentId).stream()
                .filter(c -> !c.getId().equals(cat.getId()))
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .toList();

        int targetIndex = resolveTargetIndex(siblings, cmd, cat.getId());
        int newOrder = computeOrderWithGap(siblings, targetIndex);

        if (newOrder == Integer.MIN_VALUE) {
            categoryRepository.resequenceByParent(newParentId, GAP);

            if (newParentId != null) {
                newParent = categoryRepository.getReferenceById(newParentId);
            }
            siblings = categoryRepository.findSiblingsForUpdate(newParentId).stream()
                    .filter(c -> !c.getId().equals(cat.getId()))
                    .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                    .toList();

            targetIndex = resolveTargetIndex(siblings, cmd, cat.getId());
            newOrder = computeOrderWithGap(siblings, targetIndex);
        }

        int levelDelta = newLevel - cat.getLevel();
        cat.setParent(newParent);
        cat.setLevel(newLevel);
        cat.changeOrder(newOrder);
        categoryRepository.saveAndFlush(cat);

        if (levelDelta != 0) {
            List<String> subtreeIds = categoryRepository.findSubtreeIds(cat.getId()).stream()
                    .filter(id -> !id.equals(cat.getId()))
                    .toList();
            if (!subtreeIds.isEmpty()) {
                categoryRepository.bulkBumpLevels(subtreeIds, levelDelta);
            }
        }
    }

    // =========================
    // D — DELETE
    // =========================

    @Override
    @Transactional
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

        var info = imageService.upload(toStoreCmd(image));
        var fileEntity = fileRepository.getReferenceById(info.fileId());

        fileRefService.attach(FileRefCommand.Attach.builder()
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
        fileRefService.detach(new FileRefCommand.Detach(category.getId(), category.getImage().getId()));
        category.clearImage();

        categoryRepository.saveAndFlush(category);
    }

    private int resolveTargetIndex(List<Category> siblings, CategoryCommand.Move cmd, String selfId) {
        List<Category> list = siblings.stream()
                .filter(s -> !s.getId().equals(selfId))
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .toList();

        Integer newIndex = cmd.newIndex();
        if (newIndex != null) return clamp(newIndex, list.size());

        String beforeId = cmd.beforeId();
        if (beforeId != null) {
            int i = indexOfId(list, beforeId);
            if (i >= 0) return i;
        }

        String afterId = cmd.afterId();
        if (afterId != null) {
            int i = indexOfId(list, afterId);
            if (i >= 0) return clamp(i + 1, list.size());
        }

        return list.size();
    }

    private int computeOrderWithGap(List<Category> siblings, int idx) {
        List<Category> list = siblings.stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId))
                .toList();

        Integer left = (idx - 1 >= 0 && !list.isEmpty()) ? list.get(idx - 1).getSortOrder() : null;
        Integer right = (idx < list.size()) ? list.get(idx).getSortOrder() : null;

        if (left == null && right == null) return CategoryServiceImpl.GAP;
        if (left == null) return right - CategoryServiceImpl.GAP;
        if (right == null) return left + CategoryServiceImpl.GAP;

        int diff = right - left;
        if (diff <= 1) return Integer.MIN_VALUE;
        return left + diff / 2;
    }

    private StoreCmd toStoreCmd(MultipartFile file) {
        Supplier<InputStream> supplier = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
        return StoreCmd.builder()
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .content(supplier)
                .profileHint("ICON")
                .build();
    }

    private String rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return (cur.getMessage() != null) ? cur.getMessage() : t.getMessage();
    }

    private static int clamp(int v, int max) {
        return Math.max(0, Math.min(max, v));
    }

    private static int indexOfId(List<Category> list, String id) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), id)) return i;
        }
        return -1;
    }
}