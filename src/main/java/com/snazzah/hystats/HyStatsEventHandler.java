package com.snazzah.hystats;

import com.hypixel.hytale.event.EventPriority;
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.snazzah.hystats.util.StatisticsManager;

import java.util.UUID;
import java.util.logging.Level;

public class HyStatsEventHandler {
    public void register(HyStats plugin) {
        plugin.getEventRegistry().registerGlobal(EventPriority.LATE, PlayerConnectEvent.class, this::onPlayerConnect);
        plugin.getEventRegistry().registerGlobal(EventPriority.LATE, PlayerDisconnectEvent.class, this::onPlayerDisconnect);
        plugin.getEventRegistry().registerGlobal(EventPriority.LATE, PlayerChatEvent.class, this::onPlayerChat);
    }

    private void onPlayerConnect(PlayerConnectEvent event) {
        UUID playerUuid = event.getPlayerRef().getUuid();

        StatisticsManager.getInstance().loadPlayerStats(playerUuid).thenAccept(stats -> {
            HyStats.get().incrementStat(playerUuid, "custom", "times_connected", 1);
        }).exceptionally(ex -> {
            HyStats.get().getLogger().at(Level.SEVERE)
                .log("Failed to load stats for connecting player " + playerUuid + ": " + ex.getMessage());
            return null;
        });
    }

    private void onPlayerDisconnect(PlayerDisconnectEvent event) {
        UUID playerUuid = event.getPlayerRef().getUuid();

        if (HyStats.get().getMovementSystem() != null) HyStats.get().getMovementSystem().cleanupPlayer(playerUuid);

        StatisticsManager.getInstance().unloadPlayerStats(playerUuid).exceptionally(ex -> {
            HyStats.get().getLogger().at(Level.SEVERE)
                    .log("Failed to unload stats for disconnecting player " + playerUuid + ": " + ex.getMessage());
            return null;
        });
    }

    private void onPlayerChat(PlayerChatEvent event) {
        if (event.isCancelled()) return;
        UUID playerUuid = event.getSender().getUuid();
        HyStats.get().incrementStat(playerUuid, "custom", "messages_sent", 1);
    }
}
