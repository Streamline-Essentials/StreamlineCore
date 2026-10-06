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
and knock back entities.

**Platform:** Spigot/Paper only. On proxies and mod loaders the module loads but
does nothing (and logs a warning).

**Command:** `/mobgrief reload` (alias `/mgc`), permission
`streamline.command.mobgrief.default`.
