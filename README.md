<div align="center">

![](https://get.snaz.in/AfQHrm8.png)

# HyStats
A mod/plugin that tracks player statistics, inspired by Minecraft's statistics system.

[![discord chat](https://img.shields.io/discord/311027228177727508?logo=discord&logoColor=white)](https://snaz.in/discord)

<br/>
</div>

![](https://get.snaz.in/7dH2gbc.png)
![](https://get.snaz.in/Acra1K4.png)
![](https://get.snaz.in/A3soQgh.png)

### Plugin API Usage

```java
// Register new custom stats
HyStats.registerCustomStat("things_done");

// Set/get stats
HyStats.incrementStat(playerUuid, "custom", "things_done", 1);
HyStats.setStat(playerUuid, "custom", "things_done", 100);
HyStats.resetStat(playerUuid, "custom", "things_done");

// Load offline stats
hyStats.loadOfflinePlayerStats(playerUuid).thenAccept(stats -> {
    long blocksMined = stats.get("killed", "Skeleton_Fighter");
    // TODO...
});
```
