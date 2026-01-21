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
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
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

public class PlayerKilledSystem extends EntityEventSystem<EntityStore, KillFeedEvent.DecedentMessage> {
    private final HyStats plugin;

    public PlayerKilledSystem(HyStats plugin) {
        super(KillFeedEvent.DecedentMessage.class);
        this.plugin = plugin;
    }

    public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull KillFeedEvent.DecedentMessage event) {
        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
        Player player = store.getComponent(ref, Player.getComponentType());
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());

        if (player == null || playerRef == null) return;
        
        UUID playerUuid = playerRef.getUuid();
        Damage.Source source = event.getDamage().getSource();
        this.plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "deaths", 1);

        if (source instanceof Damage.EntitySource entitySource) {
            Ref<EntityStore> killerRef = entitySource.getRef();
            Store<EntityStore> killerStore = killerRef.getStore();

            PlayerRef killerPlayerRef = killerStore.getComponent(killerRef, PlayerRef.getComponentType());
            if (killerPlayerRef != null) {
                this.plugin.incrementStat(playerUuid, "killed_by", "Player", 1);
                return;
            }

            if (NPCEntity.getComponentType() != null) {
                NPCEntity killerNpc = killerStore.getComponent(killerRef, NPCEntity.getComponentType());
                if (killerNpc != null) {
                    String builderKey = killerNpc.getRoleName();

                    if (builderKey == null || builderKey.isEmpty()) {
                        int spawnRoleIndex = killerNpc.getSpawnRoleIndex();
                        builderKey = NPCPlugin.get().getName(spawnRoleIndex);
                    }

                    if (builderKey != null && !builderKey.isEmpty()) {
                        String baseRole = this.plugin.resolveBaseNpcRoleId(builderKey);
                        this.plugin.incrementStat(playerUuid, "killed_by", baseRole, 1);
                    }
                }
            }
        } else if (source instanceof Damage.EnvironmentSource)
            // type can be explosion, thats about the only type rn
            this.plugin.incrementStat(playerUuid, "killed_by", "hystats__environment_damage_" + ((Damage.EnvironmentSource) source).getType(), 1);
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
