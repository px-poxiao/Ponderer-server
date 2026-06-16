package com.ponderer.server.storage;

import com.ponderer.server.network.packets.SyncResponsePacket;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public final class SceneStore {

    private static final String BASE_DIR = "ponderer";
    private static final String SCRIPT_DIR = "scripts";
    private static final String STRUCTURE_DIR = "structures";

    private final Path worldRoot;
    private final Logger logger;

    public SceneStore(Path worldRoot, Logger logger) {
        this.worldRoot = worldRoot;
        this.logger = logger;
    }

    public Path getSceneDir() {
        return worldRoot.resolve(BASE_DIR).resolve(SCRIPT_DIR);
    }

    public Path getStructureDir() {
        return worldRoot.resolve(BASE_DIR).resolve(STRUCTURE_DIR);
    }

    public boolean saveScene(String sceneId, String json) {
        Path path = resolveScenePath(sceneId);
        if (path == null) return false;
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, json);
            return true;
        } catch (IOException e) {
            logger.warning("Failed to write scene " + sceneId + ": " + e.getMessage());
            return false;
        }
    }

    public boolean saveStructure(String structureId, byte[] bytes) {
        if (structureId == null || structureId.isBlank() || bytes == null) return true;
        Path path = resolveStructurePath(structureId);
        if (path == null) return false;
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            return true;
        } catch (IOException e) {
            logger.warning("Failed to write structure " + structureId + ": " + e.getMessage());
            return false;
        }
    }

    public byte[] readScene(String sceneId) {
        Path path = resolveScenePath(sceneId);
        if (path == null || !Files.exists(path)) return null;
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            return null;
        }
    }

    public boolean sceneExists(String sceneId) {
        Path path = resolveScenePath(sceneId);
        return path != null && Files.exists(path);
    }

    public boolean deleteScene(String sceneId) {
        Path path = resolveScenePath(sceneId);
        if (path == null) return false;
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            logger.warning("Failed to delete scene " + sceneId + ": " + e.getMessage());
            return false;
        }
    }

    public List<SyncResponsePacket.FileEntry> collectScripts() {
        return collectFiles(getSceneDir(), ".json");
    }

    public List<SyncResponsePacket.FileEntry> collectStructures() {
        return collectFiles(getStructureDir(), ".nbt");
    }

    public int countScenesForPlayer(String playerName) {
        Path dir = getSceneDir().resolve(playerName.toLowerCase(Locale.ROOT));
        if (!Files.exists(dir)) return 0;
        try (var stream = Files.list(dir)) {
            return (int) stream.filter(p -> p.toString().endsWith(".json")).count();
        } catch (IOException e) {
            return 0;
        }
    }

    public int countTotalScenes() {
        List<SyncResponsePacket.FileEntry> scripts = collectScripts();
        return scripts.size();
    }

    private List<SyncResponsePacket.FileEntry> collectFiles(Path root, String ext) {
        List<SyncResponsePacket.FileEntry> entries = new ArrayList<>();
        if (!Files.exists(root)) return entries;
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.toString().toLowerCase(Locale.ROOT).endsWith(ext)) {
                        String id = toId(root, file, ext);
                        if (id != null) {
                            entries.add(new SyncResponsePacket.FileEntry(id, Files.readAllBytes(file)));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            logger.warning("Failed to collect files from " + root + ": " + e.getMessage());
        }
        return entries;
    }

    public Path resolveScenePath(String sceneId) {
        return resolvePath(getSceneDir(), sceneId, ".json");
    }

    public Path resolveStructurePath(String structureId) {
        return resolvePath(getStructureDir(), structureId, ".nbt");
    }

    private Path resolvePath(Path root, String id, String ext) {
        if (id == null || id.isBlank()) return null;
        int colon = id.indexOf(':');
        if (colon < 0) return null;
        String namespace = id.substring(0, colon);
        String path = id.substring(colon + 1);
        if (!isSafePath(path)) return null;
        return namespace.equals("ponderer")
                ? root.resolve(path + ext)
                : root.resolve(namespace).resolve(path + ext);
    }

    private static boolean isSafePath(String path) {
        return !path.contains("..") && !path.contains("\\") && path.matches("[a-zA-Z0-9/_\\-]+");
    }

    private static String toId(Path root, Path file, String ext) {
        Path rel = root.relativize(file);
        if (rel.getNameCount() < 1) return null;
        String namespace;
        String path;
        if (rel.getNameCount() == 1) {
            namespace = "ponderer";
            path = rel.getName(0).toString();
        } else {
            namespace = rel.getName(0).toString();
            path = rel.subpath(1, rel.getNameCount()).toString().replace("\\", "/");
        }
        if (path.endsWith(ext)) path = path.substring(0, path.length() - ext.length());
        return namespace + ":" + path;
    }
}