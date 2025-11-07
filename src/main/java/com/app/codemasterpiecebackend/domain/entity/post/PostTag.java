package com.app.codemasterpiecebackend.domain.entity.post;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@Entity
@Table(
        name = "tbl_post_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_posttag_post_sort", columnNames = {"post_id", "sort_order"}),
                @UniqueConstraint(name = "uq_posttag_post_tag", columnNames = {"post_id", "tag_id"})
        },
        indexes = {
                @Index(name = "idx_posttag_post_sort", columnList = "post_id, sort_order")
        }
)
public class PostTag {

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "post_id", length = 29, nullable = false, columnDefinition = "CHAR(29)")
        private String postId;
        @Column(name = "tag_id", length = 29, nullable = false, columnDefinition = "CHAR(29)")
        private String tagId;
    }

    @EmbeddedId
    private Id id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("postId")
    @JoinColumn(name = "post_id", referencedColumnName = "post_id",
            foreignKey = @ForeignKey(name = "fk_posttag_post"))
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("tagId")
    @JoinColumn(name = "tag_id", referencedColumnName = "tag_id",
            foreignKey = @ForeignKey(name = "fk_posttag_tag"))
    private Tag tag;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public static PostTag of(Post post, Tag tag, int sortOrder) {
        return PostTag.builder()
                .id(new Id(post.getId(), tag.getId()))
                .post(post)
                .tag(tag)
                .sortOrder(sortOrder)
                .build();
    }

    public void reorderTo(int newOrder) {
        this.sortOrder = newOrder;
    }
}
