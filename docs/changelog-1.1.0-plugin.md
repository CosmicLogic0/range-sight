## New
- **Visible from far away:** outlines use LogicLib's long-distance particles, so whole ranges show, not just the part within 32 blocks of you. Server owners can turn it off in `config.yml`.
- **Trial spawner and vault ranges** (Minecraft 1.21 and newer): `/range trial` shows how close you must be for a trial spawner to wake up (14 blocks) or a vault to open (4 blocks).
- **`/range help`** explains every command and range type.
- **`/range reload`** and a new **`config.yml`**: search distance, outlines per player, how often to look again, long-distance particles on/off, and every range's color. Permission `rangesight.admin` (operators by default).

## Improved
- A range your server's Minecraft version doesn't have now says so, instead of drawing nothing.
- Your chosen ranges stay on when you log out and back in (until the server restarts).
- Now requires **LogicLib Plugin 0.2.0 or newer**, and says clearly in the console if it's missing or too old.

## Fixed
- Outlines farther than 32 blocks were invisible, so large ranges (beacon, conduit, raid) looked cut off.

Tested automatically on a real Paper server for every supported version before release.
