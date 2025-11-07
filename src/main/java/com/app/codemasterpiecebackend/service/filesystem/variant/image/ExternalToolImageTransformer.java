package com.app.codemasterpiecebackend.service.filesystem.variant.image;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.readAllBytes;

/**
 * ExternalToolImageTransformer
 *
 * <p>네이티브 이미지 툴을 프로세스로 호출해 WebP/AVIF 인코딩과 리사이즈를 수행한다.
 * 의존 바이너리:
 * <ul>
 *   <li><b>cwebp</b> — WebP 인코더</li>
 *   <li><b>avifenc</b> — AVIF 인코더</li>
 *   <li><b>magick/convert</b> — (옵션) SVG 래스터라이즈 등 전처리</li>
 * </ul>
 *
 * <h3>설계 포인트</h3>
 * <ul>
 *   <li><b>보안</b>: 쉘을 거치지 않고 {@link ProcessBuilder} 인자 배열로 실행(커맨드 인젝션 차단).</li>
 *   <li><b>안정성</b>: 입력/출력은 임시 파일을 사용. 대형 이미지에서 메모리 압박 완화.</li>
 *   <li><b>타임아웃</b>: {@code timeoutMs} 내 미완료 시 즉시 강제 종료.</li>
 *   <li><b>로그</b>: 비정상 종료 시 표준출력(에러 병합) 내용을 예외 메시지에 담아 디버깅 용이.</li>
 *   <li><b>리소스</b>: try-with-resources와 finally 블록에서 임시 파일 정리.</li>
 * </ul>
 *
 * <h3>환경설정(application.yml)</h3>
 * <pre>
 * image:
 *   bin:
 *     cwebp: cwebp
 *     avifenc: avifenc
 *     magick: convert
 *   timeout:
 *     ms: 20000
 * </pre>
 */
@Component
public class ExternalToolImageTransformer implements ImageTransformer {

    /**
     * cwebp 바이너리 경로(또는 PATH 상 이름).
     */
    @Value("${image.bin.cwebp:cwebp}")
    private String cwebp;

    /**
     * avifenc 바이너리 경로(또는 PATH 상 이름).
     */
    @Value("${image.bin.avifenc:avifenc}")
    private String avifenc;

    /**
     * ImageMagick(옵션) — SVG 등 특수 전처리에 사용 가능.
     */
    @Value("${image.bin.magick:magick}")
    private String magick;

    /**
     * 외부 툴 실행 타임아웃(ms).
     */
    @Value("${image.timeout.ms:20000}")
    private long timeoutMs;

    /**
     * 입력 바이트를 WebP(손실)로 인코딩한다.
     *
     * @param original 원본 이미지 바이트
     * @return webp 바이트
     * @throws IllegalStateException 외부 툴 실패/타임아웃/IO 예외 시
     */
    @Override
    public byte[] toWebp(byte[] original) {
        // 품질/속도 튜닝: -q 85, -m 6, -mt (멀티스레드)
        return runWithTemp(
                original, ".in", ".webp",
                List.of(cwebp, "-q", "85", "-m", "6", "-mt", "@IN@", "-o", "@OUT@")
        );
    }

    /**
     * 입력 바이트를 AVIF(손실)로 인코딩한다.
     *
     * @param original 원본 이미지 바이트
     * @return avif 바이트
     * @throws IllegalStateException 외부 툴 실패/타임아웃/IO 예외 시
     */
    @Override
    public byte[] toAvif(byte[] original) {
        // libavif 기본 파라미터: 품질 범위/속도(타협안)
        return runWithTemp(
                original, ".in", ".avif",
                List.of(avifenc, "--min", "25", "--max", "35", "--speed", "6", "@IN@", "@OUT@")
        );
    }

