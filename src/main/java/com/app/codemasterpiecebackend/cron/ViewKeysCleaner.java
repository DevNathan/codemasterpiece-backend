package com.app.codemasterpiecebackend.cron;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

@Component
@RequiredArgsConstructor
public class ViewKeysCleaner {

    private final JedisPool jedisPool;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void purgeViewKeys() {
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
                }
                cursor = sr.getCursor();
            } while (!"0".equals(cursor));
        } catch (Exception ignore) {
        }
    }
}
