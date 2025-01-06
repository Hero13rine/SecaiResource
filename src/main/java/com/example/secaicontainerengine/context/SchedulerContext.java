package com.example.secaicontainerengine.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class SchedulerContext {
    // 定时任务的线程池
    private final ScheduledExecutorService scheduler;
    //保存每个Pod对应的定时任务的句柄，方便停止任务
    private final Map<String, ScheduledFuture<?>> taskMap;

    public SchedulerContext(int taskSize) {
        this.scheduler = Executors.newScheduledThreadPool(taskSize);
        this.taskMap = new ConcurrentHashMap<>();
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    public Map<String, ScheduledFuture<?>> getTaskMap() {
        return taskMap;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
