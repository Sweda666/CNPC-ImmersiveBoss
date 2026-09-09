# TODO

## Iron's Spellbooks / Better Combat OBB compatibility

Current status:

- Optional runtime dependencies are configured in `build.gradle`; use `-PwithoutCombatMods` for the combat/magic group or `-PwithoutOptionalMods` for every optional runtime, without moving files in `libs/`.
- Better Combat and Iron's Spellbooks OBB integration is implemented behind mod-loaded Mixin gates.
- `compileJava`, `build`, and `runClient` have passed with both optional mods installed.
- Dedicated-server class-loading smoke tests reach the EULA check with and without the optional mods; no Mixin application errors were reported.
- Game-play attack behavior and the full damage matrix still require manual in-game verification.

### 1. Minimal reproductions

- [ ] Verify vanilla melee damage and `damaged(e).hitboxName` as the baseline.
- [ ] Test Better Combat single-target attacks.
- [ ] Test Better Combat sweep, multiple targets, cone edges, dual wielding, and off-hand attacks.
- [ ] Test Iron's Spellbooks straight projectiles such as Magic Missile and Firebolt.
- [ ] Test Iron's Spellbooks cone spells.
- [ ] Test AOE, shockwave, beam, lightning, and persistent-area spells.
- [ ] Repeat the tests with OBBs entirely inside and extending outside the NPC's vanilla AABB.
- [ ] Test attackable `b` / `d` bones and sensor-only `s` bones separately.
- [ ] Confirm that the client and server hold matching OBB data.

### 2. Temporary diagnostics

Use debug-level logging and remove or disable it after compatibility work is complete.

- [ ] Log entity ID, OBB count, and bone names received by `SyncOBBPacket` on the server.
- [ ] In `MixinEntityNPCInterface.hurt()`, log the `DamageSource` type, causing entity, direct entity, OBB result, and hitbox name.
- [ ] Log the final `hitboxName` passed through the `EventHooks.onNPCDamaged()` redirect.
- [ ] For Better Combat, log the final `TargetFinder` targets, the C2S attack-request entity ID, and the server-side `ServerPlayer.attack()` target.
- [ ] For Iron's Spellbooks, log projectile type, `SpellDamageSource` causing/direct entities, spell ID, and whether `SpellDamageEvent` fires.

### 3. Better Combat compatibility

- [x] Add an optional `compat.bettercombat` integration path.
- [x] Supplement Better Combat's client target search with NPCs whose OBBs intersect the attack volume.
- [x] Use Better Combat's attack OBB directly against this mod's OBBs instead of relying on the NPC's vanilla AABB.
- [x] Preserve Better Combat's original targets and deduplicate entity IDs.
- [x] Validate submitted target IDs on the server instead of trusting the client result alone.
- [x] Carry the actual OBB hit into the path between `ServerPlayer.attack(target)` and the NPC's `hurt()` call.
- [x] Do not call `npc.hurt()` a second time.
- [ ] Preserve Better Combat damage multipliers, sweeping, knockback, dual-wield behavior, and attack cooldowns.

### 4. Iron's Spellbooks compatibility

- [x] Identify Iron's Spellbooks `DamageSource`, projectile, caster, and spell at the NPC `hurt()` entry point.
- [x] For moving projectiles, perform swept OBB detection from the previous position to the current position.
- [x] For AOE attacks, intersect the spell volume with NPC OBBs instead of using a zero-length raycast.
- [x] Handle `ConePart` and other multipart attack volumes separately.
- [x] Classify beam, lightning, shockwave, and persistent-area entities that bypass ordinary projectile collision.
- [x] Inspect compatibility points in `AbstractMagicProjectile.raycastForEntitiesAlongPath`.
- [x] Inspect compatibility points in `AbstractConeProjectile.getSubEntityCollisions`.
- [x] Inspect compatibility points in `AoeEntity.checkHits` and concrete beam/area entities.
- [x] Use `SpellDamageSource` context to retain the spell source and hitbox name after confirming event order.
- [x] Mark projectiles already handled by Iron's Spellbooks so `ProjectileOBBListener` cannot apply duplicate damage.
- [x] Preserve spell damage types, fire/freeze effects, lifesteal, invulnerability frames, and other native effects.

### 5. Mixin and loading safety

- [x] Use `remap = false` and `require = 0` for all third-party mixin targets.
- [x] Register every new mixin in `cnpc_immersiveboss.mixins.json`.
- [x] Put the Better Combat `TargetFinder` mixin under the mixin config's `client` section.
- [x] Prevent dedicated servers from loading Better Combat or Player Animator client classes.
- [x] Use `CnpcMixinConfigPlugin` to apply compatibility mixins only when the target mod is loaded.
- [x] Keep new paths idempotent with `ProjectileOBBListener`, `HitboxDamageListener`, and TACZ compatibility.

### 6. Verification matrix

- [ ] Vanilla melee.
- [ ] Better Combat single-target, sweep, and dual-wield attacks.
- [ ] Iron's Spellbooks projectile, cone, AOE, and beam attacks.
- [ ] Animated OBBs.
- [ ] OBBs extending outside the vanilla AABB.
- [ ] Multiple bones and multi-cube bones.
- [ ] Correct `damaged(e).hitboxName` value.
- [ ] Damage and status effects occur exactly once.
- [x] Dedicated-server launch class-loading smoke test (launch reaches the EULA check; gameplay remains untested).
- [x] Build and launch with every optional compatibility runtime disabled via `-PwithoutOptionalMods`.
- [x] Run `gradlew runClient`, then inspect `run/logs/latest.log` and `run/crash-reports/`.
