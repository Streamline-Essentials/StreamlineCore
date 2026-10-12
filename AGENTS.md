# AGENTS.md

> **Note:** This file was written specifically for Claude (Claude Code), so
> some wording may not fit other agents exactly — e.g. Claude-specific tool
> names, skills, or features. Interpret those in terms of your own equivalents.
>
> The master copy of the global rules these instructions build on lives at
> `{USER HOME}/.claude/CLAUDE.md` (e.g. `C:\Users\<user>\.claude\CLAUDE.md`).
> Read it as well; rules there apply unless this file overrides them.

This file provides guidance to coding agents working with code in this repository.

## Project Overview

StreamlineCore is a cross-platform Minecraft plugin framework that supports Velocity, BungeeCord, and Spigot (Bukkit) servers, plus Fabric, Forge and NeoForge servers and clients on Minecraft 1.20.1, 1.21.1, 1.21.11, 26.2 and 26.3. It provides a modular architecture where features are delivered as PF4J-based modules that run identically across all supported platforms.

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

- **Build output lives outside OneDrive.** OneDrive turns synced folders into read-only cloud placeholders that Gradle cannot delete ("Unable to delete directory" / `AccessDeniedException`), so when the checkout sits inside OneDrive, `build.gradle` points every project's `layout.buildDirectory` at `%LOCALAPPDATA%\StreamlineCore\build\<folder>-<hash>\<project path>`. Override with the `streamline.buildRoot` Gradle property or `STREAMLINE_BUILD_ROOT` (`project` keeps the usual `build/` folders). Never hard-code `build/` paths; use `layout.buildDirectory`. `clean-build.ps1` also clears the redirected root.
- **Dev runs.** Every mod project defines `client` and `server` runs (`runClient` / `runServer`), declared by Fabric Loom per subproject and in `neoforge.gradle` / `forge.gradle` for ModDevGradle and ForgeGradle 7. IntelliJ run configurations are Gradle-task configs in `.idea/runConfigurations/`, named `Minecraft Client (<project path>)` / `Minecraft Server (...)`, written on every IntelliJ sync: Loom writes the Fabric ones, the root `build.gradle` the Forge, NeoForge and Forge 1.8.9 (client only) ones. ForgeGradle 7 generates none for IntelliJ, and ModDevGradle's own are turned off (`disableIdeRun()`) so each project has one pair. Runs load the mod from its classes, so `mod-common/mod.gradle` builds `devLibrariesJar` (the bundled libraries, unrelocated and filtered like the shipped jar) and adds it to `runtimeOnly`, to ModDevGradle's `additionalRuntimeClasspath` up to 1.21.1 (MDG rejects it from 1.21.9), and to ForgeGradle 7's `extraLibraries`. ForgeGradle 7 runs need classes and resources in one output directory, which `forge.gradle` sets.
- `build` automatically chains: `clean` -> `clearDeploys` -> `build` -> `publish`
- `shadowJar` automatically runs `deploy` (copies JAR to `./deploy/` or `$DEPLOY_DIR`)
- Output JARs are named `{ProjectName}-{version}.jar`
- In CI (Jenkins/JitPack), only `module-core`, `singularity-api`, `api`, and `backend-api` are built (platform modules skipped)

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
- Mods load on dedicated servers **and clients** (Fabric `environment: "*"` with `main` + `client` entrypoints; Forge/NeoForge `side="BOTH"`). `BasePlugin.isClient()` is set by each loader before `initialize()`. A game client never runs SLAPI on its own: the client-started hook (`ModEvents.onClientStarted`: Fabric `CLIENT_STARTED`, Forge/NeoForge first client tick) calls `BasePlugin.bootClient()`, which only sets `ClientEnvironment` (folder `mods/<name>/client/`, logger) and starts `ClientModuleManager`, so client modules run on any server or none with no users, permissions, database or commands. The full `boot()` (SLAPI, users, database, server modules) runs on a dedicated server, and on a client only from `onServerEnable` once it hosts a world, for that integrated server. `BasePlugin.getSupportedPlatformTags()` is always `{modded-server}`: `ModuleManager` loads server modules only, and client modules are `ClientModuleManager`'s. World modules (see below) are server modules too, so a hosting client's `ModuleManager` loads them from `mods/<name>/modules/`; `bootClient()` turns them on (`WorldModules.setSupported(true)`) and `boot()` on a client sets `WorldModules.setHost(new WorldModuleHost(this))`, which loads/unloads them through `ModuleManager` on the server thread while Streamline runs. Stopping an integrated server keeps everything up (`CommandRegistry.forgetDispatcher()` drops the old dispatcher's displaced nodes); the full teardown, client modules included, runs when the game exits (`ModEvents.onGameStopping`, order-independent with the integrated server stop).
- Client modules (`module-core`, `singularity.client.module`): jars in `mods/<name>/client/modules/` whose `Plugin-Platforms` include `modded-client` but not `modded-server` (`WorldModules.isClientModule`) and whose main class extends `ClientModule` (`onLoad`/`onEnable`/`onDisable`, `getDataFolder()` = `client/module-data/<id>/`). `ClientModuleManager` loads each from a private copy in `client/.module-cache/`, so the real jar stays free to replace or delete on Windows, and polls the folder every 2 s: added jars load, replaced jars reload (with their dependents), deleted jars unload. All operations (`enable`/`disable`/`reload`/`remove`/`install`) run on one daemon thread; refused jars are kept with a reason (`refusals()`) and retried when they change or another module loads. Unloading goes through `ClientPluginManager.forget`, never PF4J's `unloadPlugin`, which instantiates the plugin to stop it and leaves a jar whose main class does not link registered. Turned-off modules are remembered in `client/modules.properties`. Client-only jars found in the server `modules/` folder are moved over at start; world-module jars dropped in `client/modules/` are moved to `modules/` (`WorldModules.adopt`), or refused where world modules are unsupported. World modules (`WorldModules`): jars tagged both `modded-client` and `modded-server`, i.e. server modules a client runs on the integrated server of worlds it hosts (singleplayer/LAN); they live in Streamline's server `modules/` folder beside `client/`, `WorldModules.installed()` lists them by `Plugin-Id`, and `install`/`remove` go through the `WorldModules.Host` while Streamline runs, otherwise change only the jar for the next world. `ClientModuleCloud` lists, downloads and updates eCloud client modules, plus world modules where `WorldModules.isSupported()` (`WorldModules.installable`, checked on registry and jar platforms; `Result.isWorld()` marks them); anything else is refused. `singularity.registry.ModuleRegistry` is the registry HTTP client shared with the server's `ModuleCloud`.
- Client command `/streamlineclient (menu|hud|modules (...)|<command> (args ...))` (alias `/slclient`, `mod-common` `ClientCommand`): `menu` / `hud` open the module menu / HUD editor through `ClientBridge` on the client thread; `modules (list|enable <module>|disable <module>|reload <module>|remove <module>|ecloud (list|info <module>|download <module> (version)|update <module>))` (`ClientModulesCommand`) manages client modules anywhere (`list` adds a World modules section, `remove` falls back to a world module of that id, `ecloud list` tags world modules `[world]`, and installs/updates of world modules go to `modules/`, starting at once while a world is hosted or with the next one); anything else runs a Streamline command on the hosted integrated server as the host's `ServerPlayer`, and on a remote server just explains that.
- Client UI (`client-common/`, package `net.streamline.client`, compiled into every mod by `mod-common/mod.gradle`): `StreamlineClient.init()` installs the `ClientBridge` handler (before boot, so modules always find it); `HudOverlay` draws `HudRegistry` elements; `gui/MenuScreen` (OneConfig-style menu of `ClientModuleManager` modules; its first sidebar entry, Client Settings, is `singularity.client.settings.StreamlineClientSettings` (StreamlineClient's own settings, no switch, saved in `client/client-settings.properties`, never in `ClientSettingsRegistry`: Show HUD, Remember Last Page, Check eCloud For Updates, chroma type/cycle/saturation/brightness) with the core placeholders listed below them; then an enable switch left of each name in the sidebar; each module page opens with one line, `<name> <version> by <author>` plus Details (a popup with the description, id and file), Reload, Update and Remove, `ClientSettingsRegistry` settings, HUD element toggles, an Edit button per element whose `getTemplate()` is non-null (raw text field via `Canvas.rawText`, live preview, click-to-insert placeholders, Reset, Delete for custom ones) and a + form for modules with `CustomHudElements`; its last sidebar entry browses eCloud client modules, and world modules labelled as such where supported, with install/update buttons) and `gui/HudEditorScreen` (drag, scroll to scale, right-click hide, R reset; click selects, arrows nudge 4px or 1px with Ctrl, Delete removes (`HudPlacement.remove()`: hidden and out of the editor until the menu's toggle calls `show()`; saved as a fifth `removed` field in `hud-layout.properties`), holding left Shift (`ClientCompat.leftShiftDown()`) hides the menu button and hidden elements; a drag follows the pointer each frame while `ClientCompat.leftMouseDown()` holds, from GLFW, LWJGL 2, or 26.x `MouseHandler.activeButton` since 26.2 is GLFW and 26.3 SDL, because `MouseHandler.isLeftPressed()` is only tracked with no screen open and drag events can be taken by other mods). Screens open a frame later (`ClientCompat.later`) because `Minecraft.execute` runs inline on the render thread and the chat screen closes after the command. Version differences live in `client-common/compat/gfx{1201,1211,12111,26}` (`GfxCanvas`, `CompatScreen`, `ClientCompat`): `PoseStack` vs `Matrix3x2fStack`, int input vs `MouseButtonEvent`/`KeyEvent`/`CharacterEvent` from 1.21.11, 26.3's SDL key codes and modifiers translated to GLFW ones in gfx26 `CompatScreen` (shared screens only ever see GLFW codes), and 26.x `GuiGraphicsExtractor` (`text(...)`, `extractRenderState`). HUD hooks: Fabric `fabric-hud-callback` (`HudRenderCallback`, to 1.21.1) / `fabric-hud-elements` (`HudElementRegistry`, 1.21.11+); Forge `forge-hud1201` (`RenderGuiEvent.Post`, also NeoForge 1.20.1), `forge-hud1211` (`AddGuiOverlayLayersEvent` on the mod bus), `forge-hud-eb7` (`AddGuiOverlayLayersEvent.BUS`); NeoForge 1.21+ `RenderGuiEvent.Post` in `NeoForgeClientListener`. Key mappings (`client-common/modern` `Keybinds`, built by each gfx era's `ClientKeys`: a String category to 1.21.1, a registered `KeyMapping.Category` from 1.21.11; names in `mod-common` `assets/streamlinecore/lang/en_us.json`): HUD Editor (Right Shift, `InputConstants.KEY_RSHIFT`, a GLFW code to 26.2 and SDL from 26.3) and StreamlineClient Menu (unbound), under StreamlineClient in Controls; `Keybinds.tick()` runs every client tick and opens the screen only with none open. Registered by Fabric `fabric-keys-binding` (`KeyBindingHelper`, to 1.21.11) / `fabric-keys-mapping` (`KeyMappingHelper`, 26.x), Forge `RegisterKeyMappingsEvent` (mod bus on EventBus 6, `.BUS` on 7), NeoForge `RegisterKeyMappingsEvent` on the mod bus. Client-only code is reached only behind a dist check; nothing loaded on both sides may reference `net.minecraft.client` or `net.streamline.client`.
- Client module API (`module-core`, package `singularity.client`, no Minecraft types): `ClientBridge` (`isClient`, `info()` → `ClientInfo`, `openMenu`, `openHudEditor`), `hud.HudRegistry.register(moduleId, HudElement)` (`TextHudElement` for lines of `&`-coded text; placements saved in `client/hud-layout.properties`; an element overriding `getTemplate`/`setTemplate`/`getDefaultTemplate`/`getPlaceholders`/`isCustom` gets the menu's text editor, and these have defaults so such a module still loads on older cores), `hud.HudRegistry.setCustomElements(moduleId, CustomHudElements)` (the menu's + and Delete call its `add(id, template)`/`remove(id)` on the render thread; cleared with the module's elements; call it from a class only loaded inside `catch (LinkageError)` to support older cores, as StreamlineHUD's `CustomElements` does), `settings.ClientSettingsRegistry.register(new ModuleSettings(...))` with `Setting.Toggle/Slider/Choice/Text/Color` (setting names in Title Case, e.g. "Main Color") (saved in `client/settings/<module-id>.properties`). `placeholders.ClientPlaceholders`: the core's `%streamlineclient_<key>%` placeholders (`get(key)`, `apply(text)`, `descriptions()`), currently `colors_chroma` (the Client Settings default type) and `colors_chroma_<static|breathing|pulsing>`, each a `{#rrggbb}` code from `color.Chroma` that changes over time; StreamlineHUD resolves them through `ClientValues` behind `catch (LinkageError)` for older cores. Files resolve through `ClientEnvironment`, which is unset off a client, so nothing is written there. `ClientModule.stop()` calls `ClientBridge.forgetModule`, so a stopped module leaves nothing on the render thread; a module must also drop its own static state in `onDisable`, since it can be unloaded while the game runs. `modules/StreamlineHUD` is the reference client module.
- Client module keys and held game keys (`module-core`): `settings.Setting.Key` is a setting the player binds on the module's menu page (click it, press a key; Escape cancels, right-click unbinds), saved as Minecraft's version-independent key name (`key.keyboard.g`); its `onPress` runs on the client thread when the key goes down with no screen open. Modules load after the game hands out its key mappings, so module keys never appear in Controls: `client-common` `ModuleKeys.tick()` (from `Keybinds.tick()`, and `LegacyClientListener` on 1.8.9) reads them from the window through `ClientCompat.keyDown`, and the menu captures them through `CompatScreen.keyNamed`. `input.ClientInput.hold(moduleId, GameKey.SPRINT|SNEAK, held)` keeps a game key pressed (`ClientCompat.holdGameKey`, pressed again every tick, so it lasts through screens, deaths and respawns); `ClientBridge.forgetModule` lets go of a stopped module's keys. `modules/ClientTogglesModule` (toggle sprint / toggle sneak) uses both.

