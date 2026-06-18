package com.ponderer.addon;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;

public final class ClientCommandDispatcher {

    private ClientCommandDispatcher() {}

    public static void dispatch(String command) {
        if (command == null || command.isBlank()) return;
        String[] parts = command.trim().split("\\s+", -1);
        if (parts.length == 0 || parts[0].isBlank()) return;

        String name = parts[0].toLowerCase();
        if (!PondererAddonConfig.isClientCommandAllowed(name)) {
            notifyClient(PondererAddonMessages.get("client.command_disabled", name));
            return;
        }

        try {
            switch (name) {
                case "pull" -> {
                    String mode = parts.length > 1 ? parts[1] : "check";
                    invokeCommand("pull", mode);
                }
                case "push" -> {
                    boolean force = parts.length > 1 && parts[1].equalsIgnoreCase("force");
                    String mode = force ? "force" : "check";
                    int idIdx = force ? 2 : 1;
                    if (parts.length > idIdx) {
                        invokeCommand("push", ResourceLocation.parse(parts[idIdx]), mode);
                    } else {
                        invokeCommand("pushAll", mode);
                    }
                }
                case "reload" -> invokeCommand("reloadLocal");
                case "list" -> invokeCommand("openItemList");
                case "edit" -> {
                    if (parts.length > 1) {
                        openEditor(ResourceLocation.parse(parts[1]));
                    }
                }
                case "export" -> invokeCommand("openExportScreen");
                case "import" -> invokeCommand("openImportScreen");
                case "new" -> {
                    if (parts.length > 1 && parts[1].equalsIgnoreCase("hand")) {
                        invokeCommand("newSceneFromHand", (Object) null);
                    } else if (parts.length > 1) {
                        invokeCommand("newSceneForItem", ResourceLocation.parse(parts[1]), null);
                    }
                }
                case "delete" -> {
                    if (parts.length > 2 && parts[1].equalsIgnoreCase("item")) {
                        invokeCommand("deleteScenesForItem", ResourceLocation.parse(parts[2]));
                    } else if (parts.length > 1) {
                        invokeCommand("deleteScene", ResourceLocation.parse(parts[1]));
                    }
                }
                case "copy" -> {
                    if (parts.length > 2) {
                        invokeCommand("copyScene", ResourceLocation.parse(parts[1]), ResourceLocation.parse(parts[2]));
                    }
                }
                case "download" -> {
                    if (parts.length > 1) {
                        invokeCommand("requestStructureDownload", ResourceLocation.parse(parts[1]));
                    }
                }
                case "convert" -> handleConvert(parts);
                case "unregister_pack" -> {
                    if (parts.length > 1) {
                        String nameArg = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                        invokeCommand("unregisterPack", nameArg);
                    }
                }
                default -> notifyClient(PondererAddonMessages.get("client.unknown_command", name));
            }
        } catch (Exception e) {
            notifyClient(PondererAddonMessages.get("client.command_failed", command, rootMessage(e)));
        }
    }

    private static void handleConvert(String[] parts) throws Exception {
        if (parts.length < 2) return;
        boolean toPjs = parts[1].equalsIgnoreCase("to_ponderjs");
        boolean fromPjs = parts[1].equalsIgnoreCase("from_ponderjs");
        if (!toPjs && !fromPjs) return;

        boolean all = parts.length > 2 && parts[2].equalsIgnoreCase("all");
        if (all) {
            invokeCommand(toPjs ? "convertAllToPonderJs" : "convertAllFromPonderJs");
        } else if (parts.length > 2) {
            invokeCommand(toPjs ? "convertToPonderJs" : "convertFromPonderJs", ResourceLocation.parse(parts[2]));
        }
    }

    private static void openEditor(ResourceLocation sceneId) {
        try {
            Object match = PondererReflection.findSceneById(sceneId);
            if (match == null) {
                notifyClient(PondererAddonMessages.get("client.scene_not_found", sceneId));
                return;
            }

            Object scene = PondererReflection.scene(match);
            if (!PondererReflection.canModifyScene(scene)) {
                notifyClient(PondererAddonMessages.get("client.scene_not_editable", sceneId));
                return;
            }

            Screen editor = PondererReflection.createEditor(scene, PondererReflection.sceneIndex(match));
            if (editor != null) {
                Minecraft.getInstance().setScreen(editor);
            }
        } catch (ReflectiveOperationException e) {
            notifyClient(PondererAddonMessages.get("client.editor_open_failed", rootMessage(e)));
        }
    }

    private static Object invokeCommand(String method, Object... args) throws ReflectiveOperationException {
        return PondererReflection.invokeStatic(PondererReflection.CLIENT_COMMANDS, method, args);
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
