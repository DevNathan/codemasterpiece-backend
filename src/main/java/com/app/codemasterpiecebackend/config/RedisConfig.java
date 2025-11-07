package com.app.codemasterpiecebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool(
            @Value("${redis.host}") String host,
            @Value("${redis.port}") int port,
            @Value("${redis.password:}") String password,
            @Value("${redis.ssl:false}") boolean ssl,
            @Value("${redis.timeoutMillis:2000}") int timeoutMillis,
            @Value("${redis.maxTotal:32}") int maxTotal,
            @Value("${redis.maxIdle:16}") int maxIdle,
            @Value("${redis.minIdle:0}") int minIdle
    ) {
        var cfg = new redis.clients.jedis.JedisPoolConfig();
        cfg.setMaxTotal(maxTotal);
        cfg.setMaxIdle(maxIdle);
        cfg.setMinIdle(minIdle);

        cfg.setJmxEnabled(false);

        if (password == null || password.isBlank()) {
            return new JedisPool(cfg, host, port, timeoutMillis, ssl);
        }
        return new JedisPool(cfg, host, port, timeoutMillis, password, ssl);
    }
}

