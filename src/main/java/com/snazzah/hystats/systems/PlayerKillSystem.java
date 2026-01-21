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
import com.hypixel.hytale.server.core.modules.entity.damage.event.KillFeedEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.snazzah.hystats.HyStats;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public class PlayerKillSystem extends EntityEventSystem<EntityStore, KillFeedEvent.KillerMessage> {
    private final HyStats plugin;

    public PlayerKillSystem(HyStats plugin) {
        super(KillFeedEvent.KillerMessage.class);
        this.plugin = plugin;
    }

    // This runs when a player kills something
    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull KillFeedEvent.KillerMessage event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;
        
        UUID playerUuid = playerRef.getUuid();
        
        // Get target reference and store
        Ref<EntityStore> targetRef = event.getTargetRef();
        Store<EntityStore> targetStore = targetRef.getStore();

        PlayerRef victimPlayerRef = targetStore.getComponent(targetRef, PlayerRef.getComponentType());
        if (victimPlayerRef != null) {
            this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "player_kills", 1);
            this.plugin.incrementStat(playerUuid, "killed", "Player", 1);
            return;
        }

        if (NPCEntity.getComponentType() != null) {
            NPCEntity npcEntity = targetStore.getComponent(targetRef, NPCEntity.getComponentType());
            if (npcEntity != null) {
                String builderKey = npcEntity.getRoleName();
                if (builderKey == null || builderKey.isEmpty()) {
                    int spawnRoleIndex = npcEntity.getSpawnRoleIndex();
                    builderKey = NPCPlugin.get().getName(spawnRoleIndex);
                }

                if (builderKey != null && !builderKey.isEmpty()) {
                    String baseRole = this.plugin.resolveBaseNpcRoleId(builderKey);
                    this.plugin.incrementStat(playerUuid, "killed", baseRole, 1);
                }
            }
        }

        this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "mob_kills", 1);
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
