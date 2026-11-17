![Range Sight Plugin](https://raw.githubusercontent.com/CosmicLogic0/range-sight/main/docs/images/header.png)

**Type /range. See the range.**

> ⚠️ **Requires [LogicLib Plugin](https://www.curseforge.com/minecraft/bukkit-plugins/logiclib-plugin).** Put it in your `plugins` folder too. The CurseForge app adds it for you automatically.

Your players keep asking: is the spawner close enough? Does the beacon reach my house? Will the sculk sensor hear me? **Range Sight Plugin lets every player see the invisible ranges with one command**, no mods needed.

This is the **server plugin** version of [Range Sight](https://www.curseforge.com/minecraft/mc-mods/range-sight). Players don't install anything.

---

🧩 **Only you see your outlines.** Each player turns on their own ranges, and the particle outlines are shown only to them, so nobody else's screen gets busy.

## 🔥 What it does

### 🔦 Beacon range
Shows the exact area a beacon's effects reach, from the ground up to the sky, based on how many pyramid levels it has.

### 🌊 Conduit range
Shows the sphere where a conduit gives Conduit Power, sized by its prismarine frame (Minecraft 1.13 and up).

### 🕷️ Spawner range
Shows the 16-block sphere a player has to be inside for a spawner to work, plus the box its mobs spawn in.

### 🚩 Raid range
Shows the 96-block area around an active raid's center. Because the plugin runs on the server, this works for everyone (Minecraft 1.14 and up).

### 👂 Sculk ranges
Shows how far sculk sensors, calibrated sensors and shriekers can hear, and how far a shrieker's Darkness reaches (Minecraft 1.19 and up).

### 🗺️ Spawn chunks
Shows which chunks stay loaded around world spawn (up to Minecraft 1.21.8, since newer versions no longer have spawn chunks).

![Every range, its own color](https://raw.githubusercontent.com/CosmicLogic0/range-sight/main/docs/images/ranges-plugin.png)

### ⚡ Light on your server
Range blocks near each player are looked up every few seconds, not every tick, and only the closest ones are drawn.

## 🎮 Commands
| Command | What it does |
|---|---|
| **/range** | Every range on, or off if some are on |
| **/range beacon** | Beacon range on / off (also: `conduit`, `spawner`, `raid`, `sculk`, `spawnchunks`) |
| **/range all** | Every range on |
| **/range off** | Every range off |
| **/range list** | Shows every range type, its color and whether it's on |

Permission: `rangesight.use` (everyone has it by default).

## 📦 Requirements
- **Paper:** Minecraft 1.12.2, 1.16.5, 1.18.2, 1.19.2, 1.20.1, 1.21.1, 1.21.11, 26.1, 26.2 and 26.3 (one file for all of them)
- **Requires [LogicLib Plugin](https://www.curseforge.com/minecraft/bukkit-plugins/logiclib-plugin)** (shared code for my plugins). The plugin won't start without it.
- Server only. Players don't need to install anything.
- Playing with Forge or NeoForge instead? Get the mod: **[Range Sight](https://www.curseforge.com/minecraft/mc-mods/range-sight)**.

Source and bug reports: https://github.com/CosmicLogic0/range-sight

---

## ❤️ Support the plugin
![Help keep this mod alive](https://raw.githubusercontent.com/CosmicLogic0/range-sight/main/docs/images/donate.png)

Range Sight is free. Donations keep it up to date with fixes, new Minecraft versions and new features. [Donate with PayPal](https://www.paypal.com/paypalme/DylanEMason) - any amount helps. Thank you!

---

## 🎮 The creator's server
**Logic Survival** (`play.logicsurvival.fyi`) is the survival server run by the maker of this plugin.
