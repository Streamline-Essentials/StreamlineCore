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
`/slclient menu`: main and text colors, background on/off and color, text shadow,
coordinate decimals (0-3) and 12h/24h clocks. Settings are saved under
`client/settings/streamline-hud.properties` in Streamline's data folder.

## Element text

Each element's **Edit** button on the module's page opens its text, with a live preview
and the placeholder list (click one to insert it). Text takes `&` color codes, hex colors
(`{#rrggbb}`, `&#rrggbb`) and placeholders in RAT form, `%streamlinehud_<key>%`.
`%newline%` starts another line. **Reset** puts the default back.

| Placeholder | Shows |
|---|---|
| `color_main` / `color_text` | the Main color / Text color settings, as a color code |
| `fps` | frames per second |
| `x`, `y`, `z` / `block_x`, `block_y`, `block_z` | position (coordinate decimals) / block position |
| `facing`, `yaw`, `pitch` | direction |
| `biome`, `biome_id`, `dimension`, `dimension_id` | where you are |
| `ping`, `server`, `player` | connection and your name |
| `clock`, `game_time`, `day` | real time, in-game time and day |
| `memory_used`, `memory_max`, `memory_percent` | heap use |

The **+** by "HUD elements" adds a text element of your own: an identifier (lowercase
letters, digits, `-`, `_`) and its text. It starts at the top-left corner; move it with HUD
Layout. Added elements have a **Delete** button in their editor. Edited and added text is
saved in `client/module-data/streamline-hud/elements.properties`.

Editing and adding text needs a StreamlineCore mod built with the editable HUD API
(2.6.1.1 or newer); on an older mod the elements still work with their saved text.
