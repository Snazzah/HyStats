package com.snazzah.hystats;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.asset.builder.Builder;
import com.hypixel.hytale.server.npc.asset.builder.BuilderInfo;
import com.hypixel.hytale.server.npc.role.builders.BuilderRoleVariant;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.snazzah.hystats.systems.*;
import com.snazzah.hystats.util.StatisticsManager;
import com.snazzah.hystats.util.PlayerStatistics;
import com.snazzah.hystats.util.StatsCommand;
import com.snazzah.hystats.util.StatsStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HyStats extends JavaPlugin {
    private static HyStats INSTANCE;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final String CUSTOM_CATEGORY = "custom";
    
    private final Set<String> registeredCategories;
    private final Map<String, CustomStatConfig> customStats;
    private StatisticsManager statisticsManager;
    private PlayerMovementSystem movementSystem;
    private ScheduledFuture<?> autoSaveScheduler;

    public HyStats(@Nonnull JavaPluginInit init) {
        super(init);
        this.registeredCategories = ConcurrentHashMap.newKeySet();
        this.customStats = new ConcurrentHashMap<>();
    }

    public static HyStats get() {
        return INSTANCE;
    }

    @Override
    protected void setup() {
        INSTANCE = this;
        LOGGER.atInfo().log("Setting up...");

        Path configDirectory = this.getDataDirectory().getParent().resolve("HyStats");
        StatsStorage.getInstance().initialize(configDirectory);

        this.statisticsManager = StatisticsManager.getInstance();
        this.autoSaveScheduler = HytaleServer.SCHEDULED_EXECUTOR.scheduleWithFixedDelay(() -> {
            StatisticsManager.getInstance().saveAllStats();
        }, StatisticsManager.AUTO_SAVE_INTERVAL_SECONDS, StatisticsManager.AUTO_SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        
        registerDefaultCategories();
        registerDefaultCustomStats();

        new HyStatsEventHandler().register(this);
        this.getEntityStoreRegistry().registerSystem(new BlockBreakSystem(this));
        this.getEntityStoreRegistry().registerSystem(new BlockPlaceSystem(this));
        this.getEntityStoreRegistry().registerSystem(new ItemPickupSystem(this));
        this.getEntityStoreRegistry().registerSystem(new ItemDropSystem(this));
        this.getEntityStoreRegistry().registerSystem(new CraftSystem(this));
        this.getEntityStoreRegistry().registerSystem(new PlayTimeSystem(this));
        this.getEntityStoreRegistry().registerSystem(new PlayerKillSystem(this));
        this.getEntityStoreRegistry().registerSystem(new PlayerKilledSystem(this));
        this.getEntityStoreRegistry().registerSystem(new DamageSystem(this));
        this.getEntityStoreRegistry().registerSystem(new BlockInteractionSystem(this));
        this.movementSystem = new PlayerMovementSystem(this);
        this.getEntityStoreRegistry().registerSystem(this.movementSystem);

        this.getCommandRegistry().registerCommand(new StatsCommand());

        LOGGER.atInfo().log("Setup complete");
    }

    @Override
    protected void start() {
        LOGGER.atInfo().log("Started successfully");
    }

    @Override
    protected void shutdown() {
        LOGGER.atInfo().log("Shutting down...");

        try {
            if (this.autoSaveScheduler != null) {
                var cancelled = this.autoSaveScheduler.cancel(false);
                if (!cancelled) LOGGER.atWarning().log("Could not cancel running auto-save task!");
            }
            statisticsManager.saveAllAndClear().get(60, TimeUnit.SECONDS);
            LOGGER.atInfo().log("All stats saved successfully");
        } catch (Exception e) {
            LOGGER.atSevere().log("Failed to save all stats during shutdown: " + e.getMessage());
        }
        
        LOGGER.atInfo().log("Shutdown complete");
    }
    
    private void registerDefaultCategories() {
        registerCategory("custom");
        registerCategory("mined");
        registerCategory("placed");
        registerCategory("crafted");
        registerCategory("picked_up");
        registerCategory("dropped");
        registerCategory("killed");
        registerCategory("killed_by");
    }
    
    private void registerDefaultCustomStats() {
        registerCustomStat("play_time", "Play Time", CustomStatConfig.DisplayFormat.TIME);
        registerCustomStat("deaths", "Deaths");
        registerCustomStat("mob_kills", "Mob Kills");
        registerCustomStat("player_kills", "Player Kills");
        registerCustomStat("damage_dealt", "Damage Dealt");
        registerCustomStat("damage_taken", "Damage Taken");
        registerCustomStat("times_connected", "Times Connected");
        registerCustomStat("messages_sent", "Messages Sent");
        registerCustomStat("times_slept", "Times Slept");
        registerCustomStat("jumps", "Jumps");
        registerCustomStat("drops", "Items Dropped");

        registerCustomStat("distance_walk", "Distance Walked", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_sprint", "Distance Sprinted", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_sneak", "Distance Sneaked", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_swim", "Distance Swum", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_fly", "Distance Flown", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_climb", "Distance Climbed", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_fall", "Distance Fallen", CustomStatConfig.DisplayFormat.DISTANCE);
        registerCustomStat("distance_glide", "Distance Glided", CustomStatConfig.DisplayFormat.DISTANCE);

        registerCustomStat("used_furnace", "Interactions with Furnace");
        registerCustomStat("used_workbench", "Interactions with Workbench");
        registerCustomStat("used_tannery", "Interactions with Tanning Rack");
        registerCustomStat("used_trough", "Interactions with Trough");
        registerCustomStat("used_anvil", "Interactions with Blacksmith Anvil");
        registerCustomStat("used_alchemy_bench", "Interactions with Alchemist's Workbench");
        registerCustomStat("used_arcane_bench", "Interactions with Arcanist's Workbench");
        registerCustomStat("used_armor_bench", "Interactions with Armorer's Workbench");
        registerCustomStat("used_builder_bench", "Interactions with Builder's Workbench");
        registerCustomStat("used_stove", "Interactions with Chef's Stove");
        registerCustomStat("used_farmer_bench", "Interactions with Farmer's Workbench");
        registerCustomStat("used_furniture_bench", "Interactions with Furniture Workbench");
        registerCustomStat("used_salvage_bench", "Interactions with Salvager's Workbench");
        registerCustomStat("used_chicken_coop", "Interactions with Chicken Coop");
        registerCustomStat("used_memories_bench", "Interactions with The Heart of Orbis");
    }
    
    public boolean registerCategory(@Nonnull String category) {
        if (!isValidIdentifier(category)) {
            LOGGER.atWarning().log("Invalid category identifier: " + category + " (must be alphanumeric with dashes and underscores)");
            return false;
        }
        
        boolean added = registeredCategories.add(category);
        if (added) LOGGER.atInfo().log("Registered category: " + category);
        return added;
    }
    
    public boolean registerCustomStat(@Nonnull String stat, @Nonnull String displayName) {
        return registerCustomStat(stat, displayName, CustomStatConfig.DisplayFormat.NUMBER);
    }
    
    public boolean registerCustomStat(@Nonnull String stat, @Nonnull String displayName, @Nonnull CustomStatConfig.DisplayFormat format) {
        if (!isValidIdentifier(stat)) {
            LOGGER.atWarning().log("Invalid stat identifier: " + stat + " (must be alphanumeric with dashes and underscores)");
            return false;
        }
        
        CustomStatConfig config = new CustomStatConfig(stat, displayName, format);
        customStats.put(stat, config);
        LOGGER.atInfo().log("Registered custom stat: " + stat + " (" + displayName + ", format=" + format + ")");
        return true;
    }
    
    @Nullable
    public PlayerMovementSystem getMovementSystem() {
        return movementSystem;
    }
    
    @Nullable
    public String getCustomStatName(@Nonnull String stat) {
        CustomStatConfig config = customStats.get(stat);
        return config != null ? config.displayName() : null;
    }
    
    @Nullable
    public CustomStatConfig getCustomStatConfig(@Nonnull String stat) {
        return customStats.get(stat);
    }
    
    @Nonnull
    public Map<String, String> getAllCustomStatNames() {
        Map<String, String> names = new HashMap<>();
        customStats.forEach((key, config) -> names.put(key, config.displayName()));
        return Collections.unmodifiableMap(names);
    }
    
    @Nonnull
    public Map<String, CustomStatConfig> getAllCustomStats() {
        return Collections.unmodifiableMap(customStats);
    }
    
    @Nonnull
    public String getFormattedStat(@Nonnull UUID playerUuid, @Nonnull String stat) {
        long value = getStat(playerUuid, CUSTOM_CATEGORY, stat);
        CustomStatConfig config = getCustomStatConfig(stat);
        if (config != null) {
            return config.formatValue(value);
        }
        return String.valueOf(value);
    }
    
    public void incrementStat(@Nonnull UUID playerUuid, @Nonnull String category, @Nonnull String stat, long amount) {
        PlayerStatistics stats = statisticsManager.getPlayerStats(playerUuid);
        if (stats == null) {
            return;
        }
        
        if (!isValidIdentifier(category) || !isValidIdentifier(stat)) {
            LOGGER.atWarning().log("Invalid identifier(s): category=" + category + ", stat=" + stat);
            return;
        }
        
        stats.increment(category, stat, amount);
    }
    
    public void setStat(@Nonnull UUID playerUuid, @Nonnull String category, @Nonnull String stat, long value) {
        PlayerStatistics stats = statisticsManager.getPlayerStats(playerUuid);
        if (stats == null) {
            return;
        }
        
        if (!isValidIdentifier(category) || !isValidIdentifier(stat)) {
            this.getLogger().at(Level.WARNING)
                .log("Invalid identifier(s): category=" + category + ", stat=" + stat);
            return;
        }
        
        long currentValue = stats.get(category, stat);
        long difference = value - currentValue;
        stats.increment(category, stat, difference);
    }
    
    public void resetStat(@Nonnull UUID playerUuid, @Nonnull String category, @Nonnull String stat) {
        setStat(playerUuid, category, stat, 0);
    }

    public long getStat(@Nonnull UUID playerUuid, @Nonnull String category, @Nonnull String stat) {
        PlayerStatistics stats = statisticsManager.getPlayerStats(playerUuid);
        if (stats == null) {
            return 0L;
        }
        
        return stats.get(category, stat);
    }
    
    @Nonnull
    public Map<String, Long> getCategoryStats(@Nonnull UUID playerUuid, @Nonnull String category) {
        PlayerStatistics stats = statisticsManager.getPlayerStats(playerUuid);
        if (stats == null) {
            return Collections.emptyMap();
        }
        
        return stats.getCategory(category);
    }
    
    public boolean isPlayerStatsLoaded(@Nonnull UUID playerUuid) {
        return statisticsManager.isPlayerStatsLoaded(playerUuid);
    }
    
    @Nonnull
    public CompletableFuture<PlayerStatistics> loadOfflinePlayerStats(@Nonnull UUID playerUuid) {
        return statisticsManager.loadOfflinePlayerStats(playerUuid);
    }

    @Nonnull
    public String resolveBaseNpcRoleId(@Nonnull String roleName) {
        if (roleName.isBlank()) return roleName;

        NPCPlugin npcPlugin = NPCPlugin.get();
        int roleIndex = npcPlugin.getIndex(roleName);
        BuilderInfo builderInfo = npcPlugin.getRoleBuilderInfo(roleIndex);
        if (builderInfo == null) return roleName;

        Builder<?> builder = builderInfo.getBuilder();
        if (builder instanceof BuilderRoleVariant variant && (roleName.endsWith("_Patrol") || roleName.endsWith("_Wander"))) {

            String referenceName = npcPlugin.getName(variant.getReferenceIndex());
            if (referenceName != null && !referenceName.isBlank() && !referenceName.startsWith("Template_") && roleName.startsWith(referenceName))
                return referenceName;
        }

        return roleName;
    }

    
    
    public boolean isCategoryRegistered(@Nonnull String category) {
        return registeredCategories.contains(category);
    }
    
    private boolean isValidIdentifier(@Nonnull String identifier) {
        return identifier.matches("^[.\\w-]+$");
    }
}
