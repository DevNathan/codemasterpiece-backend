package com.app.codemasterpiecebackend.global.util;

import com.app.codemasterpiecebackend.domain.post.dto.PostResult;
import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.HtmlWriter;
import com.vladsch.flexmark.html.renderer.NodeRenderer;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.html.renderer.NodeRendererFactory;
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jspecify.annotations.NonNull;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 마크다운 파싱 및 렌더링을 담당하는 유틸리티 클래스입니다.
 * 수식 보호 처리, XSS 방어, 그리고 커스텀 Admonition(콜아웃) 렌더링을 지원합니다.
 */
public class MarkdownUtil {

    private static final Parser PARSER;
    private static final HtmlRenderer POST_RENDERER;
    private static final HtmlRenderer COMMENT_RENDERER;

    private static final Pattern STRONG_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern EM_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern MARK_PATTERN = Pattern.compile("==([^=]+)==");
    private static final Pattern INS_PATTERN = Pattern.compile("\\+\\+(.*?)\\+\\+");
    private static final Pattern SUP_PATTERN = Pattern.compile("\\^([^\\^]+)\\^");
    private static final Pattern SUB_PATTERN = Pattern.compile("~([^~]+)~");

    /**
     * 마크다운 내의 수식(LaTeX) 문자열이 Flexmark 파서에 의해 오염되지 않도록 전처리합니다.
     *
     * @param markdown 원본 마크다운 문자열
     * @return 수식이 보호된 마크다운 문자열
     */
    private static String preprocessMath(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";

        // 블록 수식 보호: 파서가 코드 블록으로 인식하도록 개행 문자를 삽입합니다.
        String result = markdown.replaceAll("(?s)\\$\\$(.*?)\\$\\$", "\n\n```math-display\n$1\n```\n\n");
        // 인라인 수식 보호: 백틱을 사용하여 일반 인라인 코드로 변환합니다.
        result = result.replaceAll("(?s)(?<!\\$)\\$(?!\\$)(.+?)(?<!\\$)\\$(?!\\$)", "`math-inline $1`");

        return result;
    }

    static class PostMarkdownExtension implements HtmlRenderer.HtmlRendererExtension {
        public static PostMarkdownExtension create() { return new PostMarkdownExtension(); }
        @Override public void rendererOptions(@NonNull MutableDataHolder options) {}
        @Override public void extend(HtmlRenderer.Builder htmlRendererBuilder, @NonNull String rendererType) {
            htmlRendererBuilder.nodeRendererFactory(new PostNodeRendererFactory());
        }
    }

    static class PostNodeRendererFactory implements NodeRendererFactory {
        @Override public @NonNull NodeRenderer apply(@NonNull DataHolder options) { return new PostNodeRenderer(); }
    }

    static class PostNodeRenderer implements NodeRenderer {
        private final Map<String, Integer> slugCounts = new HashMap<>();

        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            Set<NodeRenderingHandler<?>> handlers = new HashSet<>();
            handlers.add(new NodeRenderingHandler<>(Text.class, this::renderText));
            handlers.add(new NodeRenderingHandler<>(Code.class, this::renderInlineCode));
            handlers.add(new NodeRenderingHandler<>(FencedCodeBlock.class, this::renderFencedCodeBlock));
            handlers.add(new NodeRenderingHandler<>(Heading.class, this::renderHeading));
            handlers.add(new NodeRenderingHandler<>(BlockQuote.class, this::renderBlockQuote));
            return handlers;
        }

