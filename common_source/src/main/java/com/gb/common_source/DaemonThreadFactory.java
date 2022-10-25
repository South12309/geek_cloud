package com.gb.common_source;

import java.util.concurrent.ThreadFactory;

public class DaemonThreadFactory implements ThreadFactory {

    private static DaemonThreadFactory instance;

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    }

    public Thread getThread(Runnable r, String name) {
        Thread thread = new Thread(r);
        thread.setName(name);
        thread.setDaemon(true);
        return thread;
    }

    public static DaemonThreadFactory getInstance() {
        if (instance==null) {
            instance = new DaemonThreadFactory();
        }
        return instance;
    }
}