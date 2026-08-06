# AGENTS.md — CNPC-ImmersiveBoss

## What this is
A Minecraft Forge 1.20.1 mod (`cnpc_immersiveboss`) that adds custom boss bars and multi-hitbox collision to [CustomNPCs](https://modrinth.com/mod/custom-npcs) NPCs. Uses Mixin to inject into CustomNPCs, CNPC-Gecko-Addon, and GeckoLib internals.

## Build & run

```bash
./gradlew build          # produces build/libs/cnpc_immersiveboss-*.jar
./gradlew clean build    # clean rebuild
./gradlew runClient      # launch client
./gradlew runServer      # launch server (--nogui)
./gradlew runData        # data generators → src/generated/resources/
```

- **Java 17** required. **Gradle daemon is disabled** (`org.gradle.daemon=false`).
- **`copyIdeResources = true`** — required for IDE run configs to work.
- **`run/` is gitignored** (exact name `run` only). **`run-data/` is NOT gitignored** but contains no tracked files.
- **`src/generated/resources/`** — data gen output, added to source set, not committed.
- **No tests.** `src/test/` is empty. `gameTestServer` run config will crash.

## Architecture

```
sweda.cnpc_immersiveboss/
├── Cnpc_immersiveboss.java        # @Mod entrypoint, registers event listeners + network channel
├── Config.java                    # filler — not wired to any mod behavior (safe to ignore)
├── api/
│   ├── IMixinDataDisplay.java     # getters/setters for custom boss bar fields on DataDisplay
│   ├── IMixinGuiNpcDisplay.java   # exposes DataDisplay from GuiNpcDisplay
│   ├── IMixinNpcDamagedEvent.java # backing interface for MixinNpcDamagedEvent (hitboxName)
│   ├── IOBBHolder.java            # per-entity OBB storage + lastHitboxName for damage attribution
│   ├── ImmersiveBossAPI.java      # static API for CNPC scripts: damageHitbox(), getLastHitboxName()
│   ├── HitboxCollideEvent.java    # extends NpcEvent.CollideEvent with hitbox bone names
│   └── HitboxDamagedEvent.java    # extends NpcEvent.DamagedEvent with hitboxName + source fields
├── mixin/
│   ├── MixinDataDisplay.java      # common: @Overwrites setBossbar, NBT save/load, @Unique fields
│   ├── MixinEntity.java           # common: no-op placeholder — DO NOT add logic here
│   ├── MixinEntityNPCInterface.java  # common: IOBBHolder, static AABB fallback, HEAD hurt() OBB detect, ModifyArg→event
│   ├── MixinNpcDamagedEvent.java   # common: adds getHitboxName/setHitboxName to NpcEvent.DamagedEvent (e.hitboxName in scripts)
│   ├── MixinEntityMove.java       # SOURCE-ONLY — NOT registered in mixins.json
│   ├── MixinEntityPush.java       # SOURCE-ONLY — NOT registered
│   ├── MixinGuiNpcDisplay.java    # client: @Redirect bossbar btn, injects edit btn (ID=20)
│   ├── MixinGameRenderer.java     # client: OBB-precise crosshair entity picking
│   ├── GeoBoneAccessor.java       # client: @Accessor exposes GeoBone.parent
│   └── MixinRenderCustomModel.java  # client: parse .geo.json, hide hitbox bones, sync OBBs
├── client/
│   ├── bossbar/{ClientBossBarData,CustomBossBar}.java
│   ├── gui/{SubGuiCustomBossBar,SubGuiTextureSelector}.java
│   ├── renderer/RenderHandler.java   # RenderGuiOverlayEvent.Pre, draws up to 3 boss bars
│   ├── DebugOBBRenderer.java         # F3+B wireframe debug renderer
│   └── TextureHelper.java            # @Deprecated — unused
├── event/
│   ├── NpcUpdateListener.java        # NpcEvent.* → syncs boss bar data to clients
│   ├── EntityCollisionListener.java  # ServerTickEvent.END: OBB vs OBB/AABB collision + push
│   ├── ProjectileOBBListener.java    # ProjectileImpactEvent → OBB raycasting
│   └── HitboxDamageListener.java     # LivingHurtEvent → cleans up lastHitboxName after damage flow completes
├── hitbox/
│   ├── GeoHitboxDef.java          # POJO with classify() for h[a][d][b|s]_name parsing
│   ├── GeoHitboxParser.java       # Parses .geo.json → hb_/hs_ bones
│   ├── OBB.java                   # Oriented Bounding Box
│   ├── OBBPhysics.java            # SAT collision, raycasting, enclosingAABB, staticFallbackAABB
│   ├── BoneWorldData.java         # Static server-side ConcurrentHashMap<Integer,Map<String,OBB>>
│   ├── ClientHitboxData.java      # Client-side Map<Integer,List<GeoHitboxDef>>
│   └── ServerHitboxData.java      # Server-side Map (populated by SyncHitboxPacket)
├── network/
│   ├── NetworkHandler.java        # Forge SimpleChannel, static INSTANCE; channel cnpc_immersiveboss:main, protocol "1"; packet IDs: 0=SyncCustomBossBar, 1=SyncHitbox, 2=SyncOBB
│   └── packet/
│       ├── SyncCustomBossBarPacket.java  # PLAY_TO_CLIENT: boss bar config per entity
│       ├── SyncHitboxPacket.java         # PLAY_TO_SERVER: sent once per NPC
│       └── SyncOBBPacket.java            # PLAY_TO_SERVER: sent every frame
└── until/                         # typo for "util" — do NOT rename without updating imports
    └── ColorUtils.java
```

## Key flows

### Boss bars
**Server:** `NpcUpdateListener` fires on NPC update/damage/attack/target → reads custom fields from `DataDisplay` via `IMixinDataDisplay` → sends `SyncCustomBossBarPacket` to all players.
**Client:** `SyncCustomBossBarPacket.handle()` → `ClientBossBarData.updateBossBar()` → `RenderHandler` draws `CustomBossBar` instances. **MAX_BARS = 3.**

### Multi-Hitbox (OBB Collision)
1. **Blockbench prefix convention**: `h[a][d][b|s]_name` — `h`=hitbox marker, `a`=visible, `d`=detectable (crosshair), `b`=physical blocking, `s`=sensor (ray only). Examples: `hb_body`, `hab_sword`, `hds_core`, `hadb_shield`.
2. **Client render** (`MixinRenderCustomModel`): Parses `.geo.json` → hides hitbox bones → stores defs in `ClientHitboxData` → sends `SyncHitboxPacket` once per NPC. Then each frame: builds per-bone OBBs from bone world matrices → stores via `IOBBHolder` on entity → sends `SyncOBBPacket` to server.
3. **Server OBBs**: `SyncOBBPacket.handle()` writes OBBs into the entity's `IOBBHolder` (server-side). `ProjectileOBBListener` reads from `IOBBHolder`. `BoneWorldData` is legacy — nothing writes it in live code; only the unregistered dead-code mixins read it.
4. **OBB vs Entity** (`EntityCollisionListener`): `ServerTickEvent.END` — SAT OBB-vs-OBB/OBB-vs-AABB collision + mutual push + fires `HitboxCollideEvent` to CNPC scripts.
5. **OBB vs Projectile** (`ProjectileOBBListener`): `ProjectileImpactEvent` — OBB raycasting via `OBBPhysics.intersectRay()`. If all OBBs miss → cancel event (projectile passes through). Also runs a `ServerTickEvent.END` scan to catch projectiles flying through OBBs outside the default AABB. Physical (`b`) OR detectable (`d`) bones are attackable; sensor-only (`s`) are skipped. Trident quirks: bounce back at 0.1× velocity with a 100-tick anti-re-hit flag (`BOUNCED_TRIDENTS`); the tick scan skips piercing arrows (vanilla pierce already handled damage).
6. **OBB vs Block**: ❌ NOT IMPLEMENTED. `MixinEntityMove` source exists but NOT registered. Do NOT override `Entity.move()`. (`OBBPhysics.obbMove()` block-collision code exists but is never called — dead.)
7. **Static fallback** (`MixinEntityNPCInterface.tick()`): When IOBBHolder empty → computes AABB from `GeoHitboxDef` list. When OBB data IS present → skips entirely (keeps default AABB).
8. **AABB**: `MixinEntity` has an empty `@Inject` on `refreshDimensions()` (keeps default AABB). Only `MixinEntityNPCInterface` sets custom AABB, and only when OBB data is absent.

### Hitbox Damage Attribution
1. **Projectile hits**: `ProjectileOBBListener` records hit bone name on `IOBBHolder` via `setLastHitboxName()`.
2. **Melee/direct hits**: `MixinEntityNPCInterface` injects at **HEAD** of `EntityNPCInterface.hurt()` — **before** CNPC dispatches its `NpcEvent.DamagedEvent` to scripts. Raycasts the attacker's look ray against physical OR detectable OBBs (sensor-only skipped), preferring physical; reach = 6.0 blocks for players, 3.0 for other attackers.
3. **Event injection**: `@ModifyArg` on the same mixin intercepts the `NpcEvent.DamagedEvent` argument passed to `EventHooks.onNPCDamaged()` and copies the hitbox name into the event BEFORE the CNPC script runs.
4. **Script access**: CNPC scripts can read **`e.hitboxName`** directly in the `damaged(e)` handler (no API import needed). For advanced use, `ImmersiveBossAPI.getLastHitboxName(e.npc)` also works (reads from `IOBBHolder`).
5. **Cleanup**: `HitboxDamageListener` (`LivingHurtEvent`, fires after the script) clears `lastHitboxName` to prevent stale values.
6. **Programmatic damage**: `ImmersiveBossAPI.damageHitbox(npc, amount, "hb_head")` order: (1) sets `lastHitboxName` on `IOBBHolder`, (2) fires `HitboxDamagedEvent` via the script engine (scripts can cancel), (3) applies damage via `wrapper.setHealth()`. It does **NOT** call `npc.hurt()`. Also `ImmersiveBossAPI.clearLastHitboxName(npc)` for manual cleanup in `damaged()`.

### UI chain
`MixinGuiNpcDisplay` → button ID=20 → `SubGuiCustomBossBar` → "选择" btn → `SubGuiTextureSelector` (just displays the list). The actual scan lives in `SubGuiCustomBossBar.getAvailableTextures()` — `rm.listResources("textures", *.png)` across EVERY namespace, includes `.png` files under `minecraft:` too. Boss bar PNGs at `src/main/resources/assets/cnpc_immersiveboss/textures/gui/`.

## Mixin details

Mixins defined in `src/main/resources/cnpc_immersiveboss.mixins.json`:
- `"mixins"` (common): `MixinDataDisplay`, `MixinEntity`, `MixinEntityNPCInterface`, `MixinNpcDamagedEvent`
- `"client"`: `MixinGuiNpcDisplay`, `MixinRenderCustomModel`, `MixinGameRenderer`, `GeoBoneAccessor`
- `MixinEntityMove` and `MixinEntityPush` exist as source but are **NOT registered** — dead code.
- `@Overwrite` requires annotation (`requireAnnotations: true`).

**`remap = false`** for ALL injectors targeting CustomNPCs, GeckoAddon, or GeckoLib. **`remap = true`** for vanilla targets (`MixinEntity`, `MixinGameRenderer`).

**Exception**: `MixinGuiNpcDisplay`'s `@Redirect` has **no** `remap` param → defaults to `true`. It's the only CustomNPCs-targeting injection without `remap = false`; it works today via the refmap but is fragile — don't blindly "fix" or copy it.

### Fragile points
- **`MixinDataDisplay.@Overwrite` on `setBossbar(int)`**: If CustomNPCs changes this method, it breaks. Only `@Overwrite` in the project.
- **`MixinGuiNpcDisplay.@Redirect`** target: `addButton(GuiButtonNop)`. If CustomNPCs changes `init()`, this fails silently.
- **`fg.deobf()` is required** for ALL `libs/` dependencies — without it, JARs won't remap to dev mappings and mixins fail.

## Dependencies

- **Forge 1.20.1-47.4.20** / SpongePowered Mixin 0.8.5 / MixinGradle 0.7-SNAPSHOT
- **libs/** (flatDir, all require `fg.deobf()`):
  - `CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar`
  - `geckolib-forge-1.20.1-4.8.4.jar`
  - `CNPC-Gecko-Addon-1.20.1-1.0.14.jar`

## Gotchas

### Critical: never do these
- **🚨 NEVER `setBoundingBox()` with OBB-derived AABB while OBB data is active.** NPC AABB must stay at default size. OBB is the collision shape. Inflated AABB causes floating/sinking during animation. Guard: `if (!cnpc_multihitbox$boneOBBs.isEmpty()) return;` in `MixinEntityNPCInterface.tick()`.
- **🚨 NEVER override `Entity.move()` for OBB block collision.** Breaks all vanilla physics. Use Forge events, not mixin.

### Important constraints
- **`yBodyRot` not `getYRot()`** — NPCs standing still only rotate `yHeadRot`; body facing is `yBodyRot`.
- **Scale formula**: `bbToWorld = size / 80.0` — maps Blockbench units (16/block) with NPC display size (5 = 1x).
- **OBB storage**: `IOBBHolder` (per-entity, populated server-side by `SyncOBBPacket`) is the live store — `ProjectileOBBListener`, `EntityCollisionListener`, `DebugOBBRenderer` all read it. `BoneWorldData` (static map) is legacy — only the unregistered `MixinEntityMove`/`MixinEntityPush` dead code reads it.
- **`SyncHitboxPacket`** = PLAY_TO_SERVER, sent once. **`SyncOBBPacket`** = PLAY_TO_SERVER, sent every frame.
- **Resource pack models**: Server can't read `.geo.json` in resource packs → must sync from client. Server-side fallback via `server.getResourceManager()` for non-resource-pack models.
- **Mod ID consistency**: `cnpc_immersiveboss` must match across ``gradle.properties`` (`mod_id`), `@Mod`, `mods.toml`, `mixins.json`, `pack.mcmeta`, and `ResourceLocation` calls. Project name in `settings.gradle` is `cnpc-immersiveboss` (hyphen) — unrelated.
- **`until` package** is a typo — don't "fix" without updating all imports.
- **`noppes/npcs/EventHooks.class` at repo root** is an untracked decompiled scratch file (from CustomNPCs, used to find the `onNPCDamaged` target) — not build input, not source. Don't commit it or treat it as project code.

### Boss bar quirks
- **Boss bar visibility modes**: 3 = always show, 4 = combat-only (100 tick / 5s timeout).
- **Combat detection is server-driven** — don't attempt client-side `getTarget()`/`getLastHurtByMobTimestamp()`.
- **Localization**: keys under `cnpc_immersiveboss.gui.*`. Lang files: `en_us.json`, `zh_cn.json`, `ru_ru.json`. Always add new text to all three.
- **Dead NPCs don't render bars**: `CustomBossBar.render()` returns 0 when `value <= 0` or texture/dimensions are zero.
- **Mode 3 bars hide past 128 blocks**: `RenderHandler.MAX_RENDER_DISTANCE = 128.0` — mode 3 bars vanish beyond that distance (mode 4 has no distance check).
- **`MinecraftForge.EVENT_BUS` registration order**: `ProjectileOBBListener`, `EntityCollisionListener`, `HitboxDamageListener` in `commonSetup`; `RenderHandler`, `DebugOBBRenderer` in `clientSetup`; `NpcUpdateListener` via `NpcAPI.Instance().events()` in `onLoadComplete`.

### Hitbox naming & parsing
- **Prefix**: `h[a][d][b|s]_name`. `GeoHitboxDef.classify()` does the parsing. Only `a` and `d` allowed in prefix before `b`/`s`. Suffix must be `b` or `s`.
- **Multi-cube bones** get suffixed def names (`hadb_upbody`, `hadb_upbody__1`, ...) — one def per cube.
- **Collision participation**: `EntityCollisionListener` only uses physical (`b`) bones; `ProjectileOBBListener` uses physical OR detectable (`d`); sensor-only (`s`) never collides.
- **Static pivot**: Bone pivots in `.geo.json` are absolute model-space coordinates (Bedrock format). GeckoLib handles the parent-relative conversion internally.
- **Cube rotation**: Parsed from `.geo.json` with Blockbench rotation order: Z → Y → X.
- **Mixin @Unique prefixes are inconsistent**: `cnpc_multihitbox$` (fields in `MixinEntityNPCInterface`), `cnpc_immersiveboss$` (IOBBHolder interface methods, `MixinNpcDamagedEvent`), unprefixed (`MixinDataDisplay` fields). Don't assume one prefix.
