package com.example.secaicontainerengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfiguration {

    @Bean
    public ThreadPoolExecutor taskExecutor() {
        // 创建线程工厂，用于自定义线程名称
        ThreadFactory customThreadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            private final String namePrefix = "自定义线程-";

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
                if (t.isDaemon()) {
                    t.setDaemon(false); // 设置为非守护线程
                }
                if (t.getPriority() != Thread.NORM_PRIORITY) {
                    t.setPriority(Thread.NORM_PRIORITY); // 设置默认优先级
                }
                return t;
            }
        };

        return new ThreadPoolExecutor(
                30,
                30,
                0L,
                TimeUnit.MICROSECONDS,
                new LinkedBlockingQueue<Runnable>(),
                customThreadFactory
        );
    }
}
