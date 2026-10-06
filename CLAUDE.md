# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StreamlineCore is a cross-platform Minecraft plugin framework that supports Velocity, BungeeCord, and Spigot (Bukkit) servers, plus Fabric, Forge and NeoForge servers on Minecraft 1.20.1, 1.21.1, 1.21.11, 26.2 and 26.3. It provides a modular architecture where features are delivered as PF4J-based modules that run identically across all supported platforms.

## Build System

**Gradle 9.8** with **Java 11** target compatibility for the core/platform projects (JitPack uses JDK 21). The mod-loader subprojects build only when Gradle itself runs on **Java 25** (Fabric Loom 1.18 requires it); on an older JVM, `settings.gradle` skips them with a warning.

```bash
# Build all modules (cleans, builds, deploys JARs to ./deploy/, and publishes)
./gradlew build

# Build a specific platform module
./gradlew :StreamlineCore-Velocity:shadowJar
./gradlew :StreamlineCore-Bungee:shadowJar
./gradlew :StreamlineCore-Spigot:shadowJar

# Build only the API (no platform modules)
./gradlew :StreamlineCore-API:shadowJar
./gradlew :StreamlineCore-Singularity:shadowJar
./gradlew :StreamlineCore-BAPI:shadowJar

# Clean build artifacts and deploy directory
./gradlew clean
```

- `build` automatically chains: `clean` -> `clearDeploys` -> `build` -> `publish`
- `shadowJar` automatically runs `deploy` (copies JAR to `./deploy/` or `$DEPLOY_DIR`)
- Output JARs are named `{ProjectName}-{version}.jar`
- In CI (Jenkins/JitPack), only `singularity-api`, `api`, and `backend-api` are built (platform modules skipped)

### Mod-loader subprojects (Fabric / Forge / NeoForge)

```powershell
$env:JAVA_HOME = 'C:\Users\nitra\.jdks\temurin-25.0.4.1'
.\gradlew.bat :StreamlineCore-NeoForge-1211:assemble   # one target
.\gradlew.bat assemble                                 # everything, mods included
```

- 15 projects, `<loader>/<loader>-<mc>` with mc in `1201, 1211, 12111, 262, 263`, named `StreamlineCore-<Loader>-<mc>`. Each `build.gradle` only sets versions in `ext` and applies `fabric/fabric.gradle`, `forge/forge.gradle` or `neoforge/neoforge.gradle`, which in turn apply `mod-common/mod.gradle`.
- Sources: `mod-common/` holds everything written against Mojang-mapped Minecraft (shared by all loaders); `mod-common/compat/{mc1201,mc1211,current}` holds the few calls whose shape changed between versions (1.20.x / 1.21.1 / 1.21.11+), and `mod-common/compat/menu-{clicktype,containerinput}` the menu click override (`ClickType` up to 1.21.11, `ContainerInput` from 26.1). NeoForge's block-break listener is split the same way (`neoforge/neoforge-break{121,26}`: `BlockEvent.BreakEvent` vs `BreakBlockEvent`). Per loader: `fabric/fabric-common`, `forge/forge-common` + `forge/forge-eventbus{6,7}` (Forge switched event buses during 1.21), `neoforge/neoforge-common`. NeoForge 1.20.1 is the pre-rename Forge fork, so it uses `neoforge/neoforge-1201` + the Forge EventBus 6 listener.
- Toolchains: Fabric Loom (`fabric-loom-remap` up to 1.21.11, `fabric-loom` for unobfuscated 26.x), ModDevGradle (NeoForge 1.21.1+; its `legacyforge` plugin for Forge and NeoForge 1.20.1, reobfuscated to SRG), ForgeGradle 7 (Forge 1.21.1+). Versions live in `gradle.properties` and the per-version `build.gradle` files.
- Mods shade `MOD_SHADED_LIBRARIES` (see `dependencies.gradle`), never what Minecraft already ships (Gson, Guava, Netty, SLF4J, commons-codec): Forge/NeoForge refuse two jars exporting one package.

