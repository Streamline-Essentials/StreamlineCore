# StreamlineCollections

Hypixel-style collections for StreamlineCore. Players gather items by breaking blocks,
killing mobs, fishing and filling buckets. Each collection has levels, and a completed
level's reward is claimed by clicking it in the collection's menu.

The menus are `CosmicGui`s, so the same jar shows them on Spigot, Fabric, Forge and
NeoForge. On a Velocity or BungeeCord proxy, `/collections` and `/collectionsleaderboard`
read from the shared database and send the menu to the player's backend to render.
Backends running Spigot can render it; backends on a mod loader cannot yet.

## Commands

| Command | Permission |
|---|---|
| `/collections ((player)\|open <collection>)` (aliases `collection`, `col`) | `streamline.command.collections.default`; `streamline.command.collections.others` to view another player |
| `/collectionsleaderboard (overall\|<category> (total\|<collection>)\|<collection>)` (aliases `clb`, `colleaderboard`, `collectionleaderboard`, `collectionslb`) | `streamline.command.collectionsleaderboard.default` |
| `/collectionsadmin <reload\|give <player> <collection> <amount>\|set <player> <collection> <amount>\|wipe <player>\|sync (player)>` (alias `coladmin`) | `streamline.command.collectionsadmin.default` |

`give`, `set`, `wipe` and `sync` only work for players online on the server running the command.

## Syncing from server statistics

Collections also follow each server's vanilla statistics (`world/stats/<uuid>.json`). With
`stat-sync` on, a player is synced when they join, every `interval-minutes` while online, and on
`/collectionsadmin sync`. Each collection is raised to at least the sum of its sources' statistics,
and never lowered, so whichever is larger wins: the tracked amount or the statistics. Play from
before the module was installed counts this way, as do sources a platform's events miss, such as
mob drops and fishing on Fabric.

`stat-sync.sources` picks the statistic each source type reads. By default, blocks read
`mined` and drops and fish read `picked_up`; buckets read nothing, since vanilla counts every bucket
use as one statistic. Statistics follow vanilla's rules, not the module's: `mined` counts blocks
the player placed and crops broken before they were grown, and `picked_up` counts any item picked
up from the ground. A wiped player is raised back to their statistics on the next sync.

## Files

- **`collections.yml`** holds the categories and collections, the amounts each level needs
  (`default-tiers`, or `tiers` on a collection) and what feeds each collection. Ids are item or
  block ids: `wheat` means `minecraft:wheat`, and modded content uses its full id. Menus follow
  the file's order.
- **`config.yml`** holds the tracking toggles, the save interval, the rewards for each level
  (console commands plus a description shown in menus), reminders and every message.

Progress is stored in the core database, in the `collections` and `collection_claims` tables.
Blocks players place are remembered under `placed-blocks/` in the module's folder, so breaking
them again does not count. Only placing and breaking update that record. A placed block moved
by a piston, or removed by an explosion, fire or decay, keeps its mark at the old position.

## Placeholders

These only resolve for players loaded on the server parsing them:

- `%collections_unclaimed%`
- `%collections_tiers%`
- `%collections_maxed%`
- `%collections_amount_<collection>%`
- `%collections_level_<collection>%`

## What each platform tracks

| Source | Spigot | Forge / NeoForge | Fabric |
|---|---|---|---|
| Block breaks (crops only when grown) | yes | yes | yes |
| Remembering placed blocks | yes | yes | no: Fabric API has no placement event |
| Mob drops | yes | yes | no: Fabric API exposes no death drops |
| Fishing | yes | yes | no |
| Bucket fills | yes | no | no |
