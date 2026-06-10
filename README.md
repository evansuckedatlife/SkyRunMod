# SkyRunMod
Skyblock Mod for Fabric 1.21.11 that provides speedrun splits and personal bests across all aspects of the game, quantifying player skill.

## Current implementation

This repository now includes a minimal core analytics engine at:

- `/home/runner/work/SkyRunMod/SkyRunMod/evansuckedatlife/SkyRunMod/src/main/java/com/skyrunmod/core/SkyRunAnalyticsEngine.java`

Implemented feature coverage:

- Dungeon split parsing/tracking:
  - Crystal carrier parsing from chat (`x picked up an energy crystal!`)
  - Storm/Goldor/Dragon phase split timing
  - Goldor terminal completion attribution + PB tracking
  - M7 dragon kill + relic pickup timing
- Room analytics:
  - Room clear timing (`roomEnter`/`roomExit`)
  - PB tracking per room
  - Exponential moving average (EMA) per room and route
  - Route split timing
- Macro metrics:
  - Commission PB split tracking
  - Global transition timers (e.g. `/skyblock` latency, ready-up timing)
- UI state model:
  - Dynamic overlay sections that adapt to current activity and clear stale sections
