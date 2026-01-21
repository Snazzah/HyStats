package com.snazzah.hystats.util;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.snazzah.hystats.CustomStatConfig;
import com.snazzah.hystats.HyStats;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;

public class StatisticsUI extends InteractiveCustomUIPage<StatisticsUI.EventData> {
    public static class EventData {
        public String action;
        public String tab;

        public static final BuilderCodec<EventData> CODEC = BuilderCodec.builder(EventData.class, EventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (EventData o, String v) -> o.action = v, (EventData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("Tab", Codec.STRING), (EventData o, String v) -> o.tab = v, (EventData o) -> o.tab)
                .add()
                .build();
    }

    private static final String TAB_GENERAL = "General";
    private static final String TAB_ITEMS = "Items";
    private static final String TAB_MOBS = "Mobs";
    private static final String ENVIRONMENT_PREFIX = "hystats__environment_damage_";
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,###");

    private static class ItemStatRow {
        private final String id;
        private long mined;
        private long placed;
        private long crafted;
        private long pickedUp;
        private long dropped;

        private ItemStatRow(String id) {
            this.id = id;
        }
    }

    private static class ItemDisplay {
        private final String sortKey;
        private final String fallbackName;
        @Nullable
        private final String translationKey;
        @Nullable
        private final String iconItemId;

        private ItemDisplay(String sortKey, String fallbackName, @Nullable String translationKey, @Nullable String iconItemId) {
            this.sortKey = sortKey;
            this.fallbackName = fallbackName;
            this.translationKey = translationKey;
            this.iconItemId = iconItemId;
        }
    }

    @Nonnull
    private String currentTab = TAB_GENERAL;

    public StatisticsUI(@Nonnull PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, EventData.CODEC);
    }

    @Override
    public void build(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull UICommandBuilder cmd,
            @Nonnull UIEventBuilder evt,
            @Nonnull Store<EntityStore> store
    ) {
        cmd.append("Pages/HyStats/StatisticsPage.ui");

        cmd.set("#CloseButton.Visible", true);
        evt.addEventBinding(CustomUIEventBindingType.Activating, "#CloseButton", new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "Close"), false);

