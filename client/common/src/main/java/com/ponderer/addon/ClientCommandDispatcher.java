package com.ponderer.addon;

import com.nododiiiii.ponderer.ponder.PondererClientCommands;
import com.nododiiiii.ponderer.ponder.SceneRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Method;
import java.util.Arrays;

public final class ClientCommandDispatcher {

    private ClientCommandDispatcher() {}

    public static void dispatch(String command) {
        if (command == null || command.isBlank()) return;
        String[] parts = command.trim().split("\\s+", -1);
        if (parts.length == 0 || parts[0].isBlank()) return;

        String name = parts[0].toLowerCase();
        if (!PondererAddonConfig.isClientCommandAllowed(name)) {
            notifyClient("Ponderer command is disabled locally: " + name);
            return;
        }

        try {
            switch (name) {
                case "pull" -> {
                    String mode = parts.length > 1 ? parts[1] : "check";
                    PondererClientCommands.pull(mode);
                }
                case "push" -> {
                    boolean force = parts.length > 1 && parts[1].equalsIgnoreCase("force");
                    String mode = force ? "force" : "check";
                    int idIdx = force ? 2 : 1;
                    if (parts.length > idIdx) {
                        PondererClientCommands.push(ResourceLocation.parse(parts[idIdx]), mode);
                    } else {
                        PondererClientCommands.pushAll(mode);
                    }
                }
                case "reload" -> PondererClientCommands.reloadLocal();
                case "list" -> PondererClientCommands.openItemList();
                case "edit" -> {
                    if (parts.length > 1) {
                        openEditor(ResourceLocation.parse(parts[1]));
                    }
                }
                case "export" -> invokePrivate("openExportScreen");
                case "import" -> invokePrivate("openImportScreen");
                case "new" -> {
                    if (parts.length > 1 && parts[1].equalsIgnoreCase("hand")) {
                        PondererClientCommands.newSceneFromHand(null);
                    } else if (parts.length > 1) {
                        PondererClientCommands.newSceneForItem(ResourceLocation.parse(parts[1]), null);
                    }
                }
                case "delete" -> {
                    if (parts.length > 2 && parts[1].equalsIgnoreCase("item")) {
                        PondererClientCommands.deleteScenesForItem(ResourceLocation.parse(parts[2]));
                    } else if (parts.length > 1) {
                        PondererClientCommands.deleteScene(ResourceLocation.parse(parts[1]));
                    }
                }
                case "copy" -> {
                    if (parts.length > 2) {
                        PondererClientCommands.copyScene(ResourceLocation.parse(parts[1]), ResourceLocation.parse(parts[2]));
                    }
                }
                case "download" -> {
                    if (parts.length > 1) {
                        PondererClientCommands.requestStructureDownload(ResourceLocation.parse(parts[1]));
                    }
                }
                case "convert" -> handleConvert(parts);
                case "unregister_pack" -> {
                    if (parts.length > 1) {
                        String nameArg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        invokePrivate("unregisterPack", String.class, nameArg);
                    }
                }
                default -> notifyClient("Unknown Ponderer client command: " + name);
            }
        } catch (Exception e) {
            notifyClient("Ponderer client command failed: " + command + " (" + rootMessage(e) + ")");
        }
    }

    private static void handleConvert(String[] parts) throws Exception {
        if (parts.length < 2) return;
        boolean toPjs = parts[1].equalsIgnoreCase("to_ponderjs");
        boolean fromPjs = parts[1].equalsIgnoreCase("from_ponderjs");
        if (!toPjs && !fromPjs) return;

        boolean all = parts.length > 2 && parts[2].equalsIgnoreCase("all");
        if (all) {
            if (toPjs) PondererClientCommands.convertAllToPonderJs();
            else PondererClientCommands.convertAllFromPonderJs();
        } else if (parts.length > 2) {
            invokePrivateWithRL(toPjs ? "convertToPonderJs" : "convertFromPonderJs", ResourceLocation.parse(parts[2]));
        }
    }

    private static void openEditor(ResourceLocation sceneId) {
        var match = SceneRuntime.findBySceneId(sceneId);
        if (match == null) {
            notifyClient("Ponderer scene not found locally: " + sceneId);
            return;
        }

        try {
            Object scene = match.scene();
            Class<?> sceneClass = scene.getClass();
            Class<?> editorClass = Class.forName("com.nododiiiii.ponderer.ui.SceneEditorScreen");
            Object canModify = editorClass.getMethod("canModifyScene", sceneClass).invoke(null, scene);
            if (canModify instanceof Boolean allowed && !allowed) {
                notifyClient("Ponderer scene cannot be edited: " + sceneId);
                return;
            }
            Object editor = editorClass.getConstructor(sceneClass, int.class).newInstance(scene, match.sceneIndex());
            if (editor instanceof Screen screen) {
                Minecraft.getInstance().setScreen(screen);
            }
        } catch (ReflectiveOperationException e) {
            notifyClient("Ponderer editor failed to open: " + rootMessage(e));
        }
    }

    private static void invokePrivate(String method) throws Exception {
        Method m = PondererClientCommands.class.getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(null);
    }

    private static void invokePrivate(String method, Class<?> type, Object arg) throws Exception {
        Method m = PondererClientCommands.class.getDeclaredMethod(method, type);
        m.setAccessible(true);
        m.invoke(null, arg);
    }

    private static void invokePrivateWithRL(String method, ResourceLocation rl) throws Exception {
        Method m = PondererClientCommands.class.getDeclaredMethod(method, ResourceLocation.class);
        m.setAccessible(true);
        m.invoke(null, rl);
    }

    private static void notifyClient(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null) cur = cur.getCause();
        return cur.getMessage() == null ? cur.getClass().getSimpleName() : cur.getMessage();
    }
}