        /**
         * 인용구(BlockQuote) 노드를 렌더링합니다.
         * 특정 패턴([!tip] 등)이 감지될 경우 커스텀 Admonition UI로 변환합니다.
         */
        private void renderBlockQuote(BlockQuote node, NodeRendererContext context, HtmlWriter html) {
            String kindRaw = null;
            String kind = null;
            int charsToRemove = 0;
            Paragraph firstParagraph = null;

            Node firstChild = node.getFirstChild();

            // 첫 번째 문단에서 Admonition 패턴을 확인합니다.
            if (firstChild instanceof Paragraph) {
                firstParagraph = (Paragraph) firstChild;
                String pText = firstParagraph.getChars().toString();
                Matcher m = Pattern.compile("^\\s*\\[!([a-zA-Z]+)\\]\\s*").matcher(pText);

                if (m.find()) {
                    kindRaw = m.group(1);
                    kind = kindRaw.toLowerCase();
                    charsToRemove = m.end();
                }
            }

            // 패턴이 일치할 경우 커스텀 UI 렌더링을 수행합니다.
            if (kind != null) {
                String labelText = kindRaw.substring(0, 1).toUpperCase() + kindRaw.substring(1).toLowerCase();

                final String finalContainerClass = "cm-admonition cm-admonition-" + kind;
                final String finalTitleClass = "cm-admonition-title";
                final String finalLabelText = labelText;

                List<Node> unlinkedNodes = new ArrayList<>();
                Map<Text, BasedSequence> originalCharsMap = new HashMap<>();

                Node child = firstParagraph.getFirstChild();
                int remaining = charsToRemove;

                // 렌더링에서 지시어 텍스트를 제외하기 위해 AST 노드를 일시적으로 수정합니다.
                while (child != null && remaining > 0) {
                    int len = child.getChars().length();
                    Node next = child.getNext();

                    if (remaining >= len) {
                        child.unlink();
                        unlinkedNodes.add(child);
                        remaining -= len;
                    } else {
                        if (child instanceof Text text) {
                            originalCharsMap.put(text, text.getChars());
                            text.setChars(text.getChars().subSequence(remaining, len));
                        }
                        remaining = 0;
                    }
                    child = next;
                }

                html.line();
                html.attr("class", finalContainerClass).withAttr().tag("div", false, false, () -> {
                    html.attr("class", finalTitleClass).withAttr().tag("div", false, false, () -> {
                        html.tag("span");
                        html.text(finalLabelText);
                        html.closeTag("span");
                    });
                    html.attr("class", "cm-admonition-content").withAttr().tag("div", false, false, () -> {
                        context.renderChildren(node);
                    });
                });
                html.line();

                // 렌더링 완료 후 AST 트리의 무결성을 위해 노드를 원상 복구합니다.
                for (Map.Entry<Text, BasedSequence> entry : originalCharsMap.entrySet()) {
                    entry.getKey().setChars(entry.getValue());
                }
                for (int i = unlinkedNodes.size() - 1; i >= 0; i--) {
                    firstParagraph.prependChild(unlinkedNodes.get(i));
                }

            } else {
                // 패턴이 일치하지 않을 경우 일반 인용구로 렌더링합니다.
                html.line();
                html.srcPos(node.getChars()).withAttr().tag("blockquote", false, false, () -> context.renderChildren(node));
                html.line();
            }
        }

        private void renderInlineCode(Code node, NodeRendererContext context, HtmlWriter html) {
            String content = node.getText().toString();
            if (content.startsWith("math-inline ")) {
                html.attr("class", "math-inline").withAttr().tag("span");
                html.text("$" + content.substring(12) + "$");
                html.closeTag("span");
            } else {
                html.srcPos(node.getChars()).withAttr().tag("code");
                html.text(content);
                html.closeTag("code");
            }
        }

        private void renderFencedCodeBlock(FencedCodeBlock node, NodeRendererContext context, HtmlWriter html) {
            String info = node.getInfo().toString();
            String lang = info != null && !info.isBlank() ? info.split(" ")[0] : "text";

            if ("math".equals(lang) || "math-display".equals(lang)) {
                html.line();
                html.attr("class", "math-display").withAttr().tag("div");
                html.text("$$" + node.getContentChars().normalizeEOL() + "$$");
                html.closeTag("div");
                html.line();
                return;
            }

            String copySvg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"14\" height=\"14\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\" stroke-linecap=\"round\" stroke-linejoin=\"round\" class=\"size-4\"><rect width=\"14\" height=\"14\" x=\"8\" y=\"8\" rx=\"2\" ry=\"2\"></rect><path d=\"M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2\"></path></svg>";

            html.line();
            html.attr("class", "cm-codeblock-wrapper").withAttr().tag("div", false, false, () -> {
                html.attr("class", "cm-codeblock-toolbar").withAttr().tag("div", false, false, () -> {
                    html.attr("class", "cm-codeblock-lang").withAttr().tag("span");
                    html.text(lang);
                    html.closeTag("span");

                    html.attr("type", "button")
                            .attr("class", "cm-codeblock-copy-btn")
                            .attr("aria-label", "Copy code")
                            .attr("title", "Copy code")
                            .withAttr().tag("button", false, false, () -> html.raw(copySvg));
                });
                html.line();
                html.srcPos(node.getChars()).withAttr().tag("pre").openPre();
                html.attr("class", "language-" + lang).withAttr().tag("code");
                html.text(node.getContentChars().normalizeEOL());
                html.closeTag("code");
                html.closeTag("pre").closePre();
            });
            html.line();
        }

        private void renderText(Text node, NodeRendererContext context, HtmlWriter html) {
            String text = node.getChars().toString();
            text = HtmlUtils.htmlEscape(text);
            if (text.contains("**")) text = STRONG_PATTERN.matcher(text).replaceAll("<strong>$1</strong>");
            if (text.contains("*")) text = EM_PATTERN.matcher(text).replaceAll("<em>$1</em>");
            if (text.contains("==")) text = MARK_PATTERN.matcher(text).replaceAll("<mark>$1</mark>");
            if (text.contains("++")) text = INS_PATTERN.matcher(text).replaceAll("<u>$1</u>");
            if (text.contains("^")) text = SUP_PATTERN.matcher(text).replaceAll("<sup>$1</sup>");
            if (text.contains("~")) text = SUB_PATTERN.matcher(text).replaceAll("<sub>$1</sub>");
            html.raw(text);
        }