        cmd.set("#GeneralTab.Disabled", TAB_GENERAL.equals(currentTab));
        cmd.set("#ItemsTab.Disabled", TAB_ITEMS.equals(currentTab));
        cmd.set("#MobsTab.Disabled", TAB_MOBS.equals(currentTab));

        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#GeneralTab",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "SwitchTab").append("Tab", TAB_GENERAL)
        );
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ItemsTab",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "SwitchTab").append("Tab", TAB_ITEMS)
        );
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MobsTab",
                new com.hypixel.hytale.server.core.ui.builder.EventData().append("Action", "SwitchTab").append("Tab", TAB_MOBS)
        );

        cmd.set("#EmptyLabel.Visible", false);
        cmd.set("#EmptyLabel.Text", "No statistics to display");

        UUID playerUuid = this.playerRef.getUuid();
        HyStats stats = HyStats.get();
        if (!stats.isPlayerStatsLoaded(playerUuid)) {
            cmd.set("#EmptyLabel.Text", "Statistics are still loading.");
            cmd.set("#EmptyLabel.Visible", true);
            return;
        }

        cmd.clear("#HyStatsList");
        int entryCount;
        switch (currentTab) {
            case TAB_GENERAL -> entryCount = buildGeneralTab(cmd, playerUuid, stats);
            case TAB_ITEMS -> entryCount = buildItemsTab(cmd, playerUuid, stats);
            case TAB_MOBS -> entryCount = buildMobsTab(cmd, playerUuid, stats);
            default -> entryCount = 0;
        }

        if (entryCount == 0) {
            cmd.set("#EmptyLabel.Text", "No statistics recorded yet.");
            cmd.set("#EmptyLabel.Visible", true);
        }
    }

    private int buildGeneralTab(UICommandBuilder cmd, UUID playerUuid, HyStats stats) {
        List<CustomStatConfig> customStats = new ArrayList<>(stats.getAllCustomStats().values());
        customStats.sort(Comparator.comparing(CustomStatConfig::displayName, String.CASE_INSENSITIVE_ORDER));

        int i = 0;
        for (CustomStatConfig config : customStats) {
            String value = stats.getFormattedStat(playerUuid, config.identifier());
            cmd.append("#HyStatsList", i % 2 == 0 ? "Pages/HyStats/GeneralStat.ui" : "Pages/HyStats/GeneralStatAlt.ui");
            cmd.set("#HyStatsList[" + i + "] #StatName.Text", config.displayName());
            cmd.set("#HyStatsList[" + i + "] #StatValue.Text", value);
            i++;
        }

        return i;
    }

    private int buildItemsTab(UICommandBuilder cmd, UUID playerUuid, HyStats stats) {
        Map<String, ItemStatRow> rows = new HashMap<>();
        mergeItemStats(rows, stats.getCategoryStats(playerUuid, "mined"), (row, value) -> row.mined = value);
        mergeItemStats(rows, stats.getCategoryStats(playerUuid, "placed"), (row, value) -> row.placed = value);
        mergeItemStats(rows, stats.getCategoryStats(playerUuid, "crafted"), (row, value) -> row.crafted = value);
        mergeItemStats(rows, stats.getCategoryStats(playerUuid, "picked_up"), (row, value) -> row.pickedUp = value);
        mergeItemStats(rows, stats.getCategoryStats(playerUuid, "dropped"), (row, value) -> row.dropped = value);

        if (rows.isEmpty()) return 0;

        Map<String, ItemDisplay> displayLookup = new HashMap<>();
        List<ItemStatRow> sortedRows = new ArrayList<>(rows.values());
        for (ItemStatRow row : sortedRows) displayLookup.put(row.id, resolveItemDisplay(row.id));
        sortedRows.sort(Comparator.comparing(row -> displayLookup.get(row.id).sortKey, String.CASE_INSENSITIVE_ORDER));

        int i = 0;
        for (ItemStatRow row : sortedRows) {
            ItemDisplay display = displayLookup.get(row.id);
            cmd.append("#HyStatsList", "Pages/HyStats/ItemEntry.ui");

            if (display.iconItemId != null)
                cmd.appendInline("#HyStatsList[" + i + "] #IconHolder", """
                    ItemIcon {
                        Anchor: (Width: 32, Height: 32);
                        ItemId: "%s";
                    }""".formatted(display.iconItemId));

            if (display.translationKey != null) {
                cmd.set("#HyStatsList[" + i + "] #ItemName.Text", Message.translation(display.translationKey));
            } else {
                cmd.set("#HyStatsList[" + i + "] #ItemName.Text", display.fallbackName);
            }

            cmd.set("#HyStatsList[" + i + "] #ItemDetails.Text", formatItemDetails(row));
            i++;
        }

        return i;
    }

    private int buildMobsTab(UICommandBuilder cmd, UUID playerUuid, HyStats stats) {
        Map<String, Long> killedStats = stats.getCategoryStats(playerUuid, "killed");
        Map<String, Long> killedByStats = stats.getCategoryStats(playerUuid, "killed_by");

        Set<String> mobIds = new HashSet<>();
        mobIds.addAll(killedStats.keySet());
        mobIds.addAll(killedByStats.keySet());
        if (mobIds.isEmpty()) return 0;

        int i = 0;
        for (String id : mobIds) {
            long killed = killedStats.getOrDefault(id, 0L);
            long killedBy = killedByStats.getOrDefault(id, 0L);
            var name = formatMobName(id);
            cmd.append("#HyStatsList", "Pages/HyStats/MobEntry.ui");
            cmd.set("#HyStatsList[" + i + "] #MobName.Text", name);
            cmd.set("#HyStatsList[" + i + "] #MobText.Text", "Killed " + formatNumber(killed) + ", died to " + formatNumber(killedBy) + " times.");
            i++;
        }

        return i;
    }

    private void mergeItemStats(Map<String, ItemStatRow> rows, Map<String, Long> stats, BiConsumer<ItemStatRow, Long> updater) {
        stats.forEach((id, value) -> {
            if (value <= 0L) {
                return;
            }
            ItemStatRow row = rows.computeIfAbsent(id, ItemStatRow::new);
            updater.accept(row, value);
        });
    }

    private ItemDisplay resolveItemDisplay(String id) {
        Item item = Item.getAssetMap().getAsset(id);
        if (item != null) {
            String translationKey = item.getTranslationKey();
            return new ItemDisplay(translationKey, item.getId(), translationKey, item.getId());
        }

        BlockType block = BlockType.getAssetMap().getAsset(id);
        if (block != null) {
            Item blockItem = block.getItem();
            if (blockItem != null) {
                String translationKey = blockItem.getTranslationKey();
                return new ItemDisplay(translationKey, blockItem.getId(), translationKey, blockItem.getId());
            }
        }

        return new ItemDisplay(id, id, null, null);
    }



    private String formatItemDetails(ItemStatRow row) {
        return "Mined: " + formatNumber(row.mined)
                + " • Placed: " + formatNumber(row.placed)
                + " • Crafted: " + formatNumber(row.crafted)
                + " • Picked Up: " + formatNumber(row.pickedUp)
                + " • Dropped: " + formatNumber(row.dropped);
    }

    private Message formatMobName(String id) {
        if (id == null || id.isBlank()) return Message.raw("Unknown");
        if (id.startsWith(ENVIRONMENT_PREFIX)) {
            String source = id.substring(ENVIRONMENT_PREFIX.length());
            return Message.raw("Environment (" + toTitleCase(source.replace('_', ' ')) + ")");
        }
        return Message.translation("server.npcRoles." + id + ".name");
    }

    private String toTitleCase(String raw) {
        String[] parts = raw.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!builder.isEmpty()) builder.append(' ');
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase());
            }
        }
        return builder.toString();
    }

    private String formatNumber(long value) {
        return NUMBER_FORMAT.format(value);
    }

    @Override
    public void handleDataEvent(
            @Nonnull Ref<EntityStore> ref,
            @Nonnull Store<EntityStore> store,
            @Nonnull EventData data
    ) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null && !"Close".equals(data.action)) {
            return;
        }

        switch (data.action) {
            case "Close":
                this.close();
                return;
            case "SwitchTab":
                if (data.tab != null) {
                    this.currentTab = data.tab;
                }
                this.rebuild();
                return;
            default:
                return;
        }
    }
}
