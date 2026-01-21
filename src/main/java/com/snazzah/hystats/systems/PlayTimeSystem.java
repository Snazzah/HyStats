package com.snazzah.hystats.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player play time in whole seconds.
 * Accumulates deltaTime and increments the stat when a full second has passed.
 */
public class PlayTimeSystem extends EntityTickingSystem<EntityStore> {
    private final HyStats plugin;
    private final Map<UUID, Double> accumulatedTime;

    public PlayTimeSystem(HyStats plugin) {
        this.plugin = plugin;
        this.accumulatedTime = new ConcurrentHashMap<>();
    }

    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> archetypeChunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> commandBuffer
    ) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        
        if (player != null && playerRef != null) {
            UUID playerUuid = playerRef.getUuid();
            double accumulated = accumulatedTime.getOrDefault(playerUuid, 0.0) + dt;

            if (accumulated >= 1.0) {
                long secondsToAdd = (long) accumulated;
                accumulatedTime.put(playerUuid, accumulated - secondsToAdd);
                
                this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "play_time", secondsToAdd);
            } else accumulatedTime.put(playerUuid, accumulated);
        }
    }

    @Nullable
    @Override
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @NonNullDecl
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
