package com.app.codemasterpiecebackend.service.post;

import com.app.codemasterpiecebackend.domain.repository.PostRepository;
import com.app.codemasterpiecebackend.support.time.TtlCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Service
@RequiredArgsConstructor
@Transactional
public class PostViewRedisService implements PostViewService {
    private final JedisPool jedisPool;
    private final PostRepository postRepository;
    private final TtlCalculator ttl;

    /**
     * @return 오늘 해당 IP의 최초 조회로 카운트가 증가했으면 true
     */
    public boolean registerView(String postId, String ip) {
        String key = keyOf(postId, ip, ttl.todayStr());
        long expireSec = Math.max(60, ttl.secondsUntilNext3AM()); // 안전 하한선 60초

        try (Jedis jedis = jedisPool.getResource()) {
            var params = new redis.clients.jedis.params.SetParams()
                    .nx()
                    .ex(expireSec);

            // 처음 보면 "OK", 이미 있으면 null
            String resp = jedis.set(key, "1", params);
            if ("OK".equals(resp)) {
                postRepository.bumpViewCount(postId, 1);
                return true;
            }
            return false;
        }
    }

    private static String keyOf(String postId, String ip, String ymd) {
        return "view:post:%s:ip:%s:%s".formatted(postId, ip, ymd);
    }
}
