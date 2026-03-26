package com.app.codemasterpiecebackend.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MarkdownUtil 클래스의 파싱 및 렌더링 기능을 검증하는 테스트 클래스입니다.
 */
class MarkdownUtilTest {

    @Test
    @DisplayName("게시글 파싱 테스트 - 수식 전처리 및 Admonition(콜아웃) 변환이 정상적으로 수행되어야 합니다.")
    void parsePostToHtml_MathAndAdmonition() {
        // given
        String markdown = """
                > [!tip]
                > 이것은 팁입니다.
                
                수식 블록 테스트:
                $$
                E = mc^2
                $$
                
                인라인 수식 $x = 1$ 입니다.
                """;

        // when
        String html = MarkdownUtil.parsePostToHtml(markdown);

        // then
        // 1. Admonition 컴포넌트 변환 검증
        assertThat(html).contains("cm-admonition cm-admonition-tip");
        assertThat(html).contains("이것은 팁입니다.");
        assertThat(html).doesNotContain("[!tip]");

        // 2. 수식 블록 보호 및 렌더링 검증
        assertThat(html).contains("math-display");
        assertThat(html).contains("$$", "E = mc^2");

        // 3. 인라인 수식 보호 및 렌더링 검증
        assertThat(html).contains("math-inline");
        assertThat(html).contains("$x = 1$");
    }

    @Test
    @DisplayName("댓글 파싱 테스트 - 보안을 위해 이미지, 헤딩 등의 태그가 필터링되고 XSS가 방어되어야 합니다.")
    void parseCommentToHtml_SecurityFilter() {
        // given
        String markdown = """
                # 이 헤딩은 필터링되어야 합니다.
                ![이미지 무시](http://example.com/image.png)
                <script>alert('XSS');</script>
                **강조 텍스트**는 정상적으로 렌더링됩니다.
                """;

        // when
        String html = MarkdownUtil.parseCommentToHtml(markdown);

        // then
        // 1. 금지된 마크다운 요소(Heading, Image) 제거 검증
        assertThat(html).doesNotContain("<h1");
        assertThat(html).doesNotContain("이 헤딩은 필터링되어야 합니다");
        assertThat(html).doesNotContain("<img");

        // 2. XSS 스크립트 이스케이프 처리 검증
        assertThat(html).contains("&lt;script&gt;alert('XSS');&lt;/script&gt;");

        // 3. 허용된 마크다운 요소 유지 검증
        assertThat(html).contains("<strong>강조 텍스트</strong>");
    }
}