## Project Structure & Module Dependencies

```
singularity-api  (Core cross-platform abstraction - package: singularity.*)
       ↑
      api         (Streamline API layer - package: net.streamline.api.*)
       ↑
  ┌────┼────┐
  │    │    │
velocity bungee  spigot ← (platform implementations - package: net.streamline.platform.*)
                   │
              backend-api  (Bukkit-specific utilities - package: net.streamline.apib.*)
```

Subproject names in Gradle are remapped:
- `singularity-api` → `StreamlineCore-Singularity`
- `api` → `StreamlineCore-API`
- `backend-api` → `StreamlineCore-BAPI`

## Architecture

### Singularity Abstraction Layer

The core abstraction is `Singularity<C, P, S, U, M>` (in `singularity-api`), parameterized by:
- `C` = CommandSender type
- `P extends C` = Player type
- `S extends ISingularityExtension` = Platform plugin implementation
- `U extends IUserManager<C, P>` = User management
- `M extends IMessenger` = Messaging

`SLAPI` (in `api`) extends `Singularity` with LuckPerms, PlaceholderAPI, and timer integrations.

### Key Interfaces (in `singularity.interfaces`)

- **`ISingularityExtension`** — Contract each platform plugin must implement. Defines `PlatformType` (VELOCITY, BUNGEE, SPIGOT) and `ServerType` (PROXY, BACKEND). Methods for event firing, player listing, server info, resource packs.
- **`IUserManager<C, P>`** — Player/sender creation, connection, kicks, teleportation.
- **`IMessenger`** — Platform-agnostic text messaging (sendMessage, sendTitle, color codes).
- **`IModuleLike`** — Module lifecycle interface.

### User Hierarchy

`CosmicSender` (base) → `CosmicPlayer` (extends with IP, location, server). Both use the `Loadable<L>` interface for async database persistence via `CompletableFuture`.

### Module System (PF4J)

Modules extend `CosmicModule` (which extends `org.pf4j.Plugin`). `ModuleManager` uses `JarPluginManager` to scan and load JARs from the `modules/` folder. Each module registers commands via `ModuleCommand` and fires `ModuleLoadEvent`/`ModuleEnableEvent`/`ModuleDisableEvent`.

### Command System

`CosmicCommand` (cross-platform) is wrapped by platform-specific `ProperCommand` adapters in each platform module. Commands are registered through `CommandHandler`.

### Event System

Two layers: platform-native events (Velocity `@Subscribe`, Bungee `Listener`, Bukkit `Listener`) and cross-platform `CosmicEvent` hierarchy (player lifecycle, module lifecycle, server events, commands).

`singularity.events.player.gameplay` (`PlayerBrokeBlockEvent`, `PlayerPlacedBlockEvent`, `PlayerKilledEntityEvent`, `PlayerCaughtFishEvent`, `PlayerFilledBucketEvent`) report actions that already went through, with lowercase namespaced ids. Spigot fires them from `GameplayEventsListener` at MONITOR; the mod loaders call `mod-common`'s `GameplayEvents` from their listeners. Gaps: Fabric API has no placement, fishing, bucket or death-drops hook (kills fire with no drops); Forge/NeoForge have no bucket-fill hook.

`singularity.events.entity` (`CosmicExplosionEvent`, `CosmicMobGriefEvent`, `CosmicEntityDamagedByEntityEvent`) fire *before* a non-player entity changes the world, so listeners can prevent it (turn off block damage / cancel). Spigot fires them from `EntityEventsListener` at HIGH; NeoForge (`NeoForgeListener`) and Forge EventBus 6 (`forge-eventbus6`, so also NeoForge 1.20.1) call `mod-common`'s `EntityEvents` from `ExplosionEvent.Detonate`, `EntityMobGriefingEvent` and `LivingIncomingDamageEvent`/`LivingAttackEvent`. Every platform checks `CosmicEntityEvent.hasListeners(...)` first, because the grief check runs per mob per tick. Not fired on Fabric or Forge EventBus 7 (1.21.11+). TheBase dispatches by exact class, so listen to the concrete events. `mod-common/compat/enderman-{upto262,263}` covers Mojang's `EnderMan` → `Enderman` rename in 26.3.

