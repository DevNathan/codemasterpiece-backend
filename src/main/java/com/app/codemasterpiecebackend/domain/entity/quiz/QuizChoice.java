//package com.app.codemasterpiecebackend.domain.entity.quiz;
//
//import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
//import jakarta.persistence.*;
//import lombok.*;
//
//@Entity
//@Table(
//    name = "tbl_quiz_choice",
//    indexes = {
//        @Index(name = "idx_choice_quiz_order", columnList = "quiz_id, sort_order")
//    },
//    uniqueConstraints = {
//        @UniqueConstraint(name = "uq_choice_quiz_order", columnNames = {"quiz_id","sort_order"})
//    }
//)
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//@Builder
//public class QuizChoice {
//
//    @Id
//    @PrefixedUlidId("CH")
//    @Column(name = "choice_id", length = 29, nullable = false, updatable = false)
//    private String id;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "quiz_id", foreignKey = @ForeignKey(name = "fk_choice_quiz"))
//    private QuizItem quiz;
//
//    @Column(name = "sort_order", nullable = false)
//    private int sortOrder;
//
//    /** 보기 텍스트 */
//    @Column(name = "value", nullable = false, length = 500)
//    private String value;
//
//    /** 선택형 정답 표시 */
//    @Builder.Default
//    @Column(name = "is_correct", nullable = false)
//    private boolean isCorrect = false;
//
//    /** 선택 통계(선택) */
//    @Builder.Default
//    @Column(name = "select_count", nullable = false)
//    private Long selectCount = 0L;
//}
