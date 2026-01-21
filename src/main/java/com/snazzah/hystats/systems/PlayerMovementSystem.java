package com.snazzah.hystats.systems;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.RootDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
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

public class PlayerMovementSystem extends EntityTickingSystem<EntityStore> {
    private final HyStats plugin;

    private final Map<UUID, Vector3d> lastPositions = new ConcurrentHashMap<>();
    private final Map<UUID, Double> accumulatedDistance = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasSleeping = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> wasJumping = new ConcurrentHashMap<>();

    private static final double TELEPORT_THRESHOLD = 100.0;
    private static final double MIN_DISTANCE = 0.0001; // ~0.01 cm
    
    public PlayerMovementSystem(HyStats plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void tick(
            float dt,
            int index,
            @Nonnull ArchetypeChunk<EntityStore> chunk,
            @Nonnull Store<EntityStore> store,
            @Nonnull CommandBuffer<EntityStore> buffer
    ) {
        Ref<EntityStore> ref = chunk.getReferenceTo(index);
        PlayerRef playerRef = store.getComponent(ref, PlayerRef.getComponentType());
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        MovementStatesComponent movementComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        if (playerRef == null || transform == null || movementComponent == null) return;
        
        UUID playerUuid = playerRef.getUuid();
        Vector3d currentPos = transform.getPosition();
        MovementStates states = movementComponent.getMovementStates();

        trackMovementDistance(playerUuid, currentPos, states);
        trackStateSwitches(playerUuid, states);

        lastPositions.put(playerUuid, new Vector3d(currentPos));
    }
    
    private void trackMovementDistance(UUID playerUuid, Vector3d currentPos, MovementStates states) {
        Vector3d lastPos = lastPositions.get(playerUuid);
        if (lastPos == null) return;

        double distanceBlocks = lastPos.distanceTo(currentPos);
        if (distanceBlocks < MIN_DISTANCE || distanceBlocks > TELEPORT_THRESHOLD) return;

        double distanceCm = distanceBlocks * 100.0;
        double accumulated = accumulatedDistance.getOrDefault(playerUuid, 0.0) + distanceCm;

        if (accumulated >= 1.0) {
            long cmToAdd = (long) accumulated;
            accumulatedDistance.put(playerUuid, accumulated - cmToAdd);
            
            if (states.climbing) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_climb", cmToAdd);
            } else if (states.swimming) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_swim", cmToAdd);
            } else if (states.flying) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_fly", cmToAdd);
            } else if (states.gliding) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_glide", cmToAdd);
            } else if (states.falling && !states.onGround) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_fall", cmToAdd);
            } else if (states.sprinting) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_sprint", cmToAdd);
            } else if (states.crouching && !states.inFluid) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_sneak", cmToAdd);
            } else if (states.walking || states.running) {
                plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "distance_walk", cmToAdd);
            }
        } else accumulatedDistance.put(playerUuid, accumulated);
    }
    
    private void trackStateSwitches(UUID playerUuid, MovementStates states) {
        Boolean wasSleeping = this.wasSleeping.get(playerUuid);
        if (states.sleeping && (wasSleeping == null || !wasSleeping))
            plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "times_slept", 1);
        this.wasSleeping.put(playerUuid, states.sleeping);

        Boolean wasJumping = this.wasJumping.get(playerUuid);
        if (states.jumping && (wasJumping == null || !wasJumping))
            plugin.incrementStat(playerUuid, HyStats.CUSTOM_CATEGORY, "jumps", 1);
        this.wasJumping.put(playerUuid, states.jumping);
    }

    public void cleanupPlayer(UUID playerUuid) {
        lastPositions.remove(playerUuid);
        accumulatedDistance.remove(playerUuid);
        wasSleeping.remove(playerUuid);
        wasJumping.remove(playerUuid);
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
