package com.app.codemasterpiecebackend.infra.messaging.variant;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class VariantExecutorConfig {

    @Bean(name = "variantExecutor")
    public ThreadPoolTaskExecutor variantExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        // 이미지 변환은 CPU 바운드. 코어 절반~동일 수준이 합리적.
        exec.setCorePoolSize(Math.max(2, cores / 2));
        exec.setMaxPoolSize(Math.max(2, cores));
        exec.setQueueCapacity(256);
        exec.setThreadNamePrefix("variant-");
        // 큐가 꽉 차면 호출 스레드가 직접 실행 -> 자연 백프레셔
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
