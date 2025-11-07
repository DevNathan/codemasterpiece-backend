package com.app.codemasterpiecebackend.domain.entity.analytics;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 월간 롤업: keyDate = 월 시작일(1일) YYYY-MM-DD
 */
@Entity
@Table(name = "tbl_pv_monthly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PvMonthly extends BaseRollup {

    private PvMonthly(LocalDate monthStart, long views, long uv, long sessions) {
        this.keyDate = monthStart;
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }

    public static PvMonthly of(LocalDate monthStart, long views, long uv, long sessions) {
        return new PvMonthly(monthStart, views, uv, sessions);
    }

    public void setRollup(long views, long uv, long sessions) {
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }
}
