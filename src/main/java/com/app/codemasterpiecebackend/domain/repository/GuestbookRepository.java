package com.app.codemasterpiecebackend.domain.repository;

import com.app.codemasterpiecebackend.domain.entity.guestbook.GuestbookEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GuestbookRepository extends JpaRepository<GuestbookEntry, String> {
    @Query("""
              select g from GuestbookEntry g
              where (:hasCursor = false)
                 or (g.createdAt < :lastAt)
                 or (g.createdAt = :lastAt and g.id < :lastId)
              order by g.createdAt desc, g.id desc
            """)
    List<GuestbookEntry> findSlice(
            @Param("hasCursor") boolean hasCursor,
            @Param("lastAt") LocalDateTime lastAt,
            @Param("lastId") String lastId,
            Pageable pageable
    );
}
