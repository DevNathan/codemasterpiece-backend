package com.app.codemasterpiecebackend.domain.repository.analytics;

import com.app.codemasterpiecebackend.domain.entity.analytics.PvMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface PvMonthlyRepository extends JpaRepository<PvMonthly, LocalDate> {}