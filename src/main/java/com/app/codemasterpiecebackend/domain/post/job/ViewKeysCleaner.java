package com.app.codemasterpiecebackend.domain.post.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

/**
 * Redis에 적재된 조회수 캐시 키를 일괄 정리하는 스케줄러 컴포넌트입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewKeysCleaner {

    private final JedisPool jedisPool;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeViewKeys() {
        log.info("Starting Redis view keys purge process...");
        int totalDeleted = 0;

        try (Jedis jedis = jedisPool.getResource()) {
            String cursor = ScanParams.SCAN_POINTER_START;
            ScanParams params = new ScanParams().match("view:post:*").count(2000);

            do {
                ScanResult<String> sr = jedis.scan(cursor, params);
                var keys = sr.getResult();
                if (!keys.isEmpty()) {
                    // 대량 삭제는 파이프라인으로
                    var p = jedis.pipelined();
                    for (String k : keys) p.del(k);
                    p.sync();
                    totalDeleted += keys.size();
                }
                cursor = sr.getCursor();
            } while (!"0".equals(cursor));

            log.info("Successfully purged {} view keys from Redis.", totalDeleted);
        } catch (Exception e) {
            log.error("Failed to purge view keys from Redis", e);
        }
    }
}