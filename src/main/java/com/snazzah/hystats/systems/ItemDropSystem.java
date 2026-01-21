package com.snazzah.hystats.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DropItemEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class ItemDropSystem extends EntityEventSystem<EntityStore, DropItemEvent.Drop> {
    private final HyStats plugin;

    public ItemDropSystem(HyStats plugin) {
        super(DropItemEvent.Drop.class);
        this.plugin = plugin;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull DropItemEvent.Drop event) {
        if (!event.isCancelled()) {
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
            if (player != null && playerRef != null) {
                UUID playerUuid = playerRef.getUuid();
                this.plugin.incrementStat(playerUuid, "dropped", event.getItemStack().getItemId(), event.getItemStack().getQuantity());
                this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "drops", 1);
            }
        }
    }

    @Nullable
    public Query<EntityStore> getQuery() {
        return PlayerRef.getComponentType();
    }

    @NonNullDecl
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
