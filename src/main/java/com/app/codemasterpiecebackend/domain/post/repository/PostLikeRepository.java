package com.app.codemasterpiecebackend.domain.post.repository;

import com.app.codemasterpiecebackend.domain.post.entity.PostLike;
import com.app.codemasterpiecebackend.domain.shared.security.ActorProvider;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {
    boolean existsByPost_IdAndActorProviderAndActorId(String postId, ActorProvider provider, String actorId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        delete from PostLike pl
         where pl.post.id = :postId
           and pl.actorProvider = :provider
           and pl.actorId = :actorId
    """)
    int deleteActorLike(@Param("postId") String postId,
                        @Param("provider") ActorProvider provider,
                        @Param("actorId") String actorId);

    @Modifying
    @Query("delete from PostLike pl where pl.post.id = :postId")
    void deleteAllByPostId(String postId);
}
