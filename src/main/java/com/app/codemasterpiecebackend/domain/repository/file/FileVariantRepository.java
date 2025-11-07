package com.app.codemasterpiecebackend.domain.repository.file;

import com.app.codemasterpiecebackend.domain.entity.file.FileVariant;
import com.app.codemasterpiecebackend.domain.entity.file.FileVariantKind;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FileVariantRepository extends JpaRepository<FileVariant, String> {
    boolean existsByOriginal_idAndKind(String original_id, FileVariantKind kind);

    @Query("""
            select v
            from FileVariant v
            join fetch v.original o
            where o.id in :fileIds
              and v.status = com.app.codemasterpiecebackend.domain.entity.file.FileStatus.ACTIVE
            """)
    List<FileVariant> findActiveByFileIdIn(@Param("fileIds") List<String> fileIds);
}
