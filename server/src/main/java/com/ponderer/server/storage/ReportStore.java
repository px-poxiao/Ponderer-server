package com.ponderer.server.storage;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

public final class ReportStore {

    public static final class ReportEntry {
        public String sceneId;
        public Map<String, String> reports = new HashMap<>(); // uuid -> reason
        public boolean flagged;
    }

    private static final Gson GSON = new Gson();
    private static final Type MAP_TYPE = new TypeToken<Map<String, ReportEntry>>() {}.getType();

    private final Path dataFile;
    private final Logger logger;
    private Map<String, ReportEntry> data;

    public ReportStore(Path worldRoot, Logger logger) {
        this.dataFile = worldRoot.resolve("ponderer").resolve("scene_reports.json");
        this.logger = logger;
        this.data = load();
    }

    public int addReport(String sceneId, UUID reporter, String reason) {
        ReportEntry e = data.computeIfAbsent(sceneId, k -> { ReportEntry r = new ReportEntry(); r.sceneId = k; return r; });
        e.reports.put(reporter.toString(), reason != null ? reason : "");
        save();
        return e.reports.size();
    }

    public int getReportCount(String sceneId) {
        ReportEntry e = data.get(sceneId); return e == null ? 0 : e.reports.size();
    }

    public boolean isFlagged(String sceneId) {
        ReportEntry e = data.get(sceneId); return e != null && e.flagged;
    }

    public void setFlagged(String sceneId, boolean flagged) {
        ReportEntry e = data.computeIfAbsent(sceneId, k -> { ReportEntry r = new ReportEntry(); r.sceneId = k; return r; });
        e.flagged = flagged; save();
    }

    public void dismiss(String sceneId) { data.remove(sceneId); save(); }

    public List<String> getAllFlagged() {
        return data.values().stream().filter(e -> e.flagged).map(e -> e.sceneId).toList();
    }

    private Map<String, ReportEntry> load() {
        if (!Files.exists(dataFile)) return new HashMap<>();
        try { Map<String, ReportEntry> m = GSON.fromJson(Files.readString(dataFile), MAP_TYPE); return m != null ? m : new HashMap<>(); }
        catch (IOException e) { logger.warning("Failed to load reports: " + e.getMessage()); return new HashMap<>(); }
    }

    private void save() {
        try { Files.createDirectories(dataFile.getParent()); Files.writeString(dataFile, GSON.toJson(data)); }
        catch (IOException e) { logger.warning("Failed to save reports: " + e.getMessage()); }
    }
}
