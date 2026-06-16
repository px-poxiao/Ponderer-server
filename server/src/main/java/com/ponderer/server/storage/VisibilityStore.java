package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class VisibilityStore {

    public static final class VisibilityEntry {
        public String visibility = "public"; // "public" | "private" | "group"
        public List<String> groups = new ArrayList<>();
    }

    private static final Gson GSON = new Gson();
    private static final Type VIS_TYPE = new TypeToken<Map<String, VisibilityEntry>>() {}.getType();
    private static final Type GRP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    private final Path visFile;
    private final Path grpFile;
    private final Logger logger;
    private Map<String, VisibilityEntry> visibility;
    private Map<String, List<String>> groups; // groupName -> [uuids]

    public VisibilityStore(Path worldRoot, Logger logger) {
        this.visFile = worldRoot.resolve("ponderer").resolve("scene_visibility.json");
        this.grpFile = worldRoot.resolve("ponderer").resolve("visibility_groups.json");
        this.logger = logger;
        this.visibility = loadVis();
        this.groups = loadGrp();
    }

    public String getVisibility(String sceneId) {
        VisibilityEntry e = visibility.get(sceneId);
        return e == null ? "public" : e.visibility;
    }

    public void setVisibility(String sceneId, String vis, List<String> grps) {
        VisibilityEntry e = new VisibilityEntry();
        e.visibility = vis; e.groups = grps != null ? grps : new ArrayList<>();
        visibility.put(sceneId, e); saveVis();
    }

    public boolean canSee(String sceneId, UUID viewer, UUID owner) {
        VisibilityEntry e = visibility.get(sceneId);
        if (e == null || "public".equals(e.visibility)) return true;
        if ("private".equals(e.visibility)) return viewer.equals(owner);
        // group
        String viewerStr = viewer.toString();
        for (String grpName : e.groups) {
            List<String> members = groups.getOrDefault(grpName, List.of());
            if (members.contains(viewerStr)) return true;
        }
        return viewer.equals(owner);
    }

    public void createGroup(String name) {
        groups.putIfAbsent(name, new ArrayList<>()); saveGrp();
    }

    public void deleteGroup(String name) { groups.remove(name); saveGrp(); }

    public void addPlayerToGroup(String group, UUID uuid) {
        groups.computeIfAbsent(group, k -> new ArrayList<>()).add(uuid.toString()); saveGrp();
    }

    public void removePlayerFromGroup(String group, UUID uuid) {
        List<String> members = groups.get(group);
        if (members != null) { members.remove(uuid.toString()); saveGrp(); }
    }

    public Set<String> getGroups() { return groups.keySet(); }

    public List<String> getGroupMembers(String group) {
        return groups.getOrDefault(group, List.of());
    }

    private Map<String, VisibilityEntry> loadVis() {
        if (!Files.exists(visFile)) return new HashMap<>();
        try { Map<String, VisibilityEntry> m = GSON.fromJson(Files.readString(visFile), VIS_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning("Failed to load visibility: " + e.getMessage()); return new HashMap<>(); }
    }

    private Map<String, List<String>> loadGrp() {
        if (!Files.exists(grpFile)) return new HashMap<>();
        try { Map<String, List<String>> m = GSON.fromJson(Files.readString(grpFile), GRP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning("Failed to load groups: " + e.getMessage()); return new HashMap<>(); }
    }

    private void saveVis() {
        try { Files.createDirectories(visFile.getParent()); Files.writeString(visFile, GSON.toJson(visibility)); }
        catch (IOException e) { logger.warning("Failed to save visibility: " + e.getMessage()); }
    }

    private void saveGrp() {
        try { Files.createDirectories(grpFile.getParent()); Files.writeString(grpFile, GSON.toJson(groups)); }
        catch (IOException e) { logger.warning("Failed to save groups: " + e.getMessage()); }
    }
}