### Forge 1.8.9 (game clients only)

```powershell
$env:JAVA_HOME = 'C:\Users\nitra\.jdks\temurin-25.0.4.1'
.\gradlew.bat :StreamlineCore-Forge-189:remapJar          # jar to ./deploy
.\gradlew.bat -p forge/forge-189 runClient               # dev client (Java 8 toolchain)
```

- `forge/forge-189` is a **separate Gradle build**, not a subproject: Essential's Loom fork (`gg.essential.loom`) shares packages with Fabric Loom on the root buildscript classpath. The two builds include each other (`includeBuild`), so the API project substitutes for the published artifact and root tasks reach `:StreamlineCore-Forge-189:*`; the root includes it only when it builds mods (Java 25). Its build output follows the same OneDrive redirect.
- Client-only and server-less: it runs the lean module core only, never SLAPI (no users, database, permissions or server modules). `BasePlugin.boot()` (first client tick) sets `ClientEnvironment` (`mods/<name>/client/`) and starts `ClientModuleManager`; the JVM shutdown hook calls `ClientModuleManager.shutdown()`. It runs client modules, the HUD, the module menu (eCloud browser included), the HUD Editor / StreamlineClient Menu key bindings (`LegacyClientListener`, `ClientRegistry`, `assets/streamlinecore/lang/en_US.lang`) and `/streamlineclient (menu|hud|<command> (args ...))`; other commands only explain that no server is hosted. No world modules: it hosts no Streamline server, so `WorldModules.isSupported()` stays false, eCloud does not offer them and world-module jars in `client/modules/` are refused. Its own classes live in `forge/forge-189/src` (`BasePlugin`, `ClientCommand`, `LegacyClientListener`, `LegacyModuleTransformer`), not in `mod-common` (Mojang-mapped, modern Minecraft). The UI is the shared `client-common` plus `client-common/compat/gfx189` (LWJGL 2 key codes mapped to GLFW, `§` codes only: hex colors become the nearest legacy color).
- **Java 8.** 1.8.9 runs on Java 8, the core and modules are Java 11 bytecode. `bundleLibraries` shades the API shadow jar, TheBase and the libraries, relocated under `net.streamline.libs` (also Gson, SLF4J and ASM, which the game ships older); `downgradeLibraries`/`libraryJar` rewrite that to Java 8 with JvmDowngrader and shade its API stubs. Caffeine is 2.9.3 there (3.x needs `Lookup.findClass`); Lettuce/Reactor are left out (the game's Netty is 4.0). Client modules are relocated and downgraded as they load by `LegacyModuleTransformer`, installed with `ClientModuleManager.setClassTransformer` (`singularity.loading.ModuleClassTransformer.transformAll`, which may define helper classes; `TransformingPluginClassLoader` is shared with the server `ModuleManager`). Java 11 APIs in the core (`String.isBlank`, `Files.readString`, `java.net.http`) are rewritten to the shaded stubs, so the eCloud client works there. Only `forge/forge-189/src` and `client-common` compile with javac 8, so `client-common` must stay Java 8 syntax, and core code must not *link* Java 9+ APIs JvmDowngrader cannot stub (`ClassLoader.getDefinedPackage`, `catch` of `java.net.http` exceptions).

## Project Structure & Module Dependencies

```
module-core      (Lean module core: client module API + eCloud client + module class transforming - packages: singularity.client.*, singularity.registry.*, singularity.loading.*)
       ↑          (shaded into singularity-api; game clients run only this; client modules compile against it alone)
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
- `module-core` → `StreamlineCore-ModuleCore`
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

**Platform tags (required).** Every module lists the platforms it runs on in `pf.platforms` in its `gradle.properties`, a comma-separated subset of `proxy`, `spigot`, `paper`, `modded-server`, `modded-client` (`singularity.modules.SupportedPlatformTag`). `modules/module.gradle` fails the build when it is missing or holds an unknown id, and writes it to the `Plugin-Platforms` manifest attribute. `modded-client` alone makes a client module (`clientModule` in `module.gradle`): it extends `ClientModule`, compiles against `StreamlineCore-ModuleCore` only, and is loaded by `ClientModuleManager`, never by `ModuleManager`. `modded-client` with `modded-server` (plus any other tags) makes a world module: an ordinary server module that mod-loader game clients also offer from eCloud and manage (`WorldModules`), and run on the integrated server of worlds they host; every module tagged `modded-server` also lists `modded-client`. `modded-client` with other tags but no `modded-server` fails the build. `ModuleManager.loadModulesSafe()` / `tryRegisterExternalModule()` load a jar only if it shares a tag with `ISingularityExtension.getSupportedPlatformTags()` (`ModulePlatforms.refusal`): proxies accept `proxy`; Spigot accepts `spigot`, and Paper and its forks accept `spigot` and `paper`; mod servers accept `modded-server` (so world modules load on every platform sharing a tag, and dedicated servers ignore `modded-client`). Tag what the code actually supports: Bukkit/BOU types on a common path mean `spigot, paper` only, gameplay/entity hooks mean no `proxy`. Untagged third-party jars still load on servers with a warning, but never on a game client. `loadModulesSafe()` skips jars that are already loaded.

### Command System

`CosmicCommand` (cross-platform) is wrapped by platform-specific `ProperCommand` adapters in each platform module. Commands are registered through `CommandHandler`.

### Event System

Two layers: platform-native events (Velocity `@Subscribe`, Bungee `Listener`, Bukkit `Listener`) and cross-platform `CosmicEvent` hierarchy (player lifecycle, module lifecycle, server events, commands).

`singularity.events.player.gameplay` (`PlayerBrokeBlockEvent`, `PlayerPlacedBlockEvent`, `PlayerKilledEntityEvent`, `PlayerCaughtFishEvent`, `PlayerFilledBucketEvent`) report actions that already went through, with lowercase namespaced ids. Spigot fires them from `GameplayEventsListener` at MONITOR; the mod loaders call `mod-common`'s `GameplayEvents` from their listeners. Gaps: Fabric API has no placement, fishing, bucket or death-drops hook (kills fire with no drops); Forge/NeoForge have no bucket-fill hook.

`singularity.events.entity` (`CosmicExplosionEvent`, `CosmicMobGriefEvent`, `CosmicEntityDamagedByEntityEvent`) fire *before* a non-player entity changes the world, so listeners can prevent it (turn off block damage / cancel). Spigot fires them from `EntityEventsListener` at HIGH; NeoForge (`NeoForgeListener`) and Forge EventBus 6 (`forge-eventbus6`, so also NeoForge 1.20.1) call `mod-common`'s `EntityEvents` from `ExplosionEvent.Detonate`, `EntityMobGriefingEvent` and `LivingIncomingDamageEvent`/`LivingAttackEvent`. Every platform checks `CosmicEntityEvent.hasListeners(...)` first, because the grief check runs per mob per tick. Not fired on Fabric or Forge EventBus 7 (1.21.11+). TheBase dispatches by exact class, so listen to the concrete events. `mod-common/compat/enderman-{upto262,263}` covers Mojang's `EnderMan` → `Enderman` rename in 26.3.

### GUI System (`singularity.gui`)

`CosmicGui` (title, rows, slot → `GuiIcon` of `CosmicItem` + click handler; override `draw(viewer)` to lay out per open/redraw) and `PaginatedGui` are platform-agnostic. `GuiManager` opens them through the platform's `IGuiHandler`: `SpigotGuiHandler` (BOU `ScreenInstance`) on Spigot, `ModGuiHandler` (vanilla `ChestMenu`) on the mod loaders. A proxy has no handler: it sends the render-only `GuiView` (gzip + base64url, kept under the 32767-byte serverbound payload cap) to the player's backend over `gui-*` `ProxiedMessage`s (`singularity.gui.transport.GuiMessages`); the backend reports clicks and closes back and the handlers run on the proxy. Mod backends have no proxy messenger, so proxy-sent GUIs reach Spigot backends only. Clicks and closes carry the GUI instance id and are ignored unless it is the viewer's current one.

### Player lookups and clickable links

`singularity.utils.profiles.PlayerLookup` resolves names, UUIDs and skins without Mojang (live platform profile → Floodgate → GeyserMC API for Bedrock → playerdb.co / ashcon for Java), cached, backed off on failures, and off under `no-internet` or `player-lookups.enabled: false`. `UUIDFetcher` delegates to it. GUI player heads get their texture from it (`CosmicItem.skinTexture`), so no head ever makes the server ask Mojang's session server. Links: `ClickableMessage.linkified(...)` / `singularity.utils.Links`; only Spigot auto-links plain messages.

`IGameplayHandler.damageLivingEntities(damageFor, attributionRadius)` walks every living entity (`LivingEntityView`: type, world, position, block at an offset, permission) and hurts each by what `damageFor` returns; it returns at once and runs on the server thread (Folia: each entity's region thread, via BOU `EntityUtils.collectLivingEntitiesThenDo`; mods: `getAllEntities()`, hits applied after the walk, `McCompat.hurt` for `hurt` vs `hurtServer`). Used by `modules/StoneDamager`.

`IGameplayHandler.statistics(uuid, type, ids)` reads vanilla statistics (live online, stats file offline) and waits for the server thread; the id `*` reads every entry of the type. Modules that read statistics for many players should do it off Streamline's single task thread (StreamlineCollections uses a small `StatReader` pool).

Chat formatting order on `CosmicChatEvent`: StreamlineMessaging channels (LOWEST) → TacoEssentials mute/alias prompts (NORMAL) → TacoEssentials `ChatFormatter` (HIGH, `chat.format`) → LinkParser (HIGHEST). Each cancels what it takes over and skips cancelled events.

Module commands also register as `/<module-id>:<label>` (`CosmicCommand.getNamespacedLabels()`), never written to the command's YAML.

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

## Changelog (`UPDATES.md`)

`UPDATES.md` is the player-facing changelog for the version in `gradle.properties` (`version = ...`, e.g. `2.6.1.4`), and holds only what changed in that release, never what earlier versions already shipped. After every major change (a feature, a user-visible fix, a behavior change), update it for that version, without being asked:

- One line per change, starting with `+ `, written for players and server owners, not developers: what they can now do or what no longer goes wrong (`+ Fixed left clicks counting as right clicks on 26.3.`), never class names or internals.
- Every line is at most 10 words. Put detail that does not fit in sub-bullets, indented two spaces per level, each also at most 10 words:
  ```md
  + Fixed left clicks counting as right clicks on 26.3.
    + HUD elements can be dragged again.
    + Key settings can be bound again.
  ```
- Add lines next to related ones. Edit an existing line instead of adding a near-duplicate, and do not remove the current version's lines unless asked.
- Skip internal-only work (refactors, build scripts, docs, AGENTS.md).
- If the change ships in a new version, bump `version` first (its own `bump: version` commit) so the entry belongs to the version that ships it, and start that version's `UPDATES.md` empty: the previous version's lines belong to its release, not the new one.
- Commit the changelog on its own as `docs: changelog for <version>` (e.g. `docs: changelog for 2.6.1.4`), after the change's own commit.
