package com.app.codemasterpiecebackend.domain.entity.analytics;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 일간 롤업 (UTC 기준)
 * keyDate = 해당 "UTC 날짜(YYYY-MM-DD)"
 */
@Entity
@Table(name = "tbl_pv_daily")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PvDaily extends BaseRollup {

    private PvDaily(LocalDate dayUtc, long views, long uv, long sessions) {
        this.keyDate = dayUtc;
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }

    public static PvDaily of(LocalDate dayUtc, long views, long uv, long sessions) {
        return new PvDaily(dayUtc, views, uv, sessions);
    }

    /** 전체 값 갱신 (업서트 용) */
    public void setRollup(long views, long uv, long sessions) {
        this.views = views;
        this.uv = uv;
        this.sessions = sessions;
    }
}
