package com.app.codemasterpiecebackend.domain.entity.category;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import com.app.codemasterpiecebackend.domain.entity.file.StoredFile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "tbl_category",
        indexes = {
                @Index(name = "idx_category_type", columnList = "type")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_category_parent_name", columnNames = {"parent_id", "name"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Builder
@DynamicUpdate
public class Category {

    @Id
    @PrefixedUlidId("CT")
    @Column(name = "category_id", nullable = false, updatable = false, length = 29)
    private String id;

    @Column(name = "name", nullable = false, length = 100, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CategoryType type;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "category_id",
            foreignKey = @ForeignKey(name = "fk_category_parent"))
    @JsonIgnore
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    private List<Category> children = new ArrayList<>();

    @Column(name = "link", unique = true, length = 255)
    private String link;

    /**
     * 기존 String → 연관관계
     */
    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
            name = "image_file_id",
            columnDefinition = "char(29)",
            referencedColumnName = "file_id",
            foreignKey = @ForeignKey(name = "fk_category_image_file")
    )
    private StoredFile image;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Setter
    @Column(name = "level", nullable = false)
    private int level;

    public void connectImage(StoredFile file) {
        this.image = file;
    }

    public void clearImage() {
        this.image = null;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public void changeLink(String newLink) {
        this.link = newLink;
    }

    public void changeOrder(int newOrder) {
        this.sortOrder = newOrder;
    }
}
