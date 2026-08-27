# CNPC-ImmersiveBoss

CNPC-ImmersiveBoss 是一个面向 CustomNPCs 与 CNPC Gecko Addon 的 Forge 模组。它为 GeckoLib NPC 模型提供可随骨骼动画旋转、移动的 OBB 多碰撞箱，并补充自定义 Boss 血条、分部位受伤、碰撞脚本事件和定时碰撞伤害 API。

本文档对应当前源码版本 `0.3.11`。

> 完整分主题教程见 [Wiki](docs/wiki/Home.md)：[安装与快速开始](docs/wiki/Installation-and-Quick-Start.md) · [碰撞箱建模](docs/wiki/Hitbox-Modeling.md) · [战斗与交互](docs/wiki/Combat-and-Interaction.md) · [自定义 Boss 血条](docs/wiki/Custom-Boss-Bar.md) · [脚本 API](docs/wiki/Scripting-API.md) · [常见问题](docs/wiki/Troubleshooting.md)

## 0.3.11 更新 / Update

### 中文

0.3.11 在 0.3.10 的 OBB、Boss 血条和脚本 API 基础上，进一步扩展战斗兼容性与受击反馈：

- 新增 Better Combat 兼容：攻击范围、角度和形状可直接检测 NPC 动画 OBB；客户端补充 OBB 目标，服务端重新验证攻击结果，并保留命中骨骼名称。
- 新增 Iron’s Spellbooks 兼容：支持法术投射物、锥形法术、范围法术、链式闪电等攻击路径的 OBB 检测；移动投射物支持扫掠检测，并将命中部位传递到 `damaged(e).hitboxName`。
- 伤害、暴击和伤害指示粒子可定位到实际命中的 OBB；可通过客户端配置 `damageParticlesFollowHitbox` 关闭。
- Better Combat 和 Iron’s Spellbooks 等兼容模块按需加载；未安装相关模组时，本模组仍可独立运行，并增加了条件 Mixin 与 CNPC 事件回退以提升兼容性。
- 附带 Blockbench 碰撞箱预览插件，可按碰撞箱属性显示彩色 OBB 边框，并支持隐藏碰撞箱和局部坐标轴预览。

### English

Version 0.3.11 builds on the OBB, custom boss bar, and scripting APIs from 0.3.10 with expanded combat compatibility and hit feedback:

- Added Better Combat compatibility: attack ranges, angles, and shapes can directly test animated NPC OBBs. OBB targets are added on the client, validated on the server, and the hitbox name is preserved.
- Added Iron’s Spellbooks compatibility: spell projectiles, cone and area spells, chain lightning, and other native attack paths can use OBB detection. Moving projectiles use swept detection, with the hitbox exposed through `damaged(e).hitboxName`.
- Damage, critical-hit, and damage-indicator particles can appear at the actual hit OBB. This can be disabled with the client option `damageParticlesFollowHitbox`.
- Better Combat and Iron’s Spellbooks integrations are loaded only when available. The mod remains standalone without them, with conditional Mixins and CNPC event fallbacks for better compatibility.
- Added a Blockbench hitbox preview plugin with color-coded OBB outlines, hitbox visibility controls, and local-axis previews.

## 文档导航

