# StreamlineCollections

Hypixel-style collections, slayer and progression stars for StreamlineCore. Players gather items by breaking blocks,
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
| `/slayer ((player)\|open <mob>)` | `streamline.command.slayer.default`; `streamline.command.slayer.others` to view another player |
| `/slayerlb (overall\|<category> (total\|<mob>)\|<mob>)` (aliases `slayerleaderboard`, `slayerleaderboards`) | `streamline.command.slayerlb.default` |
| `/slayeradmin (sync <all\|<player>>\|set <player> <mob\|all> <kills>\|add <player> <mob\|all> <kills>\|remove <player> <mob\|all> <kills>\|reset <player> (mob))` | `streamline.command.slayeradmin.default` |
| `/progression (gui\|stars get (player)\|exp get <category> (player)\|skills (player))` (alias `prog`) | `streamline.command.progression.default` |
| `/progression exp <set\|add\|remove> <amount> <player> <category>` | also `streamline.command.progression.admin` |
| `/progressionleaderboard (stars\|total\|<category>)` (aliases `progressionlb`, `plb`) | `streamline.command.progressionleaderboard.default` |
| `/progressionadmin sync <all\|<player>>` (alias `progadmin`) | `streamline.command.progressionadmin.default` |

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

## Slayer

Kills per mob, with ten levels per mob (`tiers`; bosses use `boss-tiers`) whose rewards are
claimed from `/slayer`, the same way as collections. Mobs are grouped into Monsters, Nether,
Aquatic, Passive, Pets and Bosses. With `sync-stats` on, every count is raised to the player's
vanilla `minecraft:killed` statistic on join and at each statistics refresh, never lowered.
`/slayeradmin` edits apply live to players loaded on the server running it and to the database
for anyone else; on a network, edit a player from the server they are on.

## Progression and stars

Every category in `progression.yml` is worth amount × weight XP; the total gives the star level.
The cost of star n → n+1 is `min(max-needed, n * formula-multiplier + formula-base)`. Stars are
cosmetic: they show in chat through `%streamlinecollections_stars%`, in `/progression` and on the
boards, and a player is told when they reach a new one.

Chat messages, joins, leaves and blocks broken or placed are counted from events. Kills, mob kills,
fish, enchants, crafts, pickups, drops and distances follow vanilla statistics: each read (every
`refresh-interval-seconds`, and on quit) adds what the statistic gained since the last one, so
several servers of a network add into one amount. With `sync-stats` on, those categories, broken
blocks and leaves are also raised to this server's statistic; staff changes made with
`/progression exp` are kept on top.

Skills (Mining, Farming, Combat, Fishing, Foraging, Enchanting, Crafting, Excavation) train one XP
per action and level up every `skill-xp-per-level`. Block skills are worked out from block ids;
Combat, Fishing, Enchanting and Crafting from what their statistic gained.

## Files

- **`collections.yml`** holds the categories and collections, the amounts each level needs
  (`default-tiers`, or `tiers` on a collection) and what feeds each collection. Ids are item or
  block ids: `wheat` means `minecraft:wheat`, and modded content uses its full id. Menus follow
  the file's order.
- **`config.yml`** holds the tracking toggles, the save interval, the rewards for each level
  (console commands plus a description shown in menus), reminders and every message.
- **`slayer.yml`** holds slayer's toggles, level thresholds, rewards and messages.
- **`progression.yml`** holds the star formula, the category weights, syncing and messages.

Progress is stored in the core database: `collections` and `collection_claims`, `slayer` and
`slayer_claims`, and `progression` (one row per player and category, skill or bookkeeping key).
Blocks players place are remembered under `placed-blocks/` in the module's folder, so breaking
them again does not count. Only placing and breaking update that record. A placed block moved
by a piston, or removed by an explosion, fire or decay, keeps its mark at the old position.

## Placeholders

These only resolve for players loaded on the server parsing them, from memory. Each also works
with the older `%collections_...%` prefix.

| Placeholder | Value |
|---|---|
| `%streamlinecollections_unclaimed%`, `_tiers%`, `_maxed%` | Collection levels waiting, completed, collections maxed |
| `%streamlinecollections_amount_<collection>%`, `_level_<collection>%` | One collection's amount and level |
| `%streamlinecollections_slayer_kills%`, `_slayer_unclaimed%`, `_slayer_tiers%`, `_slayer_maxed%` | Slayer totals |
| `%streamlinecollections_slayer_kills_<mob>%`, `_slayer_level_<mob>%` | One mob's kills and level |
| `%streamlinecollections_stars%` | The chat star prefix (`messages.chat-prefix`), empty when chat stars are off |
| `%streamlinecollections_star_emoji%` | `messages.star-emoji` |
| `%streamlinecollections_star_level%`, `_star_exp%`, `_star_progress%` | Stars, total weighted XP, percent to the next star |
| `%streamlinecollections_star_exp_<category>%` | One category's amount |
| `%streamlinecollections_skill_level_<skill>%`, `_skill_exp_<skill>%` | One skill's level and XP |

## What each platform tracks

| Source | Spigot | Forge / NeoForge | Fabric |
|---|---|---|---|
| Block breaks (crops only when grown) | yes | yes | yes |
| Remembering placed blocks | yes | yes | no: Fabric API has no placement event |
| Mob drops | yes | yes | no: Fabric API exposes no death drops |
| Fishing | yes | yes | no |
| Bucket fills | yes | no | no |
| Slayer kills | yes | yes | yes |
| Progression: placed blocks | yes | yes | no |
| Progression: everything else | yes | yes | yes |

Slayer cannot leave out spawner mobs (the kill event carries no spawn reason), and progression has
no Archery skill (the kill event does not say whether a projectile killed).
