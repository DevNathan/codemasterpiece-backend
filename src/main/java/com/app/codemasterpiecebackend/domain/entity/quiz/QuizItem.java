//package com.app.codemasterpiecebackend.domain.entity.quiz;
//
//import com.app.codemasterpiecebackend.config.jpa.PrefixedUlidId;
//import com.app.codemasterpiecebackend.domain.entity.post.Post;
//import jakarta.persistence.*;
//import lombok.*;
//
//import java.util.ArrayList;
//import java.util.List;
//
//@Entity
//@Table(
//        name = "tbl_quiz_item",
//        indexes = {
//                @Index(name = "idx_quiz_post_order", columnList = "post_id, sort_order"),
//                @Index(name = "idx_quiz_post_type",  columnList = "post_id, type")
//        }
//)
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//@AllArgsConstructor(access = AccessLevel.PRIVATE)
//@Builder
//public class QuizItem {
//
//    @Id
//    @PrefixedUlidId("QI")
//    @Column(name = "quiz_id", length = 29, nullable = false, updatable = false)
//    private String id;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(
//            name = "post_id",
//            referencedColumnName = "post_id",
//            foreignKey = @ForeignKey(name = "fk_quiz_post")
//    )
//    private Post post;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "type", length = 30, nullable = false)
//    private QuizType type; // SINGLE_CHOICE, MULTI_CHOICE, TRUE_FALSE, SHORT_TEXT ...
//
//    @Column(name = "sort_order", nullable = false)
//    private int sortOrder;
//
//    @Column(name = "question", nullable = false, length = 1000)
//    private String question;
//
//    @Column(name = "hint", length = 1000)
//    private String hint;
//
//    @Column(name = "explanation", length = 2000)
//    private String explanation;
//
//    /**
//     * 타입별 정답 스펙(JSONB) — 선택형이면 보통 null (Choice.isCorrect로 처리)
//     *   TRUE_FALSE: { "answer": true }
//     *   SHORT_TEXT: { "answers": ["ulid","ULID"], "caseInsensitive": true, "trim": true }
//     */
//    @Column(name = "answer_spec", columnDefinition = "jsonb")
//    private String answerSpecJson;
//
//    /** 선택형일 경우만 사용 */
//    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
//    @OrderBy("sortOrder ASC")
//    @Builder.Default
//    private List<QuizChoice> choices = new ArrayList<>();
//}
