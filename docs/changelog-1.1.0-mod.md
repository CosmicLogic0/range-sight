## New
- **Trial spawner and vault ranges** (Minecraft 1.21 and newer): see how close you must be for a trial spawner to wake up (14 blocks) or a vault to open (4 blocks). Key: Numpad 7.
- **Settings screen** on every version: turn each range on or off, change its color, line width and search distance. Open it with **Numpad .** or the **Config** button in the mod list. Hover a button to see what it does.
- **First-time tip:** the first time you join a world, a chat message tells you which key shows every range.

## Improved
- Only the ranges your Minecraft version has get a key and a settings row (no more do-nothing keys, like sculk on 1.16.5 or spawn chunks on 1.21.9 and newer).
- Clearer key names, help text and messages.

## Fixed
- A typo in `config/rangesight.json` reset and overwrote the file. Now your file is kept as `rangesight.json.broken-<date>`, the defaults are used, and you're told in chat.
- Outlines from the dimension or world you just left could stay on screen for a moment.
- Forge 1.21.11 and 26.x, NeoForge 1.21.11: Minecraft refused the mod's resource info, so key names and messages showed as raw text (like `key.rangesight.beacon`).
- Forge 1.16.5 and 1.18.2: Range Sight is now marked as client-only, so servers without it no longer show it as missing.
- Forge 1.21.1: no longer uses loader methods that are being removed.

Tested automatically in a real game (client and server) on every file before release.
