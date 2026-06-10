# SkyRunMod

A Fabric **1.21.11** client mod for Hypixel Skyblock that applies speedrunning split mechanics to
granular in-game tasks — dungeon phases, room/secret clears, commissions and macro transitions — and
surfaces live, metric-driven feedback through a LiveSplit-style overlay. The goal: optimize routing,
minimize downtime, and quantify mechanical improvement.

## Features

**Live overlay (HUD).** A modular, positionable, scalable overlay shows the current activity, live
status lines, and a table of recent splits. Each split row shows its time, the signed delta versus
your previous personal best (green = ahead, red = behind, gold = first-ever best), and an optional
recency-weighted average.

**Dungeon (F7 / M7) splits.**
- Crystal carrier attribution from chat (`x picked up an energy crystal!`).
- Storm / Goldor phase timing.
- Goldor terminal/device/lever completion attribution + per-objective PBs (parses both `activated`
  and `completed` wording, with `(done/total)` progress).
- M7 dragon kill and relic pickup timing.

**Room & secret analytics.**
- Room clear timing bound to room entry/exit.
- Personal bests cached per room and per secret route.
- **Exponential Moving Average (EMA)** per room/route — an "average with recency bias" so recent
  improvements outweigh older, slower runs.

**Macro-economy & global metrics.**
- Commission PB splits (Dwarven Mines / Crystal Hollows).
- Transition timers (e.g. `/skyblock` load latency, dungeon ready-up).
- **Sum of best** across any key prefix (theoretical-best run).

**Persistence.** Personal bests and EMAs are saved to `config/skyrunmod/personal_bests.json` and
restored on launch; overlay settings live in `config/skyrunmod/settings.json`.

## Controls

Two rebindable keybinds (category **SkyRunMod**, unbound by default):
- **Toggle splits overlay**
- **Reset current run** (keeps personal bests)

Client command **`/skyrun`**:

| Command | Effect |
| --- | --- |
| `/skyrun reset` | Start a fresh run (keeps PBs) |
| `/skyrun save` | Write PBs + settings to disk |
| `/skyrun toggle` | Show/hide the overlay |
| `/skyrun pb <key>` | Look up a personal best |
| `/skyrun sob <prefix>` | Sum of best for all keys under a prefix |

## Architecture

The mod is split into a Minecraft-free **core** and a thin **client** layer so the analytics logic is
unit-testable on a bare JVM:

- `com.skyrunmod.core.SkyRunAnalyticsEngine` — split timing, PBs, EMA, sum-of-best, overlay state.
  No Minecraft types; covered by JUnit tests.
- `com.skyrunmod.util` — `TimeFormat` (LiveSplit-style clocks), `TextUtil` (chat cleanup),
  `MonotonicClock` (NTP-safe durations).
- `com.skyrunmod.config` — Gson-backed `SkyRunConfig` (settings) and `PersonalBestStore` (PB/EMA persistence).
- `com.skyrunmod.client` — Fabric entrypoint, chat listener, HUD overlay, keybinds and command.

## Building

Requires **JDK 21** (the Gradle build is pinned to it in `gradle.properties`).

```bash
./gradlew build        # compile, run tests, produce the remapped jar
./gradlew runClient    # launch a dev client with the mod loaded
```

The output mod jar lands in `build/libs/`. Tests run on a plain JVM (no Minecraft on the test
classpath) via `./gradlew test`.

## Notes on chat strings

Hypixel's exact chat wording occasionally changes between updates. Split-driving patterns live in
`SkyRunAnalyticsEngine` (crystal/terminal/device/lever/dragon/relic) and are easy to adjust in one
place; phase boundaries (Storm/Goldor start, commission start, transitions) are also exposed as
programmatic `startX`/`completeX` methods so future detectors (scoreboard, boss bar, location) can
drive them without re-parsing chat.
