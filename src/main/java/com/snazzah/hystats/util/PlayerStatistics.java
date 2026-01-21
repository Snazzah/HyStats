package com.snazzah.hystats.util;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatistics {
    private static final int DATA_VERSION = 1;
    
    private final Map<String, Map<String, Long>> stats;
    private int dataVersion;
    
    public PlayerStatistics() {
        this.stats = new ConcurrentHashMap<>();
        this.dataVersion = DATA_VERSION;
    }

    public synchronized void increment(@Nonnull String category, @Nonnull String stat, long amount) {
        Map<String, Long> categoryStats = stats.computeIfAbsent(category, k -> new ConcurrentHashMap<>());
        categoryStats.merge(stat, amount, Long::sum);
    }

    public long get(@Nonnull String category, @Nonnull String stat) {
        return stats.getOrDefault(category, Collections.emptyMap())
                   .getOrDefault(stat, 0L);
    }

    @Nonnull
    public Map<String, Long> getCategory(@Nonnull String category) {
        Map<String, Long> categoryStats = stats.get(category);
        if (categoryStats == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(new HashMap<>(categoryStats));
    }

    @Nonnull
    public Map<String, Map<String, Long>> getAll() {
        Map<String, Map<String, Long>> copy = new HashMap<>();
        stats.forEach((category, categoryStats) -> 
            copy.put(category, Collections.unmodifiableMap(new HashMap<>(categoryStats)))
        );
        return Collections.unmodifiableMap(copy);
    }

    public boolean hasCategory(@Nonnull String category) {
        Map<String, Long> categoryStats = stats.get(category);
        return categoryStats != null && !categoryStats.isEmpty();
    }

    public int getDataVersion() {
        return dataVersion;
    }

    public void setDataVersion(int dataVersion) {
        this.dataVersion = dataVersion;
    }

    public Map<String, Map<String, Long>> getStatsInternal() {
        return stats;
    }

    public void setStats(@Nonnull Map<String, Map<String, Long>> loadedStats) {
        stats.clear();
        loadedStats.forEach((category, categoryStats) -> {
            Map<String, Long> newCategoryStats = new ConcurrentHashMap<>(categoryStats);
            stats.put(category, newCategoryStats);
        });
    }
}
