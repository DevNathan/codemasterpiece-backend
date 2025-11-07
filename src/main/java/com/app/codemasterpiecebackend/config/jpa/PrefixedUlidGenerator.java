package com.app.codemasterpiecebackend.config.jpa;

import com.app.codemasterpiecebackend.util.ULIDs;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.AnnotationBasedGenerator;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;

import java.util.EnumSet;

public final class PrefixedUlidGenerator
        implements BeforeExecutionGenerator, AnnotationBasedGenerator<PrefixedUlidId> {

    private String prefix;
    private boolean monotonic;

    @Override
    public void initialize(PrefixedUlidId config,
                           java.lang.reflect.Member idMember,
                           org.hibernate.generator.GeneratorCreationContext context) {
        if (config == null) throw new IllegalArgumentException("@PrefixedUlidId 가 필요합니다.");
        String p = config.value();
        if (p == null || p.length() != 2) {
            throw new IllegalArgumentException("ULID 접두사는 반드시 두글자여야 합니다.");
        }
        this.prefix = p;
        this.monotonic = config.monotonic();
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        // 식별자 생성: INSERT에서만 실행
        return EventTypeSets.INSERT_ONLY;
    }

    @Override
    public Object generate(SharedSessionContractImplementor session,
                           Object owner,
                           Object currentValue,
                           EventType eventType) {
        return monotonic ? ULIDs.newMonotonicUlid(prefix) : ULIDs.newUlid(prefix);
    }
}
