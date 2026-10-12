![Deps](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/Dependencies.png?raw=true)

MUST HAVE:
- BukkitOfUtils (__For Bukkit / Spigot / Paper ONLY__). [**[ FOUND HERE ]**](https://modrinth.com/plugin/bukkitofutils)
- Fabric API (__For Fabric ONLY__). [**[ FOUND HERE ]**](https://modrinth.com/mod/fabric-api)

RECOMMENDED:
- LuckPerms. [**[ FOUND HERE ]**](https://luckperms.net/download) (Optional. Without it, Streamline falls back to the server's own permission checks and can't grant or remove permissions.)

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/Main.png?raw=true)
Join the Streamline Hub Discord for updates, and so I can help you with bugs, questions, or suggestions.

Discord: [**click here**](https://dsc.gg/streamline)

[<font size="5">**Download Modules for Streamline Here (Click)**</font>](https://modules.drak.gg)

# Introduction to Streamline

### What is Streamline?
Streamline is a Minecraft plugin and mod that adds features to your server through **Modules**. A module is a jar you drop into Streamline's ``modules`` folder. Admins and developers can also write their own modules on top of Streamline's framework.

Current modules include:
- Redirecting players to another server when theirs goes down. (**StreamlineRedirect**)
- `/msg`, `/reply`, friends (including best friends), and chat rooms for staff, local, global, and custom chat. (**StreamlineMessaging**)
- A Discord to Minecraft chat bridge, plus Discord verification. (**StreamlineDiscord**)
- Logging chat and other server events to Discord with only webhooks. (**SimpleLogger**)
- Cross-network homes, tpa, and teleporting, plus scripts, aliases, nicknames, and online lists per server, per group, or network wide. (**StreamlineUtilities**)
- Essential commands for teleports, homes, warps, utilities, and moderation, built for modded servers and working on Spigot and Paper too. (**TacoEssentials**)
- Guilds and parties, with a `/pwarp` command. (**StreamlineGroups**)
- Chat tags players equip from a menu, granted by staff, permission, or tag group. (**StreamlineTags**)
- Hypixel-style collections with levels, claimable rewards, menus, and leaderboards. (**StreamlineCollections**)
- A cross-network quest system. (**StreamQuests**)
- Data-driven, event-based protection on the proxy. (**ProxProtect**)
- Running commands on other servers, including the proxy a backend is connected to. (**RunOn**)
- Sending resource packs to players when they join, even through BungeeCord and Velocity. (**ResourcePackUtils**)
- An MOTD system along the lines of MiniMOTD: version, description, player sample, player count, and server icons. (**StreamlineMOTD**)
- Clickable links in chat for players allowed to post them. (**LinkParser**)
- Stopping creepers, ghasts, endermen, and withers from breaking blocks, with per-mob damage toggles. (**MobGriefControl**)
- Stonecutters that damage entities standing on them. (**StoneDamager**)
- Streamline placeholders in TAB configs. (**TAB-SL**)
- *And more.*

<u>**NOTE**</u>: Not seeing a feature you want? Ask Drak on the Discord: [**click here**](https://dsc.gg/streamline)

### What makes Streamline different?
The same module runs on every platform Streamline supports. You can put one module on a BungeeCord proxy, a Velocity proxy, a Paper backend, and a Fabric, Forge, or NeoForge server, and it behaves the same on each. Since every feature is its own module, you install only the ones you want.

That also means a module you like on one server works on the rest of your network, and a developer can write a module once for every server type.

### Client mods
The Fabric, Forge, and NeoForge jars also work on game clients. In game, Streamline adds:
- A module menu (`/streamlineclient menu`, or a key you bind in Controls) where you turn client modules on and off, change their settings, and install new ones from eCloud.
- A HUD editor (Right Shift by default) for dragging, scaling, hiding, and editing on-screen elements.
- Client modules, which load, update, and unload while the game runs. **StreamlineHUD** adds FPS, ping, clocks, and more, with editable text and placeholders. **ClientTogglesModule** adds toggle sprint and toggle sneak.
- World modules: server modules that also run in your singleplayer and LAN worlds.

A client never needs a Streamline server to use any of this. A separate Forge 1.8.9 client jar runs the client modules, HUD, and menu as well (no world modules there, since it hosts no Streamline server).

### Where does Streamline run?

#### Supported Proxies:
> - Velocity
> - BungeeCord
> - Waterfall
> - FlameCord
> - Travertine
> - XCord
> - Etc.

#### Supported Backend Servers:
> - Spigot.
> - Paper.
> - Folia.
> - Purpur.
> - Tuinity.
> - ImmanitySpigot.
> - FlamePaper.
> - Mohist / Banner / Youer.
> - Etc.

#### Supported Mod Loaders (servers and clients):
> - Fabric. (1.20.1, 1.21.1, 1.21.11, 26.2, 26.3)
> - Forge. (1.20.1, 1.21.1, 1.21.11, 26.2, 26.3; 1.8.9 clients only)
> - NeoForge. (1.20.1, 1.21.1, 1.21.11, 26.2, 26.3)

#### Natively-Supported Plugins:
> - LuckPerms. (Recommended.)
> - BukkitOfUtils. (Required on Bukkit-like servers.)
> - Fabric API. (Required on Fabric.)
> - Geyser / Floodgate. (Everything works the same for Bedrock players as for Java players.)
> - PlaceholderAPI.
> - CMI. (With the Utilities module.)
> - EssentialsX. (With the Utilities module.)

![Discord Setup](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/DiscordSetupHelp.png?raw=true)

Follow the Discord setup guide [**[ FOUND HERE ]**](https://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup) to set up the Discord module.

# What can I do with the `Discord Module`?
- Link **Discord to Minecraft** and **Minecraft to Discord** with `Routes`.
    - Available link types:
        - <u>Natively</u> (Without other Modules):
            - **Proxy local chat** `<->` **Discord Text Channel**
            - **Proxy global chat** `<->` **Discord Text Channel**
            - **All online players with specific permission** `<->` **Discord Text Channel**
            - **Discord Text Channel** `<->` **Discord Text Channel**
        - <u>Groups</u> (With `StreamlineGroups` Module):
            - **Guild chat** `<->` **Discord Text Channel**
            - **Party chat** `<->` **Discord Text Channel**
        - <u>Messaging</u> (With `StreamlineMessaging` Module):
            - **Local Room chat** `<->` **Discord Text Channel**
            - **Global Room chat** `<->` **Discord Text Channel**
            - **Custom Room chat** `<->` **Discord Text Channel**
    - You can change every message format.
    - Messages sent to Discord show the player's skin as the avatar.
- Link a player's Discord account to your server.
    - Linked players can chat from Discord as their Streamline user.
- Send any Discord message as a fully custom embed.

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/CommandsAndPermissions.png?raw=true)

More information is on the wiki: [**FOUND HERE**](https://wiki.plas.host/streamline)

### NOTICE ABOUT COMMANDS, PERMISSIONS, AND MESSAGES.
***Every command has its own `.yml` file, where you can change it, its permission, and its messages.***

![How to Install It](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/HowToInstall.png?raw=true)

1. Install dependencies.
- ``BukkitOfUtils`` if on Bukkit / Spigot / Paper / Forks.
- ``Fabric API`` if on Fabric.
- ``LuckPerms`` (recommended) for all platforms.
2. Download the file for your platform (and, for mod loaders, your Minecraft version).
3. Drop it into your server's `plugins` folder (or `mods` folder on Fabric / Forge / NeoForge).
4. Start and stop your server.
5. Fill out the configs where necessary.
    - Streamline's configs are in your `plugins` folder (`mods` on mod loaders), under `StreamlineCore`.
6. Add the modules you want.
    - Download modules here: [**click here**](https://modules.drak.gg)
7. Start and stop your server again.
8. Fill out module configs where necessary.
    - Module files are in your `plugins` folder (`mods` on mod loaders), under `StreamlineCore`, under `module-resources`.
9. Start your server again.

On a game client, put the jar (and Fabric API on Fabric) in your `mods` folder and launch the game. Client modules go in `mods/StreamlineCore/client/modules/`, or install them from the eCloud page in the module menu.

More information is on the wiki: [**click here**](https://wiki.plas.host/streamline)

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/NeedHelpSubmitBugs.png?raw=true)

1. Look on the wiki: [**click here**](https://wiki.plas.host/streamline)
2. Ask on the Streamline Discord: [**click here**](https://dsc.gg/streamline)
3. Open an issue on the issue tracker (rarely checked): [**click here**](https://github.com/Streamline-Essentials/StreamlineCore/)

# BStats
**Bukkit**
![BStats Profile - Bukkit](https://bstats.org/signatures/bukkit/StreamlineCore.svg)
**BungeeCord**
![BStats Profile - BungeeCord](https://bstats.org/signatures/bungeecord/StreamlineCore.svg)
**Velocity**
![BStats Profile - Velocity](https://bstats.org/signatures/velocity/StreamlineCore.svg)