        private void renderHeading(Heading node, NodeRendererContext context, HtmlWriter html) {
            String rawText = node.getText().toString();
            String baseSlug = Stringx.slugify(rawText.toLowerCase());
            int count = slugCounts.getOrDefault(baseSlug, 0);
            slugCounts.put(baseSlug, count + 1);
            String id = (count == 0) ? baseSlug : baseSlug + "-" + count;

            html.line();
            html.srcPos(node.getChars())
                    .attr("id", id)
                    .withAttr()
                    .tag("h" + node.getLevel(), false, false, () -> context.renderChildren(node));
            html.line();
        }
    }

    /**
     * 댓글 렌더링 시 사용되는 보안 필터 확장입니다.
     * 위험할 수 있는 HTML 요소를 제거하거나 텍스트로 치환하여 XSS 공격을 방어합니다.
     */
    static class CommentFilterExtension implements HtmlRenderer.HtmlRendererExtension {
        public static CommentFilterExtension create() { return new CommentFilterExtension(); }
        @Override public void rendererOptions(@NonNull MutableDataHolder options) {}
        @Override public void extend(HtmlRenderer.Builder htmlRendererBuilder, @NonNull String rendererType) {
            htmlRendererBuilder.nodeRendererFactory(new CommentFilterNodeRendererFactory());
        }
    }

    static class CommentFilterNodeRendererFactory implements NodeRendererFactory {
        @Override public @NonNull NodeRenderer apply(@NonNull DataHolder options) { return new CommentFilterNodeRenderer(); }
    }

    static class CommentFilterNodeRenderer implements NodeRenderer {
        @Override
        public Set<NodeRenderingHandler<?>> getNodeRenderingHandlers() {
            Set<NodeRenderingHandler<?>> handlers = new HashSet<>();
            handlers.add(new NodeRenderingHandler<>(Heading.class, (node, ctx, html) -> {}));
            handlers.add(new NodeRenderingHandler<>(Image.class, (node, ctx, html) -> {}));
            handlers.add(new NodeRenderingHandler<>(TableBlock.class, (node, ctx, html) -> {}));

            // 인라인 및 블록 HTML 요소는 실행되지 않도록 문자열로 치환하여 출력합니다.
            handlers.add(new NodeRenderingHandler<>(HtmlInline.class, (node, ctx, html) -> {
                html.text(node.getChars().toString());
            }));
            handlers.add(new NodeRenderingHandler<>(HtmlBlock.class, (node, ctx, html) -> {
                html.text(node.getChars().toString());
            }));

            handlers.add(new NodeRenderingHandler<>(Link.class, (node, ctx, html) -> ctx.renderChildren(node)));
            return handlers;
        }
    }

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()
        ));
        PARSER = Parser.builder(options).build();
        POST_RENDERER = HtmlRenderer.builder(options).extensions(List.of(PostMarkdownExtension.create())).build();
        COMMENT_RENDERER = HtmlRenderer.builder(options).extensions(Arrays.asList(PostMarkdownExtension.create(), CommentFilterExtension.create())).build();
    }

    public static String parsePostToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        String safeMarkdown = preprocessMath(markdown).replaceAll("<br\\s*/?>", "  \n");
        return POST_RENDERER.render(PARSER.parse(safeMarkdown));
    }

    public static String parseCommentToHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) return "";
        String safeMarkdown = preprocessMath(markdown).replaceAll("<br\\s*/?>", "  \n");
        return COMMENT_RENDERER.render(PARSER.parse(safeMarkdown));
    }

    public static List<PostResult.Toc> extractToc(String markdown) {
        if (markdown == null || markdown.isBlank()) return new ArrayList<>();
        String safeMarkdown = preprocessMath(markdown).replaceAll("<br\\s*/?>", "  \n");
        Node document = PARSER.parse(safeMarkdown);
        List<PostResult.Toc> tocList = new ArrayList<>();
        Map<String, Integer> slugCounts = new HashMap<>();
        extractTocRecursively(document, tocList, slugCounts);
        return tocList;
    }

    private static void extractTocRecursively(Node node, List<PostResult.Toc> tocList, Map<String, Integer> slugCounts) {
        if (node instanceof Heading heading) {
            String rawText = heading.getText().toString();
            String baseSlug = Stringx.slugify(rawText.toLowerCase());
            int count = slugCounts.getOrDefault(baseSlug, 0);
            slugCounts.put(baseSlug, count + 1);
            String id = (count == 0) ? baseSlug : baseSlug + "-" + count;
            tocList.add(new PostResult.Toc(id, rawText, heading.getLevel()));
        }
        for (Node child : node.getChildren()) extractTocRecursively(child, tocList, slugCounts);
    }
}