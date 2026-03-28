package com.app.codemasterpiecebackend.domain.post.repository;

import com.app.codemasterpiecebackend.domain.post.dto.PostResult;
import com.app.codemasterpiecebackend.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, String>, PostQueryRepository {
    boolean existsBySlug(String slug);

    @Modifying
    @Query("update Post p set p.likeCount = p.likeCount + :delta where p.id = :postId")
    void bumpLikeCount(@Param("postId") String postId, @Param("delta") int delta);

    @Query("select p.likeCount from Post p where p.id = :postId")
    Integer findLikeCountOnly(@Param("postId") String postId);

    @Modifying
    @Query("update Post p set p.viewCount = p.viewCount + :delta where p.id = :postId")
    void bumpViewCount(@Param("postId") String postId, @Param("delta") int delta);

    boolean existsByCategoryId(String categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Post p set p.link = :newLink where p.category.id = :categoryId")
    void bulkUpdateLinkByCategoryId(@Param("categoryId") String categoryId,
                                   @Param("newLink") String newLink);

    @Query("select new com.app.codemasterpiecebackend.domain.post.dto.PostResult$Sitemap(p.slug, p.updatedAt) from Post p order by p.title asc")
    List<PostResult.Sitemap> findSitemaps();
}