| 目标 | 阅读位置 |
| --- | --- |
| 安装模组并完成第一次 OBB 测试 | [运行环境与安装](#运行环境与安装)、[快速开始](#快速开始) |
| 给 Blockbench 模型制作碰撞箱 | [OBB 碰撞箱](#obb-碰撞箱) |
| 理解攻击、弹射物、交互与推挤 | [攻击、交互与碰撞行为](#攻击交互与碰撞行为) |
| 编写分部位受伤或攻击动画脚本 | [CustomNPCs 脚本事件](#customnpcs-脚本事件)、[ImmersiveBossAPI](#immersivebossapi) |
| 配置自定义血条 | [自定义 Boss 血条](#自定义-boss-血条) |
| 排查模型或脚本问题 | [F3+B 调试颜色](#f3b-调试颜色)、[常见问题](#常见问题) |

## 主要功能

- 从 GeckoLib `.geo.json` 模型骨骼中读取多个 OBB 碰撞箱。
- 区分实体碰撞箱、传感器、可检测碰撞箱和可见碰撞箱。
- 支持玩家近战、交互和弹射物命中大型模型的实际 OBB，而不是只依赖 NPC 原点或原版 AABB。
- 在 `damaged(e)` 中通过 `e.hitboxName` 判断受击部位。
- 在 `collide(e)` 中获取发生重叠的双方碰撞箱名称。
- 通过脚本临时激活某个碰撞箱的伤害能力，可配置开始延迟、持续时间、伤害、重复次数、间隔、目标数量和命中回调。
- 可限制大型 NPC 的水平转向速度，让模型、身体朝向和 OBB 平滑跟随目标。
- 可选兼容 TaCZ，使枪械子弹能够命中 NPC 原版 AABB 之外的动画 OBB，并保留 `e.hitboxName`。
- 可选兼容 Better Combat，使近战攻击范围、角度和形状参与动画 OBB 检测，并在服务端验证命中结果。
- 可选兼容 Iron’s Spellbooks，使法术投射物、范围法术和链式闪电等攻击能够命中动画 OBB。
- 让伤害、暴击和伤害指示粒子跟随实际命中的 OBB 位置。
- 使用 `F3+B` 按属性显示不同颜色的 OBB，并将正在重叠的 OBB 标红。
- 使用一张上下分层的 PNG 制作自定义 Boss 血条，支持常显或仅战斗时显示。

## 运行环境与安装

当前项目使用以下环境开发和验证：

| 项目 | 版本 |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.4.20`，兼容范围为 `47.x` |
| Java | `17` |
| GeckoLib | `4.8.4` |
| CustomNPCs | `CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711` |
| CNPC Gecko Addon | 推荐 `CNPC-Gecko-Addon-CE 1.2.2` |

安装步骤：

1. 安装 Minecraft Forge 1.20.1。
2. 将 GeckoLib、CustomNPCs、CNPC Gecko Addon 和本模组的 JAR 放入游戏的 `mods` 目录。
3. 客户端和服务端均安装相同版本的模组及依赖。OBB 定义、动画变换和 Boss 血条会通过网络同步，不能只在单侧安装。
4. 将 NPC 使用的模型、贴图和血条素材放入资源包，并在客户端启用该资源包。

CNPC Gecko Addon 是本模组 OBB 渲染链路的必要依赖。仓库中的 `CNPC MoreRenderSuppot` 仅用于开发环境下的可选兼容测试，不是玩家安装本模组时的必要依赖。

## 快速开始

1. 在 Blockbench 的 GeckoLib 模型中创建一个带 cube 的骨骼，例如 `hdb_body`。
2. 通过 CNPC Gecko Addon 将该 `.geo.json` 模型设置给 NPC。
3. 进入世界并让 NPC 被客户端渲染，本模组会读取碰撞箱定义和实时骨骼变换。
4. 按下 `F3+B`，检查 OBB 的位置、朝向、类型颜色和重叠状态。
5. 攻击 NPC，在其 `damaged(e)` 脚本中读取 `e.hitboxName`。
6. 如需攻击动画中的武器判定，调用 `ImmersiveBossAPI.activateHitboxDamage(...)` 临时激活对应碰撞箱。

## 自定义 Boss 血条

### 显示模式

本模组扩展了 NPC 显示设置中的 Bossbar 选项：

| 值 | GUI 含义 | 行为 |
| --- | --- | --- |
| `0` | 隐藏 | 不显示血条 |
| `1` | 原版常显 | 使用 CustomNPCs 原有血条 |
| `2` | 原版战斗显示 | 使用 CustomNPCs 原有战斗血条 |
| `3` | 显示自定义血条 | 使用本模组素材常显，超过玩家 `128` 格时不渲染 |
| `4` | 战斗时显示自定义血条 | NPC 进入战斗后显示，脱战后隐藏 |

模式 `4` 会在 NPC 有攻击目标、受到伤害、进行近战攻击或触发目标事件时进入战斗状态。连续约 `100 tick`（正常 20 TPS 下约 5 秒）没有目标、受伤或造成伤害记录后，血条会隐藏。

屏幕顶部最多同时渲染 `3` 条自定义血条，第一条距顶部 `10` 像素，血条之间间隔 `6` 像素。模式 `3` 具有明确的 128 格渲染限制；模式 `4` 仍要求对应 NPC 实体已加载到客户端。

### 配置方法

1. 打开 CustomNPCs 的 NPC 显示设置。
2. 将 Bossbar 切换到“显示自定义血条”或“战斗时显示自定义血条”。
3. 点击同一页面新增的“编辑”按钮。
4. 填写纹理、颜色、偏移、缩放和尺寸，点击“保存”。

纹理选择器会列出当前资源管理器中 `textures` 路径下的所有 `.png`。也可以直接输入完整资源位置。

### 血条素材格式

一条自定义血条只使用一张 PNG。图片在垂直方向平均分为上下两半：

```text
+--------------------------------------+
| 上半部分：边框、背景和固定装饰       |
+--------------------------------------+
| 下半部分：生命值填充层               |
+--------------------------------------+
```

- 上半部分先以白色原样绘制。
- 下半部分覆盖在相同位置，并按当前生命百分比从左向右裁切。
- 下半部分会乘以配置的 RGB 颜色，因此建议填充层使用白色或灰度素材。
- 上下两半应具有相同宽度和相同高度，PNG 总高度最好使用偶数。
- 图片透明区域会正常保留，可以制作不规则边框、端帽和装饰。

例如资源包名称空间为 `mypack` 时：

```text
resourcepacks/MyBossPack/
├─ pack.mcmeta
└─ assets/
   └─ mypack/
      └─ textures/
         └─ gui/
            └─ bossbar.png
```

GUI 中填写：

```text
mypack:textures/gui/bossbar.png
```

不要填写磁盘绝对路径，也不要省略名称空间。未写名称空间时，代码会使用 `cnpc_immersiveboss` 作为默认名称空间，通常只适用于模组自己的内置资源。

### 血条参数

| 参数 | 默认值 | 说明 |
| --- | ---: | --- |
| 纹理路径 | 空 | 资源位置，例如 `mypack:textures/gui/bossbar.png` |
| 颜色 | `FFFFFF` | 六位 RGB 十六进制，不带 `#`；空值或非法值按白色处理 |
| X 偏移 | `0` | 生命填充左右端预留的基准宽度，可用于保护端帽 |
| X 缩放 | `0.5` | 水平显示缩放，必须大于 0 |
| Y 缩放 | `0.5` | 垂直显示缩放，必须大于 0 |
| 宽度 | `516` | PNG 的基准宽度参数，必须大于 0 |
| 高度 | `95` | PNG 上下两层合计的基准高度参数，必须大于 0 |

非法或非正数缩放会恢复为 `0.5`，非法或非正数宽高会恢复为 `516 × 95`。

实际显示尺寸为：

```text
显示宽度 = 宽度 × X缩放
整张纹理显示高度 = 高度 × Y缩放
屏幕中单条血条高度 = 整张纹理显示高度 ÷ 2
```

生命填充裁切宽度为：

```text
进度宽度 = (显示宽度 - 2 × X偏移 × X缩放) × 生命百分比
         + X偏移 × X缩放
```

`X偏移` 适合素材左右带固定端帽的情况。普通矩形填充层保持 `0` 即可。

## OBB 碰撞箱

### 命名规则

碰撞箱通过 `.geo.json` 中的骨骼名称识别，推荐使用以下规范格式：

```text
h[a][d][b|s]_名称
```

| 标记 | 含义 |
| --- | --- |
| `h` | 表示这是本模组碰撞箱，必须位于名称开头 |
| `a` | appearance，保留该骨骼 cube 的模型外观；不带 `a` 时会隐藏该碰撞箱骨骼 |
| `d` | detectable，可被准星选择、攻击或交互；大型模型扩展距离必须使用该标记 |
| `b` | blocking，实体碰撞箱，会参与物理推挤 |
| `s` | sensor，传感器，只检测重叠，不产生物理推挤 |
| `_名称` | 自定义部位名称，例如 `_body`、`_head`、`_sword` |

`b` 和 `s` 必须二选一并放在前缀末尾，`a`、`d` 是可选属性。建议固定使用 `a` 在前、`d` 在后的规范顺序，便于模型和脚本维护。

### 全部碰撞箱类型

| 前缀示例 | 可见 | 可检测/交互 | 物理推挤 | 典型用途 |
| --- | --- | --- | --- | --- |
| `hb_body` | 否 | 否 | 是 | 纯实体阻挡范围 |
| `hs_trigger` | 否 | 否 | 否 | 纯重叠触发区 |
| `hab_weapon` | 是 | 否 | 是 | 可见且有实体阻挡的模型部件 |
| `has_wing` | 是 | 否 | 否 | 可见、可重叠但不阻挡的部件 |
| `hdb_core` | 否 | 是 | 是 | 隐藏的实体与受击判定箱 |
| `hds_range` | 否 | 是 | 否 | 隐藏的攻击、交互或触发区域 |
| `hadb_shield` | 是 | 是 | 是 | 可见、可攻击且会阻挡的部件 |
| `hads_sword` | 是 | 是 | 否 | 可见武器或攻击区域，不推开目标 |

这里的“可检测”专指 `d` 标记。为了兼容旧模型，当前近战受击归属和弹射物检测也会接受所有 `b` 型碰撞箱；但是让准星在 NPC 原版 AABB 或实体原点很远时仍能选中大型模型，必须使用带 `d` 的 `hdb_`、`hds_`、`hadb_` 或 `hads_`。

### 模型示例

以下是 `.geo.json` 中 `bones` 数组的简化片段：

```json
{
  "name": "hdb_body",
  "pivot": [0, 12, 0],
  "cubes": [
    {
      "origin": [-4, 0, -2],
      "size": [8, 24, 4],
      "uv": [0, 0]
    }
  ]
},
{
  "name": "hds_sword_range",
  "parent": "right_arm",
  "pivot": [-5, 20, 0],
  "cubes": [
    {
      "origin": [-16, 16, -2],
      "size": [16, 4, 4],
      "pivot": [-5, 20, 0],
      "rotation": [0, 0, -15],
      "uv": [0, 0]
    }
  ]
}
```

`hdb_body` 是隐藏、可检测且会推挤的身体碰撞箱；`hds_sword_range` 是跟随右臂动画、可攻击或交互但不会推开目标的传感器。

制作模型时需要注意：

- 碰撞箱骨骼必须包含至少一个有效 cube，只有骨骼而没有 cube 不会生成 OBB。
- Bedrock/Gecko `.geo.json` 中骨骼 `pivot` 是绝对模型空间坐标，不是相对父骨骼的增量坐标。
- cube 自身旋转按 Blockbench/GeckoLib 的 `Z → Y → X` 顺序应用。
- 骨骼可以跟随父骨骼动画，OBB 会使用实际渲染矩阵同步位置和旋转。
- 资源包可以覆盖模型；切换模型后会按新的模型资源重新建立碰撞箱数据。
- 不要只修改贴图中的视觉大小，碰撞范围由 cube 的 `origin`、`size`、`pivot`、`rotation` 和骨骼动画共同决定。

### 一个骨骼包含多个 cube

同一个碰撞箱骨骼中的所有 cube 都会被解析。第一个 cube 使用原骨骼名，后续 cube 在内部自动命名：

```text
hadb_tail
hadb_tail__1
hadb_tail__2
```

脚本中的普通受击名称和定时伤害激活会归一化为基础骨骼名 `hadb_tail`。因此通常只需要在脚本里判断或传入基础名称，不要依赖 `__1`、`__2`。

## 攻击、交互与碰撞行为

### 玩家攻击和交互

客户端会使用玩家当前的实体触及距离，对带 `d` 的 OBB 进行准星射线检测。服务端收到攻击或交互包后，如果原版基于实体 AABB 的距离检查失败，会改用“玩家眼睛到目标可检测 OBB 的最短距离”再次验证。

因此，大型模型的 NPC 原点即使离玩家很远，只要玩家实际靠近并指向带 `d` 的模型部位，攻击和交互仍可生效。普通实体仍使用原版距离规则，方块命中点比 OBB 更近时也不会被 OBB 穿透抢占。

### 近战和弹射物

- 玩家或其他生物直接伤害 NPC 时，会用攻击者视线对可攻击 OBB 再次射线检测，并把命中的基础骨骼名写入 `e.hitboxName`。
- 弹射物会检测一帧移动线段与 OBB 的相交，包含延伸到 NPC 原版 AABB 之外的 OBB。
- 高速弹射物的路径会分段检查，降低穿过薄碰撞箱而漏判的概率。
- 物理 `b` 箱和带 `d` 的传感器都可以参与受击检测，存在物理箱命中时优先归属物理箱。
- 三叉戟命中 OBB 后会反弹并短暂减速；穿透箭会按“同一支箭对同一个 NPC 只伤害一次”去重。

### 实体重叠与推挤

服务端每 tick 检测 NPC 的 OBB 与附近实体：

- OBB 对普通生物或玩家时，使用对方原版 AABB 检测。
- OBB 对另一个具有 OBB 的 NPC 时，逐对进行 OBB 相交检测。
- `s` 型传感器会正常参与重叠、`collide(e)` 和碰撞伤害，但不会推挤。
- 只有本次相交的双方碰撞箱都为 `b` 型时，双方才会被互相推开。
- 无物理效果的实体、同一载具上的乘客等会被过滤。

## F3+B 调试颜色

按下 `F3+B` 后，本模组会在原版碰撞箱之外绘制 NPC 的 OBB 线框：

| 颜色 | 属性组合 | 对应前缀 |
| --- | --- | --- |
| 白色 | 可推挤，可检测/攻击/交互 | `hdb_`、`hadb_` |
| 蓝色 | 可推挤，不带显式检测标记 | `hb_`、`hab_` |
| 黄色 | 不推挤，可检测/攻击/交互 | `hds_`、`hads_` |
| 绿色 | 不推挤，不带显式检测标记 | `hs_`、`has_` |
| 红色 | 当前正在与其他实体 AABB 或 OBB 重叠 | 覆盖上述任意类型颜色 |

红色是实时重叠状态，不代表该箱子一定会推挤或造成伤害。传感器与玩家重叠时同样会变红。OBB 中心还会显示三条短轴线：红、绿、蓝分别表示其局部 X、Y、Z 方向。

每个 OBB 的正中心会显示该碰撞箱的名称，文本颜色与当前线框颜色一致；发生重叠时，线框和名称会同时变为红色。文本使用全亮调试渲染并始终面向玩家。一个骨骼包含多个 cube 时，后续 OBB 会显示 `__1`、`__2` 等内部名称。

名称显示可在客户端配置文件 `config/cnpc_immersiveboss-client.toml` 中控制，默认开启：

```toml
[debug]
showObbNames = true
```

将其改为 `false` 后，`F3+B` 只绘制 OBB 线框和局部轴，不再绘制名称。

## CustomNPCs 脚本事件

CustomNPCs 1.20.1 使用 Nashorn JavaScript，脚本按 ES5 编写。请使用 `var` 和普通 `function`，不要使用 `let`、`const`、箭头函数、可选链等新语法。

### `damaged(e)`：获取受击碰撞箱

普通玩家近战或弹射物命中时，直接读取 `e.hitboxName`：

```javascript
function damaged(e) {
    var hitboxName = e.hitboxName;

    if (hitboxName == "hdb_head") {
        e.damage = e.damage * 2;
        e.npc.say("命中头部");
    } else if (hitboxName == "hadb_shield") {
        e.setCanceled(true);
    }
}
```

`e.hitboxName` 通常是基础骨骼名。伤害不是从 OBB 进入、射线没有命中任何可攻击 OBB，或当前依赖版本没有解析到对应注入点时，该值可能为 `null`，脚本应保留空值处理。

### `interact(e)`：获取交互碰撞箱

玩家右键交互 NPC 时也可以读取 `e.hitboxName`。服务端会沿玩家视线检测带 `d` 的 OBB，并返回最近命中的基础骨骼名：

```javascript
function interact(e) {
    if (e.hitboxName == "hds_control_panel") {
        e.npc.say("控制面板已启动");
    }
}
```

没有命中可检测 OBB 时，该值为 `null`。需要响应交互的模型部位必须带 `d` 标记，例如 `hds_control_panel` 或 `hadb_switch`。

### `collide(e)`：获取重叠碰撞箱

NPC 脚本的 `collide(e)` 事件增加了以下字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `e.npc` | `ICustomNpc` | 拥有碰撞箱和当前脚本的 NPC |
| `e.entity` | `IEntity` | 与 NPC 重叠的另一实体 |
| `e.hitboxAName` | `String` | 当前 NPC 的 OBB 名称 |
| `e.hitboxBName` | `String` | 对方 OBB 名称；普通实体使用固定值 `AABB` |

示例：

```javascript
function collide(e) {
    if (e.hitboxAName == "hds_warning_range") {
        e.npc.getStoreddata().put("lastDetectedEntity", e.entity.getUUID());
    }
}
```

所有实际重叠的碰撞箱组合都可能触发事件，同一 NPC、实体和骨骼名称组合在同一 tick 内会去重。因此多个 OBB 同时重叠时，一 tick 仍可能收到多个 `collide(e)`。不要在事件中无条件播放声音、刷粒子或写入大量数据，必要时在脚本中自行增加冷却。

## ImmersiveBossAPI

脚本通过 `Java.type` 获取静态 API：

```javascript
var ImmersiveBossAPI = Java.type(
    "sweda.cnpc_immersiveboss.api.ImmersiveBossAPI"
);
```

建议在脚本文件顶部定义一次并复用。

### `damageHitbox`

方法签名：

```text
boolean damageHitbox(ICustomNpc npc, float amount, String hitboxName)
boolean damageHitbox(ICustomNpc npc, ICustomNpc source, float amount, String hitboxName)
```

示例：

```javascript
function trigger(e) {
    var applied = ImmersiveBossAPI.damageHitbox(
        e.npc,
        10.0,
        "hdb_core"
    );

    if (!applied) {
        e.npc.say("伤害被取消或目标无效");
    }
}
```

该方法用于脚本主动指定“哪个部位受到伤害”。它会先以 `HitboxDamagedEvent` 运行目标 NPC 的 `damaged(e)`，脚本可以修改 `e.damage` 或取消事件；未取消时再直接减少 NPC 生命值。

需要注意：这条 API 路径不是一次完整的原版 `LivingEntity.hurt`，不会自动等同于武器、护甲、无敌帧等原版伤害结算；最终生命值通过 CustomNPCs 包装器设置为整数。需要原版伤害规则的持续碰撞攻击应使用下一节的 `activateHitboxDamage`。

带 `source` 的重载只接受另一个 `ICustomNpc` 作为来源，来源可以传 `null`。

### 最近受击名称

方法签名：

```text
String getLastHitboxName(ICustomNpc npc)
void clearLastHitboxName(ICustomNpc npc)
```

普通 `damaged(e)` 应优先直接读取 `e.hitboxName`。这两个方法主要用于高级脚本或兼容逻辑：

```javascript
function damaged(e) {
    var name = ImmersiveBossAPI.getLastHitboxName(e.npc);
    if (name != null) {
        e.npc.say("最后命中: " + name);
        ImmersiveBossAPI.clearLastHitboxName(e.npc);
    }
}
```

### 碰撞箱名称查询

这些方法读取 NPC 当前模型的碰撞箱定义，返回按名称排序、按基础骨骼名去重后的 Java `String[]`：

```text
String[] getHitboxNames(ICustomNpc npc)
String[] getPhysicalHitboxNames(ICustomNpc npc)
String[] getDetectableHitboxNames(ICustomNpc npc)
String[] getSensorHitboxNames(ICustomNpc npc)
String[] getVisibleHitboxNames(ICustomNpc npc)
boolean hasHitbox(ICustomNpc npc, String hitboxName)
```

多 cube 骨骼产生的 `__1`、`__2` 内部名称不会重复出现在结果中。`physical` 对应 `b` 后缀，`sensor` 对应 `s` 后缀，`detectable` 对应 `d` 标记，`visible` 对应 `a` 标记。同一个碰撞箱可以同时出现在多个分类中。

Nashorn 中可以使用 `Java.from(...)` 转成普通 JavaScript 数组：

```javascript
function init(e) {
    var names = Java.from(ImmersiveBossAPI.getHitboxNames(e.npc));
    for (var i = 0; i < names.length; i++) {
        e.npc.say("碰撞箱: " + names[i]);
    }

    if (ImmersiveBossAPI.hasHitbox(e.npc, "hds_sword")) {
        e.npc.getStoreddata().put("hasSwordHitbox", 1);
    }
}
```

如果 NPC 无效、没有自定义模型、模型没有碰撞箱，或定义与实时 OBB 都尚不可用，则返回空数组。查询优先使用当前模型定义；只有定义不可用时才回退到实时 OBB 名称，避免模型切换后返回旧名称。

### `activateHitboxDamage`

该 API 在服务端为一个碰撞箱开启临时伤害窗口。窗口存续期间，只要该 OBB 与玩家、普通生物或其他 CustomNPCs 生物发生重叠，就会尝试造成伤害。

完整方法签名：

```text
boolean activateHitboxDamage(
    ICustomNpc npc,
    String hitboxName,
    int startDelayTicks,
    int durationTicks,
    float damage,
    int repeatCount,
    int repeatIntervalTicks,
    int maxTargets,
    function callback
)
```

参数必须按顺序传入，只能从末尾依次省略：

| 参数 | 必传 | 默认值 | 说明 |
| --- | --- | ---: | --- |
| `npc` | 是 | 无 | 拥有该碰撞箱的 NPC，脚本中通常为 `e.npc` |
| `hitboxName` | 是 | 无 | 碰撞箱基础骨骼名，例如 `hds_sword` |
| `startDelayTicks` | 是 | `0` | 从调用到窗口启用前等待的 tick 数；负数按 0 处理，无需延迟时必须传 `0` |
| `durationTicks` | 否 | `20` | 伤害窗口持续 tick 数，必须大于 0 |
| `damage` | 否 | `1.0` | 每次成功命中的伤害值，必须为有限正数 |
| `repeatCount` | 否 | `1` | 同一个目标在本次窗口内最多成功受伤次数；小于等于 0 表示无限 |
| `repeatIntervalTicks` | 否 | `10` | 同一目标两次成功伤害之间的最短 tick 数；负数按 0 处理 |
| `maxTargets` | 否 | `0` | 本次窗口最多伤害的不同目标数；小于等于 0 表示无限 |
| `callback` | 否 | 无 | 每次成功造成碰撞伤害后执行；参数依次为攻击方 NPC 和受伤目标的 CNPC 实体包装器 |

对应的所有可用重载为：

```text
activateHitboxDamage(npc, hitboxName, startDelayTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount, repeatIntervalTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount, repeatIntervalTicks, maxTargets)
```

上述任一重载都可以在末尾追加 `callback`。除了 `startDelayTicks` 必须传入，其余参数均可使用默认值：

```javascript
ImmersiveBossAPI.activateHitboxDamage(e.npc, "hds_sword", 0);
```

同一组重载也直接提供在 `e.npc` 上。下面两种写法完全等价：

```javascript
ImmersiveBossAPI.activateHitboxDamage(e.npc, "hds_sword", 0, 20, 1);
e.npc.activateHitboxDamage("hds_sword", 0, 20, 1);
```

直接调用同样支持末尾的回调函数和所有完整参数。

这表示 `hds_sword` 在接下来 20 tick 内，对每个目标造成 1 点伤害，每个目标最多成功受伤 1 次，不限制不同目标数量。

完整配置示例：

```javascript
var activated = ImmersiveBossAPI.activateHitboxDamage(
    e.npc,
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
```

这表示：

- 调用后先等待 `6 tick`，等待期间碰撞不会造成伤害。
- 窗口持续 `20 tick`。
- 每次造成 `4` 点伤害。
- 同一个目标最多成功受伤 `3` 次。
- 同一目标每次受伤至少间隔 `10 tick`。
- 只允许第一个成功受到伤害的目标占用本次窗口的名额，后续不同目标不再受伤。
- 每次实际成功造成伤害后都执行回调；同一目标允许循环受伤时，每次成功伤害都会执行一次。

回调是普通 Nashorn 函数，会保留定义它时捕获的脚本变量；额外提供的 `attacker` 和 `target` 都是 CNPC 脚本实体包装器：

```javascript
function openTrackedWindow(npc) {
    var hitCount = 0;
    return ImmersiveBossAPI.activateHitboxDamage(
        npc, "hds_sword", 4, 20, 3.0, 3, 5, 0,
        function(attacker, target) {
            hitCount++;
            attacker.getStoreddata().put("lastTarget", target.getUUID());
            attacker.getStoreddata().put("windowHits", hitCount);
        }
    );
}
```

回调只在 `hurt(...)` 实际返回成功后运行。目标处于无敌帧、伤害事件被取消或伤害没有生效时不会调用。回调抛出异常时会写入服务端日志，不会中断碰撞检测或后续窗口循环。

可以将调用放在攻击动画开始或武器进入有效帧的脚本逻辑中：

```javascript
var ImmersiveBossAPI = Java.type(
    "sweda.cnpc_immersiveboss.api.ImmersiveBossAPI"
);

function openSwordDamageWindow(npc) {
    return ImmersiveBossAPI.activateHitboxDamage(
        npc,
        "hds_sword",
        0,
        8,
        6.0,
        1,
        10,
        2
    );
}
```

调用与计数规则：

- 返回 `true` 表示成功创建窗口；名称无效、持续时间或伤害无效、NPC 已死亡、在客户端调用等情况返回 `false`。
- `startDelayTicks` 从 API 调用所在 tick 开始计时，持续时间从延迟结束、窗口正式启用时才开始计算。
- 延迟等待中的窗口仍可被查询或取消，但不会记录目标、命中次数或重复间隔。
- 再次激活同一个 NPC 的同一基础骨骼，会重置持续时间、目标列表和重复计数，不会与旧窗口叠加。
- 传入 `bone__1` 等多 cube 内部名时会归一化到基础骨骼；建议直接传基础名称。
- `repeatCount` 针对每个目标分别计数，`maxTargets` 针对整个调用周期计数。
- 只有 `hurt(...)` 实际返回成功后，才消耗一次重复次数并占用目标名额。被取消、处于无敌帧或未实际受伤不会占用名额。
- 目标名额按首次成功伤害的先后顺序占用。
- 即使重复间隔为 0，同一个目标每 tick 最多尝试伤害一次。
- NPC 死亡、移除或窗口到期后，相关状态会自动清理。
- 伤害源使用 NPC 的 `mobAttack`，因此会经过原版护甲、无敌帧、Forge 伤害事件和 CustomNPCs 伤害流程。
- 碰撞伤害不依赖 NPC 是否启用了脚本；脚本只负责调用一次来开启窗口。

### 伤害窗口查询与中断

每个窗口都可以独立查询或立即中断：

```text
boolean cancelHitboxDamageWindow(ICustomNpc npc, String hitboxName)
int cancelAllHitboxDamageWindows(ICustomNpc npc)
boolean isHitboxDamageWindowActive(ICustomNpc npc, String hitboxName)
int getHitboxDamageWindowRemainingTicks(ICustomNpc npc, String hitboxName)
String[] getActiveHitboxDamageWindows(ICustomNpc npc)
```

这组 API 也可以直接通过 `e.npc` 调用，此时无需传入第一个 `npc` 参数：

```javascript
e.npc.cancelHitboxDamageWindow("hds_sword");
e.npc.cancelAllHitboxDamageWindows();
e.npc.isHitboxDamageWindowActive("hds_sword");
e.npc.getHitboxDamageWindowRemainingTicks("hds_sword");
var names = Java.from(e.npc.getActiveHitboxDamageWindows());
```

单窗口中断成功时返回 `true`；窗口不存在、已到期或参数无效时返回 `false`。全部中断返回实际取消的活动窗口数量。剩余时间在窗口不存在或已到期时为 `0`，活动窗口名称同样按基础骨骼名排序。

例如，动画提前结束或攻击被打断时关闭剑的伤害窗口：

```javascript
function interruptSwordAttack(npc) {
    if (ImmersiveBossAPI.isHitboxDamageWindowActive(npc, "hds_sword")) {
        return ImmersiveBossAPI.cancelHitboxDamageWindow(npc, "hds_sword");
    }
    return false;
}

function resetAttackState(npc) {
    var canceled = ImmersiveBossAPI.cancelAllHitboxDamageWindows(npc);
    npc.getStoreddata().put("canceledDamageWindows", canceled);
}
```

中断会同时丢弃该窗口记录的目标名额、命中次数和重复间隔状态，但不会影响同一 NPC 的其他窗口，也不会撤销已经造成的伤害。窗口查询和控制只在服务端脚本上下文中有效。

### NPC 转向速度限制

可以为大型 NPC 设置水平转向速度上限，使移动、战斗锁定、空闲观察和脚本旋转都逐步到达目标朝向：

```javascript
function init(e) {
    e.npc.setTurnSpeedLimit(3.0);
}
```

单位为“度/tick”。Minecraft 每秒运行 20 tick，因此 `3.0` 表示每秒最多旋转 `60` 度，完成一次 180 度转身至少需要 3 秒。该限制同时作用于实体移动朝向、模型身体朝向和头部水平朝向，Gecko 模型及其 OBB 碰撞箱会随身体逐步旋转。导航移动会沿当前允许朝向形成转弯轨迹；朝向与路径偏差较大时，移动速度最低降至原速度的 20%，对准后恢复全速。

可用方法为：

```text
boolean setTurnSpeedLimit(float degreesPerTick)
float getTurnSpeedLimit()
boolean hasTurnSpeedLimit()
boolean clearTurnSpeedLimit()
void setRotationImmediate(float rotation)
```

启用限制后，CNPC 原有的 `setRotation(...)` 会变成渐进转向：

```javascript
e.npc.setTurnSpeedLimit(2.0);
e.npc.setRotation(180);          // 以每 tick 最多 2 度逐步转到 180 度
e.npc.setRotationImmediate(90); // 忽略限制，立即转到 90 度
```

规则如下：

- `setTurnSpeedLimit(...)` 只接受有限的非负数；成功时返回 `true`。
- `0` 表示完全冻结水平转向；`180` 及以上实际上不会限制最短角度转向。
- `getTurnSpeedLimit()` 在没有启用限制时返回 `-1`。
- `clearTurnSpeedLimit()` 取消限制并丢弃尚未完成的 `setRotation(...)` 目标；确实取消了限制时返回 `true`。
- `setRotationImmediate(...)` 用于生成、传送或剧情重置，同时对齐实体、身体和头部朝向。
- 目标接近正后方时会锁定一次左转或右转选择，脱离背后扇区后再解除，避免在两个等长方向之间来回抖动。
- 限速值保存在 NPC 的持久化数据中，保存并重新载入世界后仍然有效。
- 限制只改变水平转向速度，不限制头部俯仰角，也不会额外修改 CNPC 的攻击时机。

同一功能也可以通过静态 API 调用，例如 `ImmersiveBossAPI.setTurnSpeedLimit(e.npc, 3.0)`。

## 数据同步与服务端说明

模型碰撞箱定义由客户端资源管理器解析，因此资源包可以覆盖 `.geo.json`。客户端会在模型首次出现或模型变更时向服务端同步定义，并在渲染过程中同步实时动画 OBB 变换。服务端使用这些数据进行实体碰撞、脚本事件、弹射物判定和碰撞伤害。

当前网络协议要求客户端和服务端模组版本一致。用于多人服务器时应确保：

- 两侧安装相同版本的本模组、CustomNPCs、GeckoLib 和 CNPC Gecko Addon。
- 玩家使用的资源包包含正确的模型文件，且资源位置与 NPC 配置一致。
- 模型更换后让客户端重新加载并渲染该 NPC；必要时使用 `F3+T` 重载资源包。
- 不要把只存在于某个客户端的旧模型当作服务端权威配置。

## 常见问题

### `F3+B` 看不到 OBB

- 检查骨骼名是否满足 `h[a][d][b|s]_名称`，尤其是末尾必须为 `b` 或 `s` 后再接下划线。
- 检查骨骼是否至少包含一个有效 cube。
- 确认 NPC 实际使用的是当前编辑的 `.geo.json`，资源包名称空间和路径无误。
- 使用 `F3+T` 重载资源，或让 NPC 离开并重新进入渲染范围。

### 大型模型看得到，但不能攻击或交互

- 给需要被准星选中的部位增加 `d`，例如将 `hs_hand` 改为 `hds_hand`。
- `b` 型旧碰撞箱可参与部分受击归属，但只有 `d` 型 OBB 会扩展客户端准星选择和服务端交互距离。
- 确认玩家眼睛到 OBB 表面的距离没有超过当前实体触及距离，并确认中间没有更近的方块。

### `hs_` 传感器没有推开玩家

这是预期行为。`s` 表示只检测重叠，不做物理推挤。需要推挤时改用 `b`；需要攻击和交互时再增加 `d`。

### 传感器没有触发 `collide(e)` 或不变红

- 确认双方实际发生体积重叠，而不是仅仅视觉接触。
- 检查 NPC 已在客户端渲染并同步 OBB。
- `collide(e)` 需要 NPC 脚本已启用且至少存在一个脚本；红色调试显示不要求脚本开启。
- `noPhysics` 实体和同一载具中的实体可能被过滤。

### 定时碰撞伤害没有生效

- 确认 API 在服务端脚本事件中调用并返回 `true`。
- 传入完整、有效的碰撞箱名称，持续时间和伤害必须大于 0。
- 检查目标是否处于原版无敌帧、是否取消了伤害事件，以及目标是否仍在 OBB 内。
- `maxTargets` 已被先前目标占满时，后续目标不会受伤；重新调用 API 会开始一个全新窗口。

### 自定义血条不显示或显示错误

- Bossbar 模式必须为 `3` 或 `4`，纹理路径不能为空。
- 模式 `4` 只在战斗计时有效时显示；模式 `3` 超过 128 格不会显示。
- 当前最多同时绘制 3 条自定义血条。
- 检查 PNG 是否严格按上下两半组织，并确认宽高、缩放为正数。
- 颜色不带 `#`，例如红色填写 `FF4040`。
- 路径使用 `modid:textures/...png`，不要填写 `assets/` 前缀。

## 开发与构建

项目使用 Gradle 8.8、ForgeGradle 和 Java 17。依赖 JAR 位于仓库的 `libs/`，这些文件是构建所需内容，不要删除或替换为旧版本。

Windows 下常用命令：

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于：

```text
build/libs/cnpc_immersiveboss-<version>.jar
```

仓库没有自动化测试。修改碰撞、网络包或 mixin 后，至少应启动客户端或服务端，并检查 `run/logs/latest.log` 与 `run/crash-reports/`。

### 可选的 CNPC MoreRenderSuppot

开发环境中，如果 `libs/cnpc_morerendersuppot.jar` 存在，Gradle 会将其作为可选运行时依赖加载。它不属于本模组生产 JAR 的强制依赖，也不应直接放入开发实例的 `run/mods`，因为生产 SRG JAR 未经 ForgeGradle 重映射时可能出现 `NoSuchFieldError`。

### 可选的 TaCZ 兼容

TaCZ 不是本模组的强制依赖。检测到 TaCZ 时，对应兼容 mixin 才会启用：枪械射线会纳入 NPC 的动画 OBB，包括超出原版 AABB 的部分，并将命中部位传给 `damaged(e)` 的 `e.hitboxName`。开发环境可使用 `-PtaczDevRuntime` 挂载测试依赖：

```powershell
.\gradlew.bat runClient -PtaczDevRuntime
```

当前兼容代码按 TaCZ `1.1.8-hotfix` API 开发；其他版本应在实际游戏中验证。

### 可选的 Better Combat 与 Iron's Spellbooks 兼容

Better Combat 和 Iron's Spellbooks 不是本模组的强制依赖。检测到对应模组时，兼容 mixin 才会启用，并将其近战、法术投射物、范围法术和链式闪电等攻击路径纳入动画 OBB 检测。未安装这些模组时，本模组仍可独立构建和运行。

开发环境可使用以下参数关闭可选兼容运行时，验证不安装第三方战斗模组时的启动流程：

```powershell
.\gradlew.bat runClient -PwithoutCombatMods
.\gradlew.bat runClient -PwithoutOptionalMods
```

当前兼容代码按 Better Combat `1.9.0` 和 Iron's Spellbooks `1.20.1-3.16.2` 的开发环境进行适配；不同版本应在实际游戏中验证。

## 许可证

本项目使用 MIT 许可证。
