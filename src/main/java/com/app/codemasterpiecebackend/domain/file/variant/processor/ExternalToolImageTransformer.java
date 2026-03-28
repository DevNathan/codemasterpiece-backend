package com.app.codemasterpiecebackend.domain.file.variant.processor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.nio.file.Files.createTempFile;

/**
 * 네이티브 이미지 툴(cwebp, avifenc)을 프로세스로 호출하여 이미지 변환을 수행하는 구현체입니다.
 * * <p>모든 입출력은 물리적 임시 파일을 통해 이루어지며, 작업 완료 시 반환된 결과 파일은
 * 호출자(Caller) 측에서 반드시 삭제(Clean-up)하여 디스크 용량을 확보해야 합니다.</p>
 */
@Component
public class ExternalToolImageTransformer implements ImageTransformer {

    @Value("${image.bin.cwebp:cwebp}")
    private String cwebp;

    @Value("${image.bin.avifenc:avifenc}")
    private String avifenc;

    @Value("${image.timeout.ms:20000}")
    private long timeoutMs;

    @Override
    public File toWebp(File original) {
        return runWithTemp(
                original, ".webp",
                List.of(cwebp, "-q", "85", "-m", "6", "-mt", "@IN@", "-o", "@OUT@")
        );
    }

    @Override
    public File toAvif(File original) {
        return runWithTemp(
                original, ".avif",
                List.of(avifenc, "--min", "25", "--max", "35", "--speed", "6", "@IN@", "@OUT@")
        );
    }

    @Override
    public ResizeResult resizeWebp(File original, int maxWidth) {
        File out = runWithTemp(
                original, ".webp",
                List.of(cwebp, "-q", "82", "-mt", "-resize", String.valueOf(maxWidth), "0", "@IN@", "-o", "@OUT@")
        );
        return new ResizeResult(out, maxWidth, 0);
    }

    /**
     * 임시 파일을 생성하여 외부 툴을 실행합니다.
     *
     * @param in     원본 입력 파일
     * @param outExt 출력 파일의 확장자
     * @param cmdTpl 실행할 커맨드 템플릿
     * @return 처리가 완료된 출력 파일 객체 (호출자가 삭제 책임을 가짐)
     * @throws IllegalStateException 외부 프로세스 오류 또는 타임아웃 발생 시
     */
    private File runWithTemp(File in, String outExt, List<String> cmdTpl) {
        File out = null;
        try {
            out = createTempFile("img-out-", outExt).toFile();
            var cmd = getStrings(cmdTpl, in, out);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();

            boolean done = p.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            if (!done) {
                p.destroyForcibly();
                throw new IllegalStateException("Image tool timeout: " + String.join(" ", cmd));
            }

            int code = p.exitValue();
            if (code != 0) {
                try (InputStream is = p.getInputStream()) {
                    String log = new String(is.readAllBytes());
                    throw new IllegalStateException("Image tool error(" + code + "): " + log);
                }
            }

            return out;

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            if (out != null && out.exists()) out.delete();
            throw new IllegalStateException("Image tool interrupted", ie);
        } catch (IOException e) {
            if (out != null && out.exists()) out.delete();
            throw new IllegalStateException("Image transform failed", e);
        }
    }

    private static ArrayList<String> getStrings(List<String> cmdTpl, File in, File out) {
        String inPath = in.getAbsolutePath();
        String outPath = out.getAbsolutePath();
        var cmd = new ArrayList<String>(cmdTpl.size());
        for (String t : cmdTpl) {
            if (t.indexOf('@') >= 0) {
                if (t.contains("@IN@")) t = t.replace("@IN@", inPath);
                if (t.contains("@OUT@")) t = t.replace("@OUT@", outPath);
            }
            cmd.add(t);
        }
        return cmd;
    }
}