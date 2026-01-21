package com.snazzah.hystats.util;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class SparseStatisticsSerializer implements JsonSerializer<PlayerStatistics>, JsonDeserializer<PlayerStatistics> {
    
    @Override
    public JsonElement serialize(PlayerStatistics src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject root = new JsonObject();
        root.addProperty("DataVersion", src.getDataVersion());
        
        JsonObject statsObject = new JsonObject();
        Map<String, Map<String, Long>> allStats = src.getStatsInternal();
        
        // Only add categories that have non-zero stats
        allStats.forEach((category, categoryStats) -> {
            JsonObject categoryObject = new JsonObject();
            boolean hasNonZeroStats = false;
            
            // Only add non-zero stats
            for (Map.Entry<String, Long> entry : categoryStats.entrySet()) {
                if (entry.getValue() != 0L) {
                    categoryObject.addProperty(entry.getKey(), entry.getValue());
                    hasNonZeroStats = true;
                }
            }

            if (hasNonZeroStats) statsObject.add(category, categoryObject);
        });
        
        root.add("stats", statsObject);
        return root;
    }
    
    @Override
    public PlayerStatistics deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) 
            throws JsonParseException {
        PlayerStatistics stats = new PlayerStatistics();
        
        if (!json.isJsonObject()) return stats;
        
        JsonObject root = json.getAsJsonObject();
        
        // Read data version
        if (root.has("DataVersion")) stats.setDataVersion(root.get("DataVersion").getAsInt());
        
        // Read stats
        if (root.has("stats") && root.get("stats").isJsonObject()) {
            JsonObject statsObject = root.getAsJsonObject("stats");
            Map<String, Map<String, Long>> loadedStats = new HashMap<>();
            
            for (Map.Entry<String, JsonElement> categoryEntry : statsObject.entrySet()) {
                String category = categoryEntry.getKey();
                
                if (categoryEntry.getValue().isJsonObject()) {
                    JsonObject categoryObject = categoryEntry.getValue().getAsJsonObject();
                    Map<String, Long> categoryStats = new HashMap<>();
                    
                    for (Map.Entry<String, JsonElement> statEntry : categoryObject.entrySet()) {
                        String statKey = statEntry.getKey();
                        
                        if (statEntry.getValue().isJsonPrimitive()) {
                            long value = statEntry.getValue().getAsLong();
                            categoryStats.put(statKey, value);
                        }
                    }
                    
                    if (!categoryStats.isEmpty()) loadedStats.put(category, categoryStats);
                }
            }
            
            stats.setStats(loadedStats);
        }
        
        return stats;
    }
}
