# LinkParser

Makes links in chat clickable for players allowed to post them, on Spigot, the mod loaders,
and (opt-in) Velocity/BungeeCord.

- **Permission:** `streamline.linkparser.use` (configurable). Without LuckPerms, server
  operators count as having it.
- **Colours:** players without `streamline.linkparser.colors` have `&` codes stripped from
  messages that contain links.
- A chat message from an allowed player that contains a link is cancelled and re-sent using
  `format` from `config.yml`, with every link clickable (`link-hover` is the tooltip).
  Messages without links are left to the server untouched.
- Runs after other chat modules: if StreamlineMessaging (or anything else) already took a
  message over, LinkParser leaves it alone.
- **Proxies:** off by default (`proxy.enabled`). Recent clients sign chat, and a proxy
  cancelling signed chat can get the player kicked; install the module on the game servers
  instead.
