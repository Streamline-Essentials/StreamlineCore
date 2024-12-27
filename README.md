# Dependencies
* [BukkitOfUtils](https://modrinth.com/plugin/bukkitofutils)

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/Main.png?raw=true)
Please join the Streamline Hub Discord in order to get updates and for me to fully assist you with bugs, questions, or suggestions.

Discord: [**click here**](https://dsc.gg/streamline)

# Introduction to Streamline

### What is Streamline?
Streamline is a Minecraft plugin that adds limitless functionality to Minecraft servers through what is known as **Modules**. These Modules are downloadable content that you insert into the Streamline plugin's ``modules`` folder to enhance your server.

Streamline Modules can do many things and even allow server admins and developers to make their own with a very nice API.

The currently made modules add functionality such as:
- Redirecting players from one server to another when that server goes down. (StreamlineRedirect Module)
- Network staff chat, whispering, and friends. (StreamlineMessaging Module)
- Discord to Minecraft and Minecraft to Discord. (StreamlineDiscord Module)
- Aliases, buildable functions, online list (per server, per group, or network wide), cross-network tpa, cross-network homes, cross-network teleporting, and more. (StreamlineUtilities Module)
- Data-Driven event-based protection for proxy and backend servers. (ProxProtect Module)
- Spying on all commands sent by players and sending them to Discord. (SimpleLogger Module)
- Send resourcepacks to players on join. (ResourcePackUtils Module)
- Cross-network quests system with limitless potential. (StreamQuests Module)
- TAB (the plugin) integration, adding support for Streamline placeholders in TAB configurations. (TAB-SL Module)
- Simple and easily configured MOTD system like that of MiniMOTD. (StreamlineMOTD Module)
- More.

Not seeing a feature you want? Ask Drak to add it on his Discord! - [**click here**](https://dsc.gg/streamline)

### What makes Streamline special?
Streamline allows for **universal cross-platform capabilities** with its Modules.

This essentially means you can *run the same modules on a BungeeCord proxy that you would on a Velocity proxy* (or even a Spigot or Paper backend server). This, coupled with the `Modularized` features means that **you**, the server admin, can choose *exactly* what you want in your server.

This also makes it very simple for server admins to use a specific module **across your servers**, or even for `Developers` to **develop modules for your server** (or the community).

### What exactly is Streamline capable of?
Literally *anything*.

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
> - Etc.

#### Natively-Supported Plugins:
> - Luckperms. (Required.)
> - BukkitOfUtils. (Required on Bukkit-like servers.)
> - Geyser.
> - PlaceholderAPI.
> - CMI. (With Utils module.)
> - EssentialsX. (With Utils module.)

![Deps](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/Dependencies.png?raw=true)

MUST HAVE:
- LuckPerms. [**[ FOUND HERE ]**](https://luckperms.net/download)
- BukkitOfUtils (__For Bukkit / Spigot / Paper ONLY__). [**[ FOUND HERE ]**](https://www.spigotmc.org/resources/118276/)

![Discord Setup](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/DiscordSetupHelp.png?raw=true)

Follow the Discord setup guide [**[ FOUND HERE ]**](https://github.com/Streamline-Essentials/StreamlineWiki/wiki/Discord-Setup) to set up the discord module.

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
  - Message formats are completely customizable.
  - Avatar with player's skin will pop up when their message is sent to Discord.
- Link player's Discord profile with your server.
  - Players can chat using their StreamlineUser profile from Discord.
- All Discord messages can be configured to be sent as <u>completely customized Discord embedded messages</u>

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/CommandsAndPermissions.png?raw=true)

More information is found on our wiki: [**FOUND HERE**](https://wiki.plas.host/streamline)

### NOTICE ABOUT COMMANDS
***All commands are completely customizable in their `.yml` file.***

### NOTICE ABOUT PERMISSIONS
***All permissions are completely customizable in their command's `.yml` file.***

![How to Install It](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/HowToInstall.png?raw=true)

1. Install dependencies.
  - ``BukkitOfUtils`` if on Bukkit / Spigot / Paper / Forks.
  - ``LuckPerms`` for all platforms.
2. Download the plugin file.
3. Drop it into your server's plugins folder.
4. Start and stop your server.
5. Fill out the configs where necessary.
   - Configurations for the streamline plugin can be found in your `plugins` folder, under `StreamlineCore` (or `streamlinecore` if using Velocity).
6. Add desired modules.
   - Modules can be downloaded from here: [**click here**](https://wiki.plas.host/modules)
7. Start and stop your server again.
8. Fill out module configs where necessary.
   - Module files can be found in your `plugins` folder, under `StreamlineCore` (or `streamlinecore` if using Velocity), under `module-resources`.
9. Start your server again.

More information can be found on our wiki: [**click here**](https://wiki.plas.host/streamline)

![Discord](https://github.com/Streamline-Essentials/StreamlineWiki/blob/main/website/images/NeedHelpSubmitBugs.png?raw=true)

1. Look on the wiki: [**click here**](https://wiki.plas.host/streamline)
2. Get in touch on the Streamline Discord: [**click here**](https://dsc.gg/streamline)
3. Submit a bug on the issue tracker (this is rarely checked): [**click here**](https://github.com/Streamline-Essentials/StreamlineCore/issues)

# StreamlineCore API
## What does the `StreamlineCore API` allow for?
- Friendly API to create your own addons
- `RATAPI` (Replace A Thing API)
    - This is an API that gives you the ability to easily and efficiently add your own placeholders (similar to `PlaceholderAPI`), but for **both** proxies and backends.
- Reload ability
    - `Streamline` works with reloads to prevent proxy and backend restarts.
