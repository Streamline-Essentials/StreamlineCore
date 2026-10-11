# StreamlineHUD

A client-only StreamlineCore module (`pf.platforms = modded-client`) that adds HUD
elements to Fabric, Forge and NeoForge game clients. It extends `ClientModule` and
compiles against `StreamlineCore-ModuleCore` only (`singularity.client.*`), with no
Minecraft types. Install it in game with `/slclient modules ecloud download StreamlineHUD`
or from the eCloud page of `/slclient menu`; it loads and unloads without a restart.

| Element | Shows | Default place |
|---|---|---|
| `fps` | Frames per second | left, row 1 |
| `coordinates` | X / Y / Z | left, row 2 |
| `direction` | Facing and yaw | left, row 3 |
| `biome` | Biome at your feet | left, row 4 |
| `dimension` | Current dimension | left, row 5 |
| `ping` | Round trip to the server | right, row 1 |
| `server` | Server address (`singleplayer` locally) | right, row 2 |
| `clock` | Real-world time | right, row 3 |
| `game-time` | In-game day and time | right, row 4 |
| `memory` | Used / max heap (refreshed once a second) | right, row 5 |

Move, scale and hide them with `/slclient hud`. Style them on the module's page in
`/slclient menu`: text color, background on/off and color, text shadow, coordinate
decimals (0-3) and 12h/24h clocks. Settings are saved under
`client/settings/streamline-hud.properties` in Streamline's data folder.
