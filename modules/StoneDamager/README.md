# StoneDamager

A StreamlineCore module that hurts living entities (mobs, animals, players) while
they stand in or on configured blocks — by default, stonecutters. Port of the
standalone StoneDamager Bukkit plugin, running on every backend platform.

Each entry under `damagers:` in `config.yml` sets:

| Key | Meaning |
|---|---|
| `enabled` | Whether the damager runs. |
| `offset.x/y/z` | Where to look for the block, from the entity's feet. `0/0/0` = the block the feet are in; `0/-0.1/0` = the block below. |
| `ticks-per-damage` | Ticks between hits (20 = 1 second). |
| `damage-amount` | Damage per hit, in half-hearts. |
| `permission.include` / `permission.exclude` | Players need `include` (if set) and must lack `exclude` (default `stonedamager.bypass`) to be hurt. |
| `materials`, `worlds`, `entities` | `list` + `is-blacklist`. An empty whitelist matches nothing. |

Ids are namespaced (`minecraft:stonecutter`, `minecraft:zombie`); the namespace
may be left out and case is ignored, so configs from the Bukkit plugin
(`STONECUTTER`, `ZOMBIE`) keep working. Worlds are world names on Spigot/Paper and
dimension ids (`minecraft:the_nether`) on the mod loaders.

`settings.try-find-player` credits each hit on a mob to the nearest player within
`radius`, so kills drop player-kill loot and experience. Hits on players are never
credited to anyone.

Other modules can listen to `host.plas.stonedamager.events.StoneDamageEvent` to
cancel a hit or change its damage.

## Platforms

Uses `IGameplayHandler.damageLivingEntities` from the core:

- **Spigot / Paper / Folia** — Folia included; each entity is handled on its region thread.
- **Fabric, Forge, NeoForge** — every supported Minecraft version.
- **Velocity, BungeeCord** — proxies have no worlds; the module loads and does nothing.

## Command

`/reloadstonedamager` (aliases `/rlsd`, `/reloadsd`), permission
`streamline.command.reloadstonedamager.default`.
