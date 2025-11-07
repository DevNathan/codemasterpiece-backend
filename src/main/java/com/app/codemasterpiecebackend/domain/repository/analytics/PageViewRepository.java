package com.app.codemasterpiecebackend.domain.repository.analytics;

import com.app.codemasterpiecebackend.domain.dto.analytics.PvAggDto;
import com.app.codemasterpiecebackend.domain.entity.analytics.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface PageViewRepository extends JpaRepository<PageView, String> {

    /**
     * [from, to) 범위의 집계. 모든 파라미터는 UTC Instant.
     * occurred_at 컬럼은 TIMESTAMP WITH TIME ZONE 으로 가정.
     */
    @Query("""
                select new com.app.codemasterpiecebackend.domain.dto.analytics.PvAggDto(
                    count(p),
                    count(distinct p.cid),
                    count(distinct p.sid)
                )
                from PageView p
                where p.occurredAt >= :from and p.occurredAt < :to
            """)
    PvAggDto aggregateRange(@Param("from") Instant from, @Param("to") Instant to);

    @Modifying
    @Query("""
                delete from PageView p
                where p.occurredAt < :cutoff
            """)
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
