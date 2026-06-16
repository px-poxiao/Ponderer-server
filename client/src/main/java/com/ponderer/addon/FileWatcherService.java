package com.ponderer.addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class FileWatcherService {

    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "ponderer-file-watcher");
                t.setDaemon(true);
                return t;
            });

    private FileWatcherService() {}

    public static void start() {
        if (!PondererAddonConfig.isFileWatcherEnabled()) return;
        Thread thread = new Thread(FileWatcherService::watch, "ponderer-file-watcher-main");
        thread.setDaemon(true);
        thread.start();
    }

    private static void watch() {
        Path sceneDir = getSceneDir();
        if (sceneDir == null) return;

        try {
            Files.createDirectories(sceneDir);
        } catch (Exception ignored) {
            return;
        }

        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            sceneDir.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            AtomicReference<ScheduledFuture<?>> debounce = new AtomicReference<>();

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                boolean hasChange = key.pollEvents().stream()
                        .anyMatch(e -> e.kind() != StandardWatchEventKinds.OVERFLOW);

                key.reset();

                if (hasChange) {
                    ScheduledFuture<?> prev = debounce.getAndSet(null);
                    if (prev != null) prev.cancel(false);
                    ScheduledFuture<?> next = SCHEDULER.schedule(
                            () -> Minecraft.getInstance().execute(FileWatcherService::showToast),
                            2, TimeUnit.SECONDS);
                    debounce.set(next);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void showToast() {
        if (!PondererAddonConfig.shouldShowFileWatcherToast()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        SystemToast.addOrUpdate(mc.getToasts(),
                SystemToast.SystemToastId.PERIODIC_NOTIFICATION,
                Component.literal("Ponderer"),
                Component.literal("Scene files changed. Use /ponderer push to upload."));
    }

    private static Path getSceneDir() {
        try {
            Class<?> storeClass = Class.forName("com.nododiiiii.ponderer.ponder.SceneStore");
            return (Path) storeClass.getMethod("getSceneDir").invoke(null);
        } catch (Exception e) {
            return Path.of("config", "ponderer", "scripts");
        }
    }
}
