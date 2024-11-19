package com.github.getcurrentthread.soopapi.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public class SchedulerManager {
    private static final ScheduledExecutorService SHARED_SCHEDULER = Executors.newScheduledThreadPool(8);
    private static final AtomicInteger clientCount = new AtomicInteger(0);

    public static ScheduledExecutorService getScheduler() {
        clientCount.incrementAndGet();
        return SHARED_SCHEDULER;
    }

    public static void releaseScheduler() {
        if (clientCount.decrementAndGet() == 0) {
            SHARED_SCHEDULER.shutdownNow();
        }
    }

    public static void shutdownScheduler() {
        SHARED_SCHEDULER.shutdownNow();
    }

}
