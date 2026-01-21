package com.snazzah.hystats.systems;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
public class BlockInteractionSystem extends EntityEventSystem<EntityStore, UseBlockEvent.Post> {
    private final HyStats plugin;

    private static final Map<String, String> INTERACTION_STATS = new HashMap<>();
    static {
        INTERACTION_STATS.put("Bench_Furnace", "used_furnace");
        INTERACTION_STATS.put("Bench_WorkBench", "used_workbench");
        INTERACTION_STATS.put("Bench_Tannery", "used_tannery");
        INTERACTION_STATS.put("Bench_Trough", "used_trough");
        INTERACTION_STATS.put("Bench_Weapon", "used_anvil");
        INTERACTION_STATS.put("Bench_Alchemy", "used_alchemy_bench");
        INTERACTION_STATS.put("Bench_Arcane", "used_arcane_bench");
        INTERACTION_STATS.put("Bench_Armor", "used_armor_bench");
        INTERACTION_STATS.put("Bench_Builders", "used_builder_bench");
        INTERACTION_STATS.put("Bench_Stove", "used_stove");
        INTERACTION_STATS.put("Bench_Farming", "used_farmer_bench");
        INTERACTION_STATS.put("Bench_Furniture", "used_furniture_bench");
        INTERACTION_STATS.put("Bench_Salvage", "used_salvage_bench");
        INTERACTION_STATS.put("Coop_Chicken", "used_chicken_coop");
        INTERACTION_STATS.put("Bench_Memories", "used_memories_bench");
    }

    public BlockInteractionSystem(HyStats plugin) {
        super(UseBlockEvent.Post.class);
        this.plugin = plugin;
    }
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull UseBlockEvent.Post event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        if (player == null || playerRef == null) return;

        UUID playerUuid = playerRef.getUuid();
        String blockTypeId = event.getBlockType().getId();
        String customStat = INTERACTION_STATS.get(blockTypeId);
        if (customStat != null) this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, customStat, 1);
    }

    @Nullable
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }
    @NonNullDecl
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }

    public static void registerBlockInteraction(String blockTypeId, String customStatName) {
        INTERACTION_STATS.put(blockTypeId, customStatName);
    }

    public static Map<String, String> getRegisteredInteractions() {
        return Collections.unmodifiableMap(INTERACTION_STATS);
    }
}