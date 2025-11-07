package com.app.codemasterpiecebackend.config.jpa;

import org.hibernate.annotations.IdGeneratorType;

import java.lang.annotation.*;

@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@IdGeneratorType(PrefixedUlidGenerator.class)
public @interface PrefixedUlidId {
    /** 접두사 2글자 (예: "AD") */
    String value();
    /** 단조 증가 ULID 사용할지 (기본 true) */
    boolean monotonic() default true;
}
