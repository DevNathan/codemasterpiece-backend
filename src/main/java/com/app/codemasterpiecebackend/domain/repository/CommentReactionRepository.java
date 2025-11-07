package com.app.codemasterpiecebackend.domain.repository;

import com.app.codemasterpiecebackend.domain.entity.comment.CommentReaction;
import org.apache.ibatis.annotations.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReaction, String> {

    @Query(value = """
            WITH params AS (
                SELECT 
                    cast(:commentId as char(29))        AS comment_id,
                    cast(:actorProvider as varchar(16)) AS actor_provider,
                    cast(:actorId as varchar(100))      AS actor_id,
                    cast(:value as varchar(16))         AS new_value
            ),
            upsert AS (
                INSERT INTO tbl_comment_reaction (reaction_id, comment_id, actor_provider, actor_id, value, created_at, updated_at)
                SELECT 
                    lpad(replace(cast(gen_random_uuid() as text),'-',''), 29, '0') as reaction_id,
                    p.comment_id, p.actor_provider, p.actor_id, p.new_value::varchar(16), now(), now()
                FROM params p
                WHERE p.new_value IS NOT NULL
                  AND p.actor_id IS NOT NULL
                ON CONFLICT (comment_id, actor_provider, actor_id)
                DO UPDATE SET value = EXCLUDED.value, updated_at = now()
                RETURNING value
            ),
            del AS (
                DELETE FROM tbl_comment_reaction r
                USING params p
                WHERE p.new_value IS NULL
                  AND p.actor_id IS NOT NULL
                  AND r.comment_id = p.comment_id
                  AND r.actor_provider = p.actor_provider
                  AND r.actor_id = p.actor_id
                RETURNING r.comment_id
            )
            SELECT 
                COALESCE((
                    SELECT u.value FROM upsert u
                    UNION ALL
                    SELECT NULL::varchar(16) WHERE EXISTS (SELECT 1 FROM del)
                    UNION ALL
                    SELECT r2.value 
                    FROM tbl_comment_reaction r2 
                    JOIN params p2 ON p2.comment_id = r2.comment_id
                                  AND p2.actor_provider = r2.actor_provider
                                  AND p2.actor_id = r2.actor_id
                    WHERE NOT EXISTS (SELECT 1 FROM upsert) AND NOT EXISTS (SELECT 1 FROM del)
                    LIMIT 1
                ), NULL) AS my_reaction
            """, nativeQuery = true)
    String reactAndGetMyReaction(
            @Param("commentId") String commentId,
            @Param("actorProvider") String actorProvider,
            @Param("actorId") String actorId,
            @Param("value") String value
    );
}
