package com.ponderer.addon;

import net.minecraft.client.Minecraft;

public final class StorageContextMonitor {

    private static volatile boolean started;

    private StorageContextMonitor() {}

    public static void start() {
        if (started) return;
        started = true;

        Thread thread = new Thread(StorageContextMonitor::run, "ponderer-storage-context-monitor");
        thread.setDaemon(true);
        thread.start();
    }

    private static void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Minecraft.getInstance().execute(PondererStorageContext::reloadPondererIfContextChanged);
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception ignored) {
            }
        }
    }
}
