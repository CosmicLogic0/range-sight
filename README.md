# Range Sight

See invisible game ranges in your world: beacon effect range, conduit range, spawner activation range,
raid range, sculk sensor and shrieker range, and spawn chunks. Each type has its own color and a key to turn it on or off.
Outlines are drawn in batches, so they don't lag.

- Mod id: `rangesight` · Package: `com.cosmiclogic.rangesight`
- `mod/` – client mod for Forge and NeoForge (multiloader, same layout as LogicLib)
- `plugin/` – Paper companion: `/range` shows outlines only to you (needs LogicLib for Paper)
- `ci/` – automatic tests copied from LogicLib and LogicLib for Paper (`server-test.sh`, `get-paper.sh`)

## Target versions (the 10)
1.12.2 · 1.16.5 · 1.18.2 · 1.19.2 · 1.20.1 · 1.21.1 · 1.21.11 · 26.1.2 · 26.2 · 26.3

| Part | Versions |
|---|---|
| Paper plugin | all 10 (one jar, like LogicLib for Paper) |
| Forge mod | all 10 |
| NeoForge mod | 1.21.1, 1.21.11, 26.1.2, 26.2, 26.3 (NeoForge doesn't exist for the older ones) |

Spawn chunks were removed by Mojang in 1.21.9, so that range type only appears on 1.12.2 to 1.21.1.

## Testing
Every version must pass the automatic tests before upload (GitHub Actions):
server start test (`ci/server-test.sh`), headless client test for the mod, and Paper start test on all 10 versions.
