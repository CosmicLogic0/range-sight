# Range Sight 1.1.0

## New
- **Trial spawner and vault ranges** (Minecraft 1.21 and newer): see how close you must be for a trial spawner to wake up (14 blocks) or a vault to open (4 blocks). Key: Numpad 7. Plugin: `/range trial`.
- **Settings screen** on every version (1.12.2 to 26.3): turn each range on or off, change its color, line width and search distance. Open it with Numpad . or the **Config** button in the mod list. Hovering a button explains it (the 1.12.2 screen too).
- **First-time tip:** the first time you join a world, a chat message tells you which key shows every range.
- **Plugin:** `/range help`, `/range reload` (permission `rangesight.admin`) and a `config.yml` for search distance, outline count, rescan time, long-distance particles and colors.
- **Plugin:** outlines use LogicLib's long-distance particles, so whole ranges show from more than 32 blocks away.

## Improved
- Only the ranges your Minecraft version has get a key and a settings row (no more do-nothing keys for sculk on 1.16.5, or for spawn chunks on 1.21.9 and newer).
- The plugin says so when a range doesn't exist on the server's Minecraft version, instead of drawing nothing.
- Your choices in the plugin are kept when you log out and back in (until the server restarts).
- Clearer key names, help text and chat messages.
- The plugin now requires LogicLib Plugin 0.2.0 or newer, and tells the server owner clearly if it's missing or too old.

## Fixed
- A typo in `config/rangesight.json` reset and overwrote the file. Now it's kept as `rangesight.json.broken-<date>`, the defaults are used, and you're told in chat.
- Outlines from the world or dimension you just left could stay on screen for a moment after changing dimension or opening another world.
- Plugin outlines disappeared beyond 32 blocks, so large ranges (beacon, conduit, raid) were cut off.
- Forge 1.16.5 and 1.18.2 showed Range Sight as missing in the server list on servers without it (it's client-side, so it's now marked as optional there).
- Forge 1.21.11 and 26.x, and NeoForge 1.21.11: Minecraft refused the mod's resource pack info, so key names and messages showed as raw text (like `key.rangesight.beacon`).
- Forge 1.21.1 used loader methods that are being removed.

All 26 files were started in a real game (client and server) and tested automatically before release, including a broken settings file, the Nether, a dimension change, trial spawners and the settings screen.
