# CustomNPCs 脚本 API

CustomNPCs 1.20.1 使用 Nashorn JavaScript。示例必须按 ES5 编写：使用 `var` 和普通 `function`，不要使用 `let`、`const`、箭头函数或可选链。

## 事件字段

### 分部位受伤

```javascript
function damaged(e) {
    if (e.hitboxName == "hdb_head") {
        e.damage = e.damage * 2;
    } else if (e.hitboxName == "hadb_shield") {
        e.setCanceled(true);
    }
}
```

### 分部位交互

```javascript
function interact(e) {
    if (e.hitboxName == "hds_control_panel") {
        e.npc.say("控制面板已启动");
    }
}
```

### 碰撞事件

```javascript
function collide(e) {
    if (e.hitboxAName == "hds_warning_range") {
        e.npc.getStoreddata().put("lastEntity", e.entity.getUUID());
    }
}
```

没有对应 OBB 命中时，`e.hitboxName` 可能为 `null`，脚本必须允许空值。

## 获取静态 API

```javascript
var ImmersiveBossAPI = Java.type(
    "sweda.cnpc_immersiveboss.api.ImmersiveBossAPI"
);
```

## 查询碰撞箱

`getHitboxNames`、`getPhysicalHitboxNames`、`getDetectableHitboxNames`、`getSensorHitboxNames` 和 `getVisibleHitboxNames` 返回按基础名去重、排序后的 Java `String[]`。使用 `Java.from(...)` 转成 JavaScript 数组：

```javascript
function init(e) {
    var names = Java.from(ImmersiveBossAPI.getHitboxNames(e.npc));
    if (ImmersiveBossAPI.hasHitbox(e.npc, "hds_sword")) {
        e.npc.getStoreddata().put("hasSword", 1);
    }
}
```

## 动画碰撞伤害

完整参数顺序如下：

```text
activateHitboxDamage(
    npc, hitboxName, startDelayTicks, durationTicks, damage,
    repeatCount, repeatIntervalTicks, maxTargets, callback
)
```

除 `startDelayTicks` 外，其余尾部参数可依次省略。默认值为持续 `20 tick`、伤害 `1.0`、每目标命中 `1` 次、重复间隔 `10 tick`、目标数不限。`repeatCount <= 0` 或 `maxTargets <= 0` 表示无限。

```javascript
function openSwordWindow(npc) {
    return npc.activateHitboxDamage(
        "hds_sword",
        6,
        20,
        4.0,
        3,
        10,
        1,
        function(attacker, target) {
            attacker.say("命中 " + target.getName());
        }
    );
}
```

示例先等待 6 tick，再开启 20 tick 的窗口；每次伤害 4，同一目标最多成功受伤 3 次且间隔至少 10 tick，只接纳首个成功受伤的目标。回调只在 `hurt(...)` 实际成功后执行。

可用控制方法：

```text
cancelHitboxDamageWindow(name)
cancelAllHitboxDamageWindows()
isHitboxDamageWindowActive(name)
getHitboxDamageWindowRemainingTicks(name)
getActiveHitboxDamageWindows()
```

## 主动指定受击部位

`ImmersiveBossAPI.damageHitbox(npc, amount, name)` 会运行可取消、可修改伤害的分部位 `damaged(e)`，随后直接修改生命值。它不等同于完整的原版 `hurt` 结算；需要护甲和无敌帧规则时使用碰撞伤害窗口。

## 转向限速

```javascript
function init(e) {
    e.npc.setTurnSpeedLimit(3.0);
}
```

单位是度/tick。`3.0` 相当于每秒最多 60 度；`0` 完全冻结水平转向。导航前进方向会受当前朝向约束，转向误差较大时速度最低降到 20%，对准后恢复全速；目标位于正后方时会稳定选择一个方向完成转身，不会在左右两边反复切换。`getTurnSpeedLimit()` 无限制时返回 `-1`，`clearTurnSpeedLimit()` 取消限制，`setRotationImmediate(angle)` 可绕过限制立即对齐身体和头部。限速值会随 NPC 保存。

README 的 [ImmersiveBossAPI 章节](https://github.com/Sweda666/CNPC-ImmersiveBoss/blob/ce-addon/README.md#immersivebossapi)列出了全部重载、计数与清理规则。

[上一页：自定义 Boss 血条](Custom-Boss-Bar) · [下一页：常见问题](Troubleshooting)

## 投技动画

投技由服务器控制玩家位置和状态，适合放在 `attack(e)`、`damaged(e)` 或自定义计时器脚本中。Nashorn 只支持 ES5 语法，请使用 `var` 和普通 `function`。调用必须在服务端脚本执行；客户端脚本调用会返回 `false`。

```javascript
var BossAPI = Java.type("sweda.cnpc_immersiveboss.api.ImmersiveBossAPI");

function attack(e) {
    var target = e.npc.getAttackTarget();
    if (target == null || BossAPI.isThrowActive(target)) return;

    BossAPI.startThrow(e.npc, target, "grab", 60, true, "ad", 5,
        function(npc, player) {
            npc.say("玩家挣脱了");
        },
        function(npc, player) {
            npc.say("投技完成");
        });
}
```

完整字符串签名：

```text
startThrow(npc, target, animation, durationTicks,
           returnToStart, struggleMode, difficulty,
           onEscape, onFinish)
```

`animation` 是 NPC GeckoLib 动画名，`durationTicks` 使用 tick（20 tick = 1 秒）。`returnToStart` 为 `true` 时，投技结束或挣脱后恢复玩家开始位置；为 `false` 时保留服务器记录的结束位置。旧的重载省略该参数时默认为 `false`。

`struggleMode` 支持 `none`（不可挣脱）、`ad`（A/D）、`space` 和 `shift`，`difficulty` 必须为正整数，数值越大通常需要更多次输入。也可以使用数字模式：`0` = `none`、`1` = `ad`、`2` = `space`、`3` = `shift`。数字模式示例：

```javascript
// 1 = A/D, difficulty 5, 结束时回到抓取前的位置
BossAPI.startThrow(e.npc, target, "grab", 60, 1, 5, true,
    function(npc, player) { npc.say("挣脱了"); },
    function(npc, player) { npc.say("投技结束"); });
```

`onEscape` 和 `onFinish` 的参数都是 `(npc, player)`，并且在服务器恢复玩家状态之后执行。取消、替换、NPC 或玩家死亡、退出游戏和换维度不会触发这两个回调。一个 NPC 同时只能控制一个投技目标；目标必须是 CNPC 的 `IEntity` 玩家包装器，不能传 UUID 或实体 ID。

投技控制方法：

```javascript
BossAPI.stopThrow(target);     // 取消当前投技并恢复玩家状态
BossAPI.isThrowActive(target); // 查询玩家是否被投技控制
```

如果脚本直接从 NPC 包装器调用，也可以使用 `e.npc.startThrow(target, "grab", 60, ...)`、`e.npc.stopThrow(target)` 和 `e.npc.isThrowActive(target)`。目标应传入 `IEntity` 玩家包装器，而不是 UUID 或实体 ID。

投技动画模型建议包含 `victim_root` 及其玩家身体骨骼；`camera_root` 用于第一人称视角，`camera_second_person` 和 `camera_third_person` 分别用于前、后方第三人称视角。缺少后两个骨骼时，客户端会从 `camera_root` 推导视角。
