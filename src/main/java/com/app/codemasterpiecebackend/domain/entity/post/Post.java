package com.app.codemasterpiecebackend.domain.entity.post;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.base.BaseTimeEntity;
import com.app.codemasterpiecebackend.domain.entity.category.Category;
import com.app.codemasterpiecebackend.domain.entity.comment.Comment;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.*;

@Entity
@Table(
        name = "tbl_post",
        indexes = {
                // 링크 축 기본
                @Index(name = "idx_post_link", columnList = "link"),
                @Index(name = "idx_post_link_pub_created", columnList = "link, is_published, created_at"),
                // 기존 category 기반 인덱스는 유지하되, 점진적으로 link로 전환하면서 정리 고려
                @Index(name = "idx_post_cat_published", columnList = "category_id, is_published"),
                @Index(name = "idx_post_cat_created", columnList = "category_id, created_at"),
                @Index(name = "idx_post_cat_updated", columnList = "category_id, updated_at"),
                @Index(name = "idx_post_cat_title", columnList = "category_id, title"),
                @Index(name = "idx_post_cat_views", columnList = "category_id, view_count"),
                @Index(name = "idx_post_cat_likes", columnList = "category_id, like_count"),
                // 단일 정렬/검색
                @Index(name = "idx_post_created_at", columnList = "created_at"),
                @Index(name = "idx_post_updated_at", columnList = "updated_at"),
                @Index(name = "idx_post_title", columnList = "title"),
                @Index(name = "idx_post_view_count", columnList = "view_count"),
                @Index(name = "idx_post_like_count", columnList = "like_count")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_post_slug", columnNames = {"slug"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@ToString(exclude = "category")
@DynamicUpdate
public class Post extends BaseTimeEntity {

    /**
     * 게시글 ID (ULID + Prefix)
     */
    @Id
    @PrefixedUlidId("PO")
    @Column(name = "post_id", nullable = false, updatable = false, length = 29)
    private String id;

    /**
     * 슬러그 (유니크)
     */
    @Column(name = "slug", length = 200, nullable = false, unique = true)
    private String slug;

    /**
     * 제목
     */
    @Column(name = "title", length = 255, nullable = false, unique = false)
    private String title;

    /**
     * 카테고리 (필수)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", referencedColumnName = "category_id",
            foreignKey = @ForeignKey(name = "fk_post_category"))
    private Category category;

    /**
     * 헤더 이미지 (URL 또는 파일경로)
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "head_image_id",
            referencedColumnName = "file_id",
            foreignKey = @ForeignKey(name = "fk_post_image_file")
    )
    private StoredFile headImage;

    /**
     * 헤더 콘텐츠(요약)
     */
    @Column(name = "head_content", length = 1000)
    private String headContent;

    /**
     * 본문
     */
    @Column(name = "main_content", columnDefinition = "TEXT")
    private String mainContent;

    @Column(name = "link", length = 100, nullable = false)
    private String link;

    /**
     * 태그
     */
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<PostTag> postTags = new ArrayList<>();

    /**
     * 조회수 (반정규화)
     */
    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    /**
     * 좋아요 수 (반정규화)
     */
    @Column(name = "like_count", nullable = false)
    @Builder.Default
    private Long likeCount = 0L;

    /**
     * 공개 여부
     */
    @Column(name = "is_published", nullable = false)
    @Builder.Default
    private boolean published = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Comment> commentList = new ArrayList<>();

//    @OneToMany(
//            mappedBy = "post",
//            cascade = CascadeType.ALL,
//            orphanRemoval = true,
//            fetch = FetchType.LAZY
//    )
//    @OrderBy("sortOrder ASC")
//    @Builder.Default
//    private List<QuizItem> quizList = new ArrayList<>();

    public List<Tag> getTagsInOrder() {
        if (postTags == null || postTags.isEmpty()) {
            return List.of();
        }
        return postTags.stream()
                .sorted(Comparator.comparingInt(PostTag::getSortOrder))
                .map(PostTag::getTag)
                .toList();
    }

    public void setTagsInOrder(List<Tag> tagsInOrder) {
        if (tagsInOrder == null) {
            this.postTags.clear();
            return;
        }

        // 대상 태그 ID 순서 매핑
        Map<String, Integer> desiredOrder = new LinkedHashMap<>();
        int o = 1;
        for (Tag t : tagsInOrder) {
            desiredOrder.put(t.getId(), o++);
        }

        // 현재 존재하는 PostTag 매핑
        Map<String, PostTag> existing = new HashMap<>();
        for (PostTag pt : this.postTags) {
            existing.put(pt.getTag().getId(), pt);
        }

        // 삭제 대상 먼저 제거
        this.postTags.removeIf(pt -> !desiredOrder.containsKey(pt.getTag().getId()));

        // 정렬 + 신규 추가
        for (Map.Entry<String, Integer> e : desiredOrder.entrySet()) {
            String tid = e.getKey();
            int order = e.getValue();

            PostTag pt = existing.get(tid);
            if (pt != null) {
                pt.reorderTo(order);
            } else {
                Tag t = tagsInOrder.stream()
                        .filter(x -> x.getId().equals(tid))
                        .findFirst()
                        .orElseThrow();

                this.postTags.add(PostTag.of(this, t, order));
            }
        }
    }


    /**
     * 제목/슬러그 동시 변경
     */
    public void renameTo(String newTitle, String newSlug) {
        if (newTitle == null || newTitle.isBlank()) throw new IllegalArgumentException("title blank");
        if (newSlug == null || newSlug.isBlank()) throw new IllegalArgumentException("slug blank");
        this.title = newTitle;
        this.slug = newSlug;
    }

    /**
     * 카테고리 이동 + 링크 축 동기화
     */
    public void moveTo(Category newCategory, String linkAxis) {
        if (newCategory == null) throw new IllegalArgumentException("category null");
        if (linkAxis == null || linkAxis.isBlank()) throw new IllegalArgumentException("link blank");
        this.category = newCategory;
        this.link = linkAxis;
    }

    /**
     * 헤더(요약/이미지) 교체 — null 허용(삭제)
     */
    public void reviseHead(StoredFile newHeadImage, String newHeadContent) {
        this.headImage = newHeadImage;        // null 허용
        this.headContent = (newHeadContent != null && !newHeadContent.isBlank()) ? newHeadContent : null;
    }

    /**
     * 본문 교체(리라이트 결과 저장)
     */
    public void rewriteBody(String rewrittenMarkdown) {
        this.mainContent = (rewrittenMarkdown != null) ? rewrittenMarkdown : "";
    }

    /**
     * 공개 상태 토글/지정
     */
    public void publish(boolean publish) {
        this.published = publish;
    }

    /**
     * 태그 재구성(입력 순서 고정)
     */
    public void retag(List<Tag> tagsInOrder) {
        this.postTags.clear();
        if (tagsInOrder == null || tagsInOrder.isEmpty()) return;
        int i = 1;
        for (Tag t : tagsInOrder) {
            this.postTags.add(PostTag.of(this, t, i++));
        }
    }
}
