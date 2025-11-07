package com.app.codemasterpiecebackend.domain.entity.file;

import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "tbl_file_ref",
        indexes = {
                @Index(name = "idx_fref_owner", columnList = "owner_type, owner_id"),
                @Index(name = "idx_fref_file", columnList = "file_id"),
                @Index(name = "idx_fref_owner_purpose", columnList = "owner_type, owner_id, purpose"),
                @Index(name = "idx_fref_sort", columnList = "owner_type, owner_id, purpose, sort_order")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class FileRef {

    @Id
    @PrefixedUlidId("FR")
    @Column(length = 29, name = "file_ref_id", columnDefinition = "char(29)", nullable = false, updatable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", referencedColumnName = "file_id",
            foreignKey = @ForeignKey(name = "fk_fref_file"))
    private StoredFile storedFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 32, nullable = false)
    private FileOwnerType ownerType;

    @Column(name = "owner_id", columnDefinition = "char(29)", nullable = false)
    private String ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", length = 32, nullable = false)
    private FilePurpose purpose;

    @Column(name = "sort_order")
    @Setter
    private Integer sortOrder;

    @Column(name = "display_name", length = 255)
    private String displayName;
}