    /**
     * WebP로 리사이즈(가로 기준) + 인코딩을 한 번에 수행한다.
     * <p>세로는 비율 유지 위해 자동(0)로 지정.</p>
     *
     * @param original 원본 이미지 바이트
     * @param maxWidth 목표 가로 픽셀 (세로는 비율 유지)
     * @return 리사이즈된 webp 바이트와 가로/세로(세로는 0으로 반환; 실제 치수가 필요하면 별도 측정)
     * @throws IllegalStateException 외부 툴 실패/타임아웃/IO 예외 시
     */
    @Override
    public ResizeResult resizeWebp(byte[] original, int maxWidth) {
        byte[] out = runWithTemp(
                original, ".in", ".webp",
                List.of(cwebp, "-q", "82", "-mt", "-resize", String.valueOf(maxWidth), "0", "@IN@", "-o", "@OUT@")
        );
        // 필요 시 identify로 실제 높이 추출 가능. 여기선 0으로 표기.
        return new ResizeResult(out, maxWidth, 0);
    }

    /**
     * 임시 파일을 사용해 외부 툴을 실행하고 결과 파일을 읽어 반환한다.
     *
     * <p>동작 순서:</p>
     * <ol>
     *   <li>입력 바이트를 임시 입력 파일에 기록</li>
     *   <li>커맨드 템플릿(cmdTpl)의 {@code @IN@}/{@code @OUT@} 토큰을 실제 경로로 치환</li>
     *   <li>{@link ProcessBuilder}로 실행(쉘 미사용/인자 배열)</li>
     *   <li>타임아웃 초과 시 프로세스 강제 종료</li>
     *   <li>비정상 종료 시 표준 출력 로그를 포함해 예외 발생</li>
     *   <li>정상 종료 시 출력 파일을 읽어 바이트 반환</li>
     * </ol>
     *
     * @param input  입력 바이트
     * @param inExt  임시 입력 확장자(로더가 포맷을 유추할 수 있게 적절히 선택)
     * @param outExt 임시 출력 확장자
     * @param cmdTpl 외부 툴 커맨드 템플릿(토큰: {@code @IN@}, {@code @OUT@})
     * @return 출력 파일 바이트
     * @throws IllegalStateException 외부 툴 실패/타임아웃/IO 예외 시
     */
    private byte[] runWithTemp(byte[] input, String inExt, String outExt, List<String> cmdTpl) {
        File in = null, out = null;
        try {
            // 1) 임시 입출력 파일 생성
            in = createTempFile("img-", inExt).toFile();
            out = createTempFile("img-", outExt).toFile();

            // 2) 입력 기록
            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(in))) {
                os.write(input);
            }

            // 3) 커맨드 구성(쉘 미사용, 인자 배열로 안전하게 실행)
            var cmd = getStrings(cmdTpl, in, out);

            // 4) 실행 + 타임아웃
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true); // stderr → stdout 병합
            Process p = pb.start();

            boolean done = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                p.destroyForcibly();
                throw new IllegalStateException("Image tool timeout: " + String.join(" ", cmd));
            }

            // 5) 종료 코드 확인 + 에러 로그 수집
            int code = p.exitValue();
            if (code != 0) {
                try (InputStream is = p.getInputStream()) {
                    String log = new String(is.readAllBytes());
                    throw new IllegalStateException("Image tool error(" + code + "): " + log);
                }
            }

            // 6) 성공: 출력 파일 로드
            return readAllBytes(out.toPath());

        } catch (InterruptedException ie) {
            // 인터럽트 플래그 복원 후 래핑
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Image tool interrupted", ie);
        } catch (IOException e) {
            throw new IllegalStateException("Image transform failed", e);
        } finally {
            // 7) 임시 파일 정리(실패/성공 모두)
            if (in != null && in.exists()) in.delete();
            if (out != null && out.exists()) out.delete();
        }
    }

    private static ArrayList<String> getStrings(List<String> cmdTpl, File in, File out) {
        String inPath = in.getAbsolutePath();
        String outPath = out.getAbsolutePath();
        var cmd = new ArrayList<String>(cmdTpl.size());
        for (int i = 0; i < cmdTpl.size(); i++) {
            String t = cmdTpl.get(i);
            if (t.indexOf('@') >= 0) {
                if (t.contains("@IN@")) t = t.replace("@IN@", inPath);
                if (t.contains("@OUT@")) t = t.replace("@OUT@", outPath);
            }
            cmd.add(t);
        }
        return cmd;
    }
}
