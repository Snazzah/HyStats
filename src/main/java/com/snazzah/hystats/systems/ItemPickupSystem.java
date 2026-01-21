package com.snazzah.hystats.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.HolderSystem;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.HyStats;

import javax.annotation.Nonnull;
import java.util.UUID;

public class ItemPickupSystem extends HolderSystem<EntityStore> {
    private final HyStats plugin;

    public ItemPickupSystem(HyStats plugin) {
        this.plugin = plugin;
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return PickupItemComponent.getComponentType();
    }

    @Override
    public void onEntityAdd(@Nonnull Holder<EntityStore> holder, @Nonnull AddReason reason, @Nonnull Store<EntityStore> store) {
        if (reason != AddReason.SPAWN) return;

        PickupItemComponent pickupComponent = holder.getComponent(PickupItemComponent.getComponentType());
        ItemComponent itemComponent = holder.getComponent(ItemComponent.getComponentType());
        if (pickupComponent == null || itemComponent == null) return;

        Ref<EntityStore> targetRef = pickupComponent.getTargetRef();
        if (targetRef == null || !targetRef.isValid()) return;

        PlayerRef playerRef = store.getComponent(targetRef, PlayerRef.getComponentType());
        if (playerRef == null) return;

        UUID playerUuid = playerRef.getUuid();
        ItemStack itemStack = itemComponent.getItemStack();

        if (itemStack != null && !ItemStack.isEmpty(itemStack))
            plugin.incrementStat(playerUuid, "picked_up", itemStack.getItemId(), itemStack.getQuantity());
    }

    @Override
    public void onEntityRemoved(@Nonnull Holder<EntityStore> holder, @Nonnull RemoveReason reason, @Nonnull Store<EntityStore> store) {}
}
