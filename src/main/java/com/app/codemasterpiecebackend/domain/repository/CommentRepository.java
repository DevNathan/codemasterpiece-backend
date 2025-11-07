package com.app.codemasterpiecebackend.domain.repository;

import com.app.codemasterpiecebackend.domain.entity.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    interface ParentBrief {
        String getPostId();

        int getDepth();
    }

    @Query("""
                select c.post.id as postId, c.depth as depth
                from Comment c
                where c.id = :parentId
            """)
    Optional<ParentBrief> findParentBrief(@Param("parentId") String parentId);

    // 부모까지 한 번에 (상향 정리 위해 parent 필요)
    @Query("""
                select c from Comment c
                left join fetch c.parent p
                where c.id = :id
            """)
    Optional<Comment> findWithParent(@Param("id") String id);

    // 살아있는(soft-deleted가 아닌) 직계 자식 존재 여부
    @Query("""
                select case when count(c) > 0 then true else false end
                from Comment c
                where c.parent.id = :parentId and c.deleted = false
            """)
    boolean existsActiveChild(@Param("parentId") String parentId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Comment c set c.deleted = true where c.id = :id and c.deleted = false")
    int softDelete(@Param("id") String id);

    // 상향 정리 시 다음 부모 id가 필요할 때 (부모를 삭제해버리기 전 lookup)
    @Query("select c.parent.id from Comment c where c.id = :id")
    Optional<String> findParentId(@Param("id") String id);

    @Modifying
    @Query("UPDATE Comment c SET c.hidden = :hidden WHERE c.id = :commentId")
    int updateHiddenById(@Param("commentId") String commentId, @Param("hidden") boolean hidden);
}
