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
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class CraftSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {
    private final HyStats plugin;

    public CraftSystem(HyStats plugin) {
        super(CraftRecipeEvent.Post.class);
        this.plugin = plugin;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull CraftRecipeEvent.Post event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = (Player)store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = (PlayerRef)store.getComponent(ref, PlayerRef.getComponentType());
        if (player != null && playerRef != null) {
            UUID playerUuid = playerRef.getUuid();
            var itemId = event.getCraftedRecipe().getPrimaryOutput().getItemId();
            if (itemId != null) {
                long quantity = (long) event.getCraftedRecipe().getPrimaryOutput().getQuantity() * event.getQuantity();
                this.plugin.incrementStat(playerUuid, "crafted", itemId, quantity);
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
