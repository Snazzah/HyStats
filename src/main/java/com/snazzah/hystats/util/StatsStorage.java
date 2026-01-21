package com.snazzah.hystats.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.snazzah.hystats.HyStats;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class StatsStorage {
    private static final StatsStorage INSTANCE = new StatsStorage();
    private static final String STATS_FOLDER = "stats";
    
    private final Gson gson;
    private Path dataDirectory;
    private Path statsDirectory;

    private StatsStorage() {
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(PlayerStatistics.class, new SparseStatisticsSerializer())
                .create();
    }

    public static StatsStorage getInstance() {
        return INSTANCE;
    }

    public void initialize(@Nonnull Path dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.statsDirectory = dataDirectory.resolve(STATS_FOLDER);
        ensureDirectories();
    }

    private void ensureDirectories() {
        try {
            if (!Files.exists(dataDirectory)) {
                Files.createDirectories(dataDirectory);
            }
            if (!Files.exists(statsDirectory)) {
                Files.createDirectories(statsDirectory);
            }
        } catch (IOException e) {
            HyStats.get().getLogger().at(Level.SEVERE)
                .log("Failed to create data directories: " + e.getMessage());
        }
    }

    @Nonnull
    public CompletableFuture<PlayerStatistics> loadPlayerStats(@Nonnull UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (statsDirectory == null) {
                HyStats.get().getLogger().at(Level.WARNING)
                    .log("Stats directory not initialized!");
                return new PlayerStatistics();
            }

            Path statsFile = getStatsFile(playerUuid);
            
            if (!Files.exists(statsFile)) {
                // New player - return empty stats
                return new PlayerStatistics();
            }

            try {
                String json = Files.readString(statsFile);
                PlayerStatistics stats = gson.fromJson(json, PlayerStatistics.class);
                
                HyStats.get().getLogger().at(Level.FINE)
                    .log("Loaded stats for player: " + playerUuid);
                
                return stats != null ? stats : new PlayerStatistics();
            } catch (IOException e) {
                HyStats.get().getLogger().at(Level.SEVERE)
                    .log("Failed to load stats for " + playerUuid + ": " + e.getMessage());
                return new PlayerStatistics();
            } catch (JsonParseException e) {
                HyStats.get().getLogger().at(Level.SEVERE)
                    .log("Failed to parse stats JSON for " + playerUuid + ": " + e.getMessage());
                // Return empty stats if JSON is corrupted
                return new PlayerStatistics();
            }
        });
    }

    @Nonnull
    public CompletableFuture<Void> savePlayerStats(@Nonnull UUID playerUuid, @Nonnull PlayerStatistics stats) {
        return CompletableFuture.runAsync(() -> {
            if (statsDirectory == null) {
                HyStats.get().getLogger().at(Level.WARNING)
                    .log("Stats directory not initialized!");
                return;
            }

            ensureDirectories();
            Path statsFile = getStatsFile(playerUuid);

            try {
                String json = gson.toJson(stats);
                Files.writeString(statsFile, json);
                
                HyStats.get().getLogger().at(Level.FINE)
                    .log("Saved stats for player: " + playerUuid);
            } catch (IOException e) {
                HyStats.get().getLogger().at(Level.SEVERE)
                    .log("Failed to save stats for " + playerUuid + ": " + e.getMessage());
            }
        });
    }

    @Nonnull
    private Path getStatsFile(@Nonnull UUID playerUuid) {
        return statsDirectory.resolve(playerUuid.toString() + ".json");
    }

    @Nonnull
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    @Nonnull
    public Path getStatsDirectory() {
        return statsDirectory;
    }
}
