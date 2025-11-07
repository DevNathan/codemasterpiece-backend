// 롤업 테이블들
package com.app.codemasterpiecebackend.domain.repository.analytics;

import com.app.codemasterpiecebackend.domain.entity.analytics.PvDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PvDailyRepository extends JpaRepository<PvDaily, LocalDate> {
}
