# Throw scripting

Throw animations are server authoritative. Call them from a server side
CustomNPCs script with a player `IEntity` target. Durations use ticks (`20`
ticks = one second).

```javascript
function meleeAttack(a) {
    var hitboxes = ["hdb_body", "hdb_head"];
    for (var i in hitboxes) {
        a.npc.activateHitboxDamage(hitboxes[i], 0, 40, 1.0, 1, 10, 0,
            function(source, target) {
                source.cancelAllHitboxDamageWindows();
                source.startThrow(target, "attack_grab", 90, true, "ad", 5);
            });
    }
}
```

The static API supports escape and finish callbacks:

```javascript
var BossAPI = Java.type("sweda.cnpc_immersiveboss.api.ImmersiveBossAPI");
BossAPI.startThrow(npc, target, "attack_grab", 90, true, "ad", 5,
    function(npc, player) { /* escaped */ },
    function(npc, player) { /* finished */ });
```

Struggle modes are `none`, `ad`, `space`, and `shift`. The static API also
accepts numeric modes `0`–`3`. With direct `e.npc.startThrow` calls, use a
string mode when passing two `null` callbacks to avoid Nashorn overload
ambiguity. `stopThrow(target)` cancels and restores the player;
`isThrowActive(target)` checks the current state.

Players cannot attack while controlled by a throw by default. Configure this
in `config/cnpc_immersiveboss-common.toml`:

```toml
[throw]
disableTargetAttack = true
```

Set it to `false` to allow attacks during throws. The server's common config
value controls multiplayer behavior.
