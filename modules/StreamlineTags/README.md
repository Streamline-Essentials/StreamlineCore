# StreamlineTags

Chat tags for StreamlineCore. Staff create tags and tag groups in game; players pick their
active tag from the `/tags` menu, and chat formats show it through placeholders. Works on
every Streamline platform; the menu is a `CosmicGui`.

## Who can use a tag

A player can use a tag when any of these hold:

- they have the permission `streamline.tags.tag.<id>`;
- staff granted it to them with `/tag grant <player> <id>` (stored per player);
- they have `streamline.tags.group.<group>` for a group that contains the tag.

A tag the player loses access to stops showing, even while it is still selected.

## Commands

| Command | Permission |
|---|---|
| `/tags` (alias `chattags`) | `streamline.command.tags.default` (everyone by default) |
| `/tag (create <id> <display>\|set <id> <display>\|remove <id>\|grant <player> <id>\|revoke <player> <id>\|list\|group create <group>\|group delete <group>\|group add <group> <id>\|group remove <group> <id>\|group grant <player> <group>\|reload)` (alias `tagadmin`; only `/tagadmin` on Fabric, Forge and NeoForge, where `/tag` would replace vanilla's) | `streamline.command.tagadmin.default` |

Ids are lowercase letters, numbers, `_` and `-`. A display may be wrapped in quotes and uses `&`
and `#RRGGBB` colours, e.g. `/tag create vip "&8[#FFD700VIP&8]"`.

## Placeholders

| Placeholder | Value |
|---|---|
| `%streamlinetags_tag%` | The active tag, or nothing |
| `%streamlinetags_tag_left%` | A space then the tag (for after a name), or nothing |
| `%streamlinetags_tag_right%` | The tag then a space (for before a name), or nothing |
| `%streamlinetags_tag_id%` | The active tag's id, or nothing |

They read a per-player cache, filled when the player joins. Through PlaceholderAPI (Spigot),
write them as `%streamline_!streamlinetags_tag%`.

## Storage and networks

Tags live in Streamline's main database (`tags`, `tag_groups`, `tag_group_members`,
`tag_grants`, `tag_active`, with the core's table prefix). Every server re-reads tags and groups
every `refresh-interval-seconds` (default 60), and `/tag reload` does so at once, so a tag made
on one server of a network reaches the others. Online players' grants and selections are re-read on the same interval.