### GUI System (`singularity.gui`)

`CosmicGui` (title, rows, slot → `GuiIcon` of `CosmicItem` + click handler; override `draw(viewer)` to lay out per open/redraw) and `PaginatedGui` are platform-agnostic. `GuiManager` opens them through the platform's `IGuiHandler`: `SpigotGuiHandler` (BOU `ScreenInstance`) on Spigot, `ModGuiHandler` (vanilla `ChestMenu`) on the mod loaders. A proxy has no handler: it sends the render-only `GuiView` (gzip + base64url, kept under the 32767-byte serverbound payload cap) to the player's backend over `gui-*` `ProxiedMessage`s (`singularity.gui.transport.GuiMessages`); the backend reports clicks and closes back and the handlers run on the proxy. Mod backends have no proxy messenger, so proxy-sent GUIs reach Spigot backends only. Clicks and closes carry the GUI instance id and are ignored unless it is the viewer's current one.

### Platform Entry Points

Each platform has `net.streamline.platform.BasePlugin`:
- **Velocity**: Annotation-based plugin, uses `@Subscribe` for `ProxyInitializeEvent`/`ProxyShutdownEvent`
- **BungeeCord**: Extends `net.md_5.bungee.api.plugin.Plugin` with `onLoad()`/`onEnable()`
- **Spigot**: Extends `BetterPlugin` (from BukkitOfUtils) with `onBaseConstruct()`/`onBaseEnabled()`

### Initialization Sequence

1. Platform plugin loads → `BasePlugin.load()`
2. Platform enable → creates `SLAPI` instance with platform-specific `UserManager`, `Messenger`, `ConsoleHolder`, `PlayerInterface`
3. Registers platform listener, starts `TaskManager`
4. `ModuleManager` scans and loads modules from `modules/` folder
5. LuckPerms/PlaceholderAPI integrations initialize if present

## Key Dependencies

- **PF4J 3.10.0** — Module/plugin framework
- **Lettuce 6.8.0** — Redis client for cross-server sync
- **Caffeine 3.1.8** — Caching
- **TheBase 1.1.0** — Event system and utilities (`gg.drak:TheBase`)
- **Logback 1.5.6** — Logging (relocated to `host.plas.bou.libs.logback`)
- **LuckPerms API 5.5** — Optional permission backend. Only `LuckPermsPermissionProvider` (and `LuckPermsUtil`) may link against it; everything else goes through `net.streamline.api.permissions.Permissions`, which falls back to `NoPermissionProvider` when LuckPerms is absent. A class that imports `net.luckperms.*` on a common path crashes servers without LuckPerms with `NoClassDefFoundError`.
- **PlaceholderAPI 2.11.6** — Placeholder expansion (Spigot only)

## Conventions

- Uses **Lombok** across all modules (applied via `io.freefair.lombok` plugin)
- Shadow JAR relocations: `com.github.Anon8281.universalScheduler` → `host.plas.bou.libs.universalScheduler`, `ch.qos.logback` → `host.plas.bou.libs.logback`
- Group ID: `gg.drak`, root package varies by layer (`singularity.*`, `net.streamline.api.*`, `net.streamline.platform.*`)
- Resource templates use Gradle `expand()` for `plugin.yml`, `velocity-plugin.json`, `streamline.properties`, `singularity.properties`, `fabric.mod.json`, `mods.toml`
- Publishing target: `https://repo.codemc.io/repository/streamline-essentials/`
