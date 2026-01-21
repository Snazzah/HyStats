package com.snazzah.hystats.util;

import com.snazzah.hystats.HyStats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.logging.Level;

public class StatisticsManager {
    private static final StatisticsManager INSTANCE = new StatisticsManager();
    public static final int AUTO_SAVE_INTERVAL_SECONDS = 300;
    
    private final ConcurrentHashMap<UUID, PlayerStatistics> onlinePlayerStats;
    private final StatsStorage storage;
    private ScheduledExecutorService autoSaveExecutor;
    private ScheduledFuture<?> autoSaveTask;
    
    private StatisticsManager() {
        this.onlinePlayerStats = new ConcurrentHashMap<>();
        this.storage = StatsStorage.getInstance();
    }
    
    public static StatisticsManager getInstance() {
        return INSTANCE;
    }

    public void saveAllStats() {
        if (onlinePlayerStats.isEmpty()) return;
        
        HyStats.get().getLogger().at(Level.FINE)
            .log("Saving all stats for " + onlinePlayerStats.size() + " online players");
        
        onlinePlayerStats.forEach((uuid, stats) -> {
            storage.savePlayerStats(uuid, stats).exceptionally(ex -> {
                HyStats.get().getLogger().at(Level.SEVERE)
                    .log("Save-all failed for player " + uuid + ": " + ex.getMessage());
                return null;
            });
        });
    }

    @Nonnull
    public CompletableFuture<PlayerStatistics> loadPlayerStats(@Nonnull UUID playerUuid) {
        return storage.loadPlayerStats(playerUuid).thenApply(stats -> {
            onlinePlayerStats.put(playerUuid, stats);
            HyStats.get().getLogger().at(Level.INFO).log("Loaded stats for player: " + playerUuid);
            return stats;
        });
    }

    @Nonnull
    public CompletableFuture<Void> savePlayerStats(@Nonnull UUID playerUuid) {
        PlayerStatistics stats = onlinePlayerStats.get(playerUuid);
        if (stats == null) return CompletableFuture.completedFuture(null);
        
        return storage.savePlayerStats(playerUuid, stats);
    }

    @Nonnull
    public CompletableFuture<Void> unloadPlayerStats(@Nonnull UUID playerUuid) {
        PlayerStatistics stats = onlinePlayerStats.get(playerUuid);
        if (stats == null) return CompletableFuture.completedFuture(null);
        
        return storage.savePlayerStats(playerUuid, stats).thenRun(() -> {
            onlinePlayerStats.remove(playerUuid);
            HyStats.get().getLogger().at(Level.INFO).log("Unloaded stats for player: " + playerUuid);
        });
    }

    @Nullable
    public PlayerStatistics getPlayerStats(@Nonnull UUID playerUuid) {
        return onlinePlayerStats.get(playerUuid);
    }

    public boolean isPlayerStatsLoaded(@Nonnull UUID playerUuid) {
        return onlinePlayerStats.containsKey(playerUuid);
    }

    @Nonnull
    public CompletableFuture<Void> saveAllAndClear() {
        if (onlinePlayerStats.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }
        
        HyStats.get().getLogger().at(Level.INFO).log("Saving stats for " + onlinePlayerStats.size() + " players...");
        
        CompletableFuture<?>[] futures = onlinePlayerStats.entrySet().stream()
            .map(entry -> storage.savePlayerStats(entry.getKey(), entry.getValue()))
            .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures).thenRun(() -> {
            onlinePlayerStats.clear();
            HyStats.get().getLogger().at(Level.INFO).log("All player stats saved and cleared from memory");
        });
    }

    @Nonnull
    public CompletableFuture<PlayerStatistics> loadOfflinePlayerStats(@Nonnull UUID playerUuid) {
        return storage.loadPlayerStats(playerUuid);
    }
}
