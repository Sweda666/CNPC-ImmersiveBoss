# 战斗与交互

## 玩家选择与交互

客户端准星会检测带 `d` 的 OBB。若 OBB 比 NPC 原版 AABB 更近，且没有更近的方块遮挡，准星目标会切换到该 NPC。服务端收到攻击或交互包后会再次验证玩家视线和触及距离，因此大型模型即使远离实体原点，也能从实际模型表面被攻击或交互。

`damaged(e)` 与 `interact(e)` 都可读取命中的基础骨骼名：

```javascript
function interact(e) {
    if (e.hitboxName == "hds_switch") {
        e.npc.say("开关已触发");
    }
}
```

## 近战与弹射物

- 直接伤害 NPC 时，模组以攻击者视线对 OBB 再次射线检测并设置 `e.hitboxName`。
- 原版弹射物按一帧的移动线段检测 OBB；高速弹射物会分段检测。
- 超出 NPC 原版 AABB 的 OBB 也会被服务端扫描。
- 三叉戟命中后反弹并短暂减速；穿透箭对同一个 NPC 去重。
- 安装 TaCZ 后会按需启用兼容 mixin，使其枪械射线包含动画 OBB，并向 `damaged(e)` 传递部位名。当前实现按 TaCZ `1.1.8-hotfix` API 验证。

为了兼容旧模型，受击检测接受物理 `b` 箱以及带 `d` 的传感器；需要扩展准星和交互距离时仍必须显式添加 `d`。

## 实体重叠与推挤

服务端每 tick 检测 NPC OBB 与附近实体：普通实体使用自己的 AABB，另一个 OBB NPC 则逐对比较 OBB。传感器 `s` 会触发重叠事件但不推挤；只有相交双方都是物理 `b` 箱时才互相推开。

`collide(e)` 提供：

| 字段 | 含义 |
| --- | --- |
| `e.entity` | 与 NPC 重叠的实体 |
| `e.hitboxAName` | 当前 NPC 的 OBB 名称 |
| `e.hitboxBName` | 对方 OBB 名称；普通实体为 `AABB` |

同一 tick 内相同 NPC、实体和骨骼组合会去重，但多个 OBB 同时相交仍可能产生多个事件。声音、粒子或高开销脚本应自行增加冷却。

## 动画攻击

对剑、尾巴等动画骨骼，使用 `activateHitboxDamage` 在有效帧打开伤害窗口。该窗口通过 `mobAttack` 伤害源结算，会经过护甲、无敌帧和事件取消；窗口可以延迟启动、重复命中、限制目标数并在命中后回调。完整示例见[脚本 API](Scripting-API.md)。

[上一页：碰撞箱建模](Hitbox-Modeling.md) · [下一页：自定义 Boss 血条](Custom-Boss-Bar.md)
