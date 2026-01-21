package com.snazzah.hystats.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class DamageSystem extends EntityEventSystem<EntityStore, Damage> {
    private final HyStats plugin;

    public DamageSystem(HyStats plugin) {
        super(Damage.class);
        this.plugin = plugin;
    }

    // This is when the player TAKES damage, not deals it.
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull Damage event) {
        if (!event.isCancelled()) {
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            Player player = store.getComponent(ref, Player.getComponentType());
            PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

            if (playerRef != null && player != null && player.getGameMode() != GameMode.Creative) {
                UUID playerUuid = playerRef.getUuid();
                this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "damage_taken", Math.round(event.getAmount()));
            }

            Damage.Source source = event.getSource();
            PlayerRef damagingPlayerRef = null;
            if (source instanceof Damage.EntitySource) {
                Ref<EntityStore> victimRef = ((Damage.EntitySource) source).getRef();
                if (victimRef.isValid()) {
                    Store<EntityStore> victimStore = victimRef.getStore();
                    damagingPlayerRef = victimStore.getComponent(victimRef, PlayerRef.getComponentType());
                }
            }

            if (damagingPlayerRef != null) {
                UUID victimUuid = damagingPlayerRef.getUuid();
                this.plugin.incrementStat(victimUuid, HyStats.CUSTOM_CATEGORY, "damage_dealt", Math.round(event.getAmount()));
            }
        }
    }

    @Nullable
    public Query<EntityStore> getQuery() {
        return Query.or(PlayerRef.getComponentType(), NPCEntity.getComponentType());
    }

    @NonNullDecl
    public Set<Dependency<EntityStore>> getDependencies() {
        return Collections.singleton(RootDependency.first());
    }
}
