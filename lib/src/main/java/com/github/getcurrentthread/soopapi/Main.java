package com.github.getcurrentthread.soopapi;

import com.github.getcurrentthread.soopapi.util.SchedulerManager;

public class Main {

    public static void main(String[] args) {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            SchedulerManager.shutdownScheduler();
        }));
    }
}
