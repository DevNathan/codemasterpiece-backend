package com.app.codemasterpiecebackend.domain.entity.post;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * 태그 엔티티
 * <p>
 * - name: 고유 태그 이름 (중복 불가)
 * - id: 접두사 2 + '-' + ULID(26) = 총 29자
 */
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "tbl_tag",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tags_name", columnNames = "name")
        }
)
public class Tag {

    /** ULID 기반 식별자 ("TG-xxxxxxxx...") */
    @Id
    @PrefixedUlidId("TG")
    @Column(name = "tag_id", nullable = false, length = 29, columnDefinition = "CHAR(29)")
    @ToString.Include
    private String id;

    /** 태그 이름 (유니크) */
    @Column(name = "name", length = 60, nullable = false)
    @ToString.Include
    private String name;

    public Tag(String name) {
        this.name = name;
    }
}
