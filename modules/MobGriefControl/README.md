# MobGriefControl

A StreamlineCore module that stops mobs from griefing blocks while leaving their
attacks intact (each toggle is configurable).

| Mob | Block damage | Entity damage |
|---|---|---|
| Creeper explosion | `creeper.explosion.block-damage` | `creeper.explosion.entity-damage` |
| Ghast fireball (incl. fire) | `ghast.fireball.block-damage` | `ghast.fireball.entity-damage` |
| Enderman | `enderman.pick-up-blocks` | — |
| Wither skulls | `wither.skulls.block-damage` | `wither.skulls.entity-damage` |
| Wither itself (moving + spawn blast) | `wither.self.block-damage` | — |

`false` disables the behaviour. Defaults: no block damage anywhere, entity damage on.

Block damage is removed by clearing the explosion's block list, so blasts still hurt
and knock back entities. Endermen may still put down a block they already carry.

## Platforms

Built on StreamlineCore's cross-platform entity events (`singularity.events.entity`):

- **Spigot/Paper** — everything.
- **NeoForge** (1.20.1 – 26.3) and **Forge 1.20.1 / 1.21.1** — everything, except that
  `entity-damage: false` only protects living entities (mobs, players, armor stands):
  the loaders have no damage hook for item frames, paintings or dropped items.
- **Forge 1.21.11+, Fabric, proxies** — the core does not fire these events there; the
  module loads but blocks nothing.

## Command

`/mobgrief reload` (alias `/mgc`), permission `streamline.command.mobgrief.default`.
