# ArenaFFAManager

ArenaFFAManager is a comprehensive Spigot plugin (v1.8.8) designed for managing Arenas, BuildFFA, BridgeFight, and Private Worlds. It includes features like stats tracking, leaderboards, region-based command triggers, hotbar management, and moderation tools.

## Features

- **Arena Management**: Create, edit, and restore arenas for various game modes.
- **BuildFFA & BridgeFight**: Specialized game modes with integrated kits and stats.
- **Region System**: Define custom regions that trigger commands or have specific flags.
- **Hotbar Manager**: Allow players to customize their item layouts.
- **Stats & Leaderboards**: Track kills, deaths, and streaks with GUI-based leaderboards.
- **Moderation Tools**: Freeze players, handle reports, and manage bans.
- **Placeholders**: Support for PlaceholderAPI to display stats anywhere.

---

## Commands

### Admin Commands

| Command | Usage | Description | Permission |
|:---|:---|:---|:---|
| `/arenamap` | See [Arena Subcommands](#arena-subcommands-arenamap) | Manage arenas | `arenamap.admin` |
| `/arenaconfig` | `/arenaconfig reload` | Reload arena configuration | `arenamap.admin` |
| `/configreload` | `/configreload` | Reload general configuration | `arenamap.admin` |
| `/rc` | See [Region Subcommands](#region-subcommands-rc) | Manage command regions | `arenamap.admin` |
| `/setlobby` | `/setlobby` | Set the global lobby location | `arenamap.admin` |
| `/build` | `/build` | Toggle Build Mode for admins | `arenamap.admin` |
| `/setstats` | `/setstats <player> <mode> <stat> <value>` | Manually set player stats | `arenamap.admin` |
| `/statsreset` | `/statsreset <player>` | Reset a player's stats | `arenamap.admin` |
| `/hbmresetall` | `/hbmresetall` | Reset all hotbar manager data | `arenamap.admin` |
| `/loadworld` | `/loadworld <name> <type>` | Load a world into the server | `arenamap.admin` |
| `/togglebridgeegg` | `/togglebridgeegg` | Toggle Bridge Egg functionality | `arenamap.admin` |
| `/givepots` | `/givepots <type> <amount>` | Give specialized potions | `arenamap.admin` |
| `/syncdata` | `/syncdata` | Synchronize plugin data | `arenamap.admin` |
| `/arenabypass` | `/arenabypass` | Bypass arena restrictions | `arenamap.admin` |
| `/commandbypass` | `/commandbypass` | Bypass command restrictions | `arenamap.admin` |

#### Arena Subcommands (`/arenamap`)
- `create <name>`: Create a new arena.
- `pos1/pos2 <name>`: Set region corners.
- `center <name>`: Set the arena spawn/center point.
- `type <name> <ffa|ffabuild|duel>`: Set arena game type.
- `buildlimit <name> <Y>`: Set building height limit.
- `voidlimit <name> <Y>`: Set void TP/death limit.
- `capture <name>`: Capture current blocks (for FFABUILD restoration).
- `restore <name>`: Restore captured blocks.
- `finish <name>`: Mark arena as ready for use.
- `clone <name>`: Clone arena across different arena worlds.
- `list`: List all arenas and their status.
- `info`: Display info about the arena you are standing in.
- `reload/save`: Reload or save arena data.

#### Region Subcommands (`/rc`)
- `pos1/pos2`: Set selection corners.
- `wand`: Get the region selection tool (Stone Axe).
- `create <name> flag <flag> <value>`: Create a region with a specific flag.
- `create <name> command <player|console> <command>`: Create a region that runs a command on entry.
- `edit <name> flag <flag> <value>`: Edit existing region flags.
- `delete <name>`: Remove a region.
- `info <name>`: Display region details.
- `list`: List all defined regions.

---

### Player Commands

| Command | Usage | Description |
|:---|:---|:---|
| `/buildffa` | `/buildffa` | Join the BuildFFA arena (Alias: `/ffa`) |
| `/bridgefight` | `/bridgefight` | Join BridgeFight (Alias: `/housing`) |
| `/lobby` | `/lobby` | Return to spawn (Alias: `/spawn`) |
| `/stats` | `/stats [player]` | View your or another player's stats |
| `/guistats` | `/guistats [player]` | View stats in a GUI |
| `/leaderboard` | `/leaderboard` | View the leaderboards |
| `/guileaderboard` | `/guileaderboard` | Open the leaderboard GUI |
| `/hotbarmanager`| `/hotbarmanager` | Customize your item layout (Alias: `/hbm`) |
| `/report` | `/report <player> <reason>` | Report a player for misbehavior |
| `/patchnotes` | `/patchnotes [page]` | View recent update notes |
| `/kit` | `/kit` | Open kit selection menu |
| `/privateworld` | `/privateworld` | Access your private world (Requires: `privateworld.access`) |

---

### Moderation Commands

| Command | Usage | Description | Permission |
|:---|:---|:---|:---|
| `/freeze` | `/freeze <player>` | Freeze a player for inspection (Alias: `/ss`) | `arenamap.freeze` |
| `/unfreeze` | `/unfreeze <player>` | Unfreeze a player | `arenamap.freeze` |
| `/reports` | `/reports [page]` | View active player reports | `arenamap.admin` |
| `/playerhistory`| `/playerhistory <player>`| View a player's history | `arenamap.admin` |
| `/bridgeban` | `/bridgeban <player> [dur] [res]` | Ban a player from BridgeFight | `bridgefight.banned` |
| `/bridgeunban` | `/bridgeunban <player>` | Unban a player from BridgeFight | `bridgefight.unbanned` |

---

## Permissions Summary

- `arenamap.admin`: Full access to admin and setup commands.
- `arenamap.freeze`: Ability to freeze/unfreeze players.
- `bridgefight.banned`: Permission used to restrict access (usually assigned by the ban system).
- `bridgefight.unbanned`: Permission to use the unban command.
- `privateworld.access`: Access to the Private World system.

---

## Dependencies

- **Spigot 1.8.8** (Target Platform)
- **PlaceholderAPI** (Optional - for stats placeholders)
- **WorldGuard & WorldEdit** (Required for region and arena selection)
- **ProtocolLib** (Optional - for advanced packet handling)

---

## Installation

1. Place the `ArenaAndFFAManager.jar` in your server's `plugins` folder.
2. Ensure you have the required dependencies (WorldGuard, WorldEdit).
3. Restart the server.
4. Configure settings in `plugins/ArenaAndFFAManager/config.yml`.
