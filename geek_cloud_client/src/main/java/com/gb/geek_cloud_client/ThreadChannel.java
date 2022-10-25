package com.gb.geek_cloud_client;

public class ThreadChannel {
    private static Thread instance;


    public static void startInstance(Runnable r) {
        if (instance==null) {
            instance = new Thread(r);
            instance.setName("client_channel");
            instance.setDaemon(true);
            instance.start();
        }

    }
}
