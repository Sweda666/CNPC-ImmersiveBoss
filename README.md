# CNPC-ImmersiveBoss

CNPC-ImmersiveBoss is a Forge mod targeting **CustomNPCs** and **CNPC Gecko Addon**. It provides OBB (Oriented Bounding Box) multi-collision boxes for GeckoLib NPC models that rotate and move with bone animations, and supplements custom boss bars, per-hitbox damage, collision script events, and timed hitbox damage API.

CNPC-ImmersiveBoss 是一个面向 **CustomNPCs** 与 **CNPC Gecko Addon** 的 Forge 模组。它为 GeckoLib NPC 模型提供可随骨骼动画旋转、移动的 OBB（有向包围盒）多碰撞箱，并补充自定义 Boss 血条、分部位受伤、碰撞脚本事件和定时碰撞伤害 API。

本文档对应当前源码版本 `0.5.8`。
This documentation corresponds to source version `0.5.8`.

> Full topic tutorials are available in the [Wiki](docs/wiki/Home.md): [Installation and Quick Start](docs/wiki/Installation-and-Quick-Start.md) · [Hitbox Modeling](docs/wiki/Hitbox-Modeling.md) · [Combat and Interaction](docs/wiki/Combat-and-Interaction.md) · [Custom Boss Bar](docs/wiki/Custom-Boss-Bar.md) · [Scripting API (including Throws)](docs/wiki/Scripting-API.md) · [Troubleshooting](docs/wiki/Troubleshooting.md)
> 完整分主题教程见 [Wiki](docs/wiki/Home.md)：[安装与快速开始](docs/wiki/Installation-and-Quick-Start.md) · [碰撞箱建模](docs/wiki/Hitbox-Modeling.md) · [战斗与交互](docs/wiki/Combat-and-Interaction.md) · [自定义 Boss 血条](docs/wiki/Custom-Boss-Bar.md) · [脚本 API（含投技）](docs/wiki/Scripting-API.md) · [常见问题](docs/wiki/Troubleshooting.md)

## 0.5.8 更新 / Update

Version 0.5.8 adds server-authoritative throw animations and completes the script-facing combat API. The throw boolean is placed before `struggleMode` and defaults to `false` in older overloads:

版本 0.5.8 新增了服务端权威的投技动画，并完善了面向脚本的战斗 API。投技布尔参数位于 `struggleMode` 之前，在旧版重载中默认值为 `false`：

```javascript
ImmersiveBossAPI.startThrow(npc, target, "grab", 60,
    true, "ad", 5, onEscape, onFinish);
```

`true` restores the player's captured position when the throw ends or is escaped; `false` leaves the player at the server-controlled ending position. The server also suppresses stale client movement packets while a throw is active, preventing vanilla's illegal-movement disconnect during scripted throws. Numeric struggle modes are `0` (none), `1` (A/D), `2` (space), and `3` (shift).

`true` 在投技结束或挣脱时恢复玩家被捕获的位置；`false` 则让玩家停留在服务端控制的结束位置。服务端还会在投技激活期间抑制过期的客户端移动数据包，防止原版在脚本投技期间因非法移动而断开连接。数字挣扎模式为 `0`（无）、`1`（A/D）、`2`（空格）和 `3`（Shift）。

### 中文 / Chinese

0.5.8 在 OBB、Boss 血条和脚本 API 基础上，进一步扩展战斗兼容性与受击反馈，并加入可由脚本驱动的投技动画：

Version 0.5.8 builds on the OBB, custom boss bar, and scripting APIs with expanded combat compatibility, hit feedback, and server-authoritative throw animations:

- 新增 Better Combat 兼容：攻击范围、角度和形状可直接检测 NPC 动画 OBB；客户端补充 OBB 目标，服务端重新验证攻击结果，并保留命中骨骼名称。
- Added Better Combat compatibility: attack ranges, angles, and shapes can directly test animated NPC OBBs. OBB targets are added on the client, validated on the server, and the hitbox name is preserved.
- 新增 Iron's Spellbooks 兼容：支持法术投射物、锥形法术、范围法术、链式闪电等攻击路径的 OBB 检测；移动投射物支持扫掠检测，并将命中部位传递到 `damaged(e).hitboxName`。
- Added Iron's Spellbooks compatibility: spell projectiles, cone and area spells, chain lightning, and other native attack paths can use OBB detection. Moving projectiles use swept detection, with the hitbox exposed through `damaged(e).hitboxName`.
- 伤害、暴击和伤害指示粒子可定位到实际命中的 OBB；可通过客户端配置 `damageParticlesFollowHitbox` 关闭。
- Damage, critical-hit, and damage-indicator particles can appear at the actual hit OBB. This can be disabled with the client option `damageParticlesFollowHitbox`.
- Better Combat 和 Iron's Spellbooks 等兼容模块按需加载；未安装相关模组时，本模组仍可独立运行，并增加了条件 Mixin 与 CNPC 事件回退以提升兼容性。
- Better Combat and Iron's Spellbooks integrations are loaded only when available. The mod remains standalone without them, with conditional Mixins and CNPC event fallbacks for better compatibility.
- 附带 Blockbench 碰撞箱预览插件，可按碰撞箱属性显示彩色 OBB 边框，并支持隐藏碰撞箱和局部坐标轴预览。
- Added a Blockbench hitbox preview plugin with color-coded OBB outlines, hitbox visibility controls, and local-axis previews.
- 新增服务端权威投技 API：支持抓取动画、A/D 或单键挣扎、结束/挣脱回调和可选的起始位置恢复；NPC 或玩家死亡、退出和换维度时会自动清理状态。
- Added scriptable throw animations with struggle modes, callbacks, optional return-to-start placement, and cleanup on death, disconnect, or dimension changes.

## 文档导航 / Documentation Navigation

| 目标 / Goal | 阅读位置 / Reading Location |
| :--- | :--- |
| 安装模组并完成第一次 OBB 测试 / Install the mod and complete the first OBB test | [运行环境与安装 / Runtime Environment & Installation](#运行环境与安装--runtime-environment--installation), [快速开始 / Quick Start](#快速开始--quick-start) |
| 给 Blockbench 模型制作碰撞箱 / Create collision boxes for a Blockbench model | [OBB 碰撞箱 / OBB Collision Boxes](#obb-碰撞箱--obb-collision-boxes) |
| 理解攻击、弹射物、交互与推挤 / Understand attacks, projectiles, interaction, and pushing | [攻击、交互与碰撞行为 / Attacks, Interaction & Collision](#攻击交互与碰撞行为--attacks-interaction--collision) |
| 编写分部位受伤或攻击动画脚本 / Write part-based damage or attack animation scripts | [CustomNPCs 脚本事件 / CustomNPCs Script Events](#customnpcs-脚本事件--customnpcs-script-events), [ImmersiveBossAPI](#immersivebossapi) |
| 配置自定义血条 / Configure custom boss bars | [自定义 Boss 血条 / Custom Boss Bar](#自定义-boss-血条--custom-boss-bar) |
| 排查模型或脚本问题 / Troubleshoot model or script issues | [F3+B 调试颜色 / F3+B Debug Colors](#f3b-调试颜色--f3b-debug-colors), [常见问题 / Troubleshooting](#常见问题--troubleshooting) |

## 主要功能 / Key Features

- 从 GeckoLib `.geo.json` 模型骨骼中读取多个 OBB 碰撞箱。
  Read multiple OBB collision boxes from GeckoLib `.geo.json` model bones.
- 区分实体碰撞箱、传感器、可检测碰撞箱和可见碰撞箱。
  Distinguish between entity collision boxes, sensors, detectable collision boxes, and visible collision boxes.
- 支持玩家近战、交互和弹射物命中大型模型的实际 OBB，而不是只依赖 NPC 原点或原版 AABB。
  Support player melee, interaction, and projectile hits on the actual OBBs of large models, rather than relying solely on the NPC origin or vanilla AABB.
- 在 `damaged(e)` 中通过 `e.hitboxName` 判断受击部位。
  Determine the hit part via `e.hitboxName` in `damaged(e)`.
- 在 `collide(e)` 中获取发生重叠的双方碰撞箱名称。
  Obtain the names of both colliding boxes in `collide(e)`.
- 通过脚本临时激活某个碰撞箱的伤害能力，可配置开始延迟、持续时间、伤害、重复次数、间隔、目标数量和命中回调。
  Temporarily activate the damage capability of a specific collision box via script, configurable with start delay, duration, damage, repeat count, interval, target count, and hit callback.
- 可限制大型 NPC 的水平转向速度，让模型、身体朝向和 OBB 平滑跟随目标。
  Limit the horizontal turn speed of large NPCs to allow the model, body orientation, and OBBs to smoothly follow the target.
- 可选兼容 TaCZ，使枪械子弹能够命中 NPC 原版 AABB 之外的动画 OBB，并保留 `e.hitboxName`。
  Optional TaCZ compatibility, allowing gun bullets to hit animated OBBs outside the NPC's vanilla AABB and preserving `e.hitboxName`.
- 可选兼容 Better Combat，使近战攻击范围、角度和形状参与动画 OBB 检测，并在服务端验证命中结果。
  Optional Better Combat compatibility, allowing melee attack ranges, angles, and shapes to participate in animated OBB detection and validating hit results on the server.
- 可选兼容 Iron's Spellbooks，使法术投射物、范围法术和链式闪电等攻击能够命中动画 OBB。
  Optional Iron's Spellbooks compatibility, allowing spell projectiles, area spells, and chain lightning to hit animated OBBs.
- 让伤害、暴击和伤害指示粒子跟随实际命中的 OBB 位置。
  Make damage, critical hit, and damage indicator particles follow the actual hit OBB position.
- 使用 `F3+B` 按属性显示不同颜色的 OBB，并将正在重叠的 OBB 标红。
  Use `F3+B` to display OBBs in different colors by property and highlight overlapping OBBs in red.
- 使用一张上下分层的 PNG 制作自定义 Boss 血条，支持常显或仅战斗时显示。
  Create a custom Boss bar using a single vertically split PNG, supporting always-on or combat-only display.

## 运行环境与安装 / Runtime Environment & Installation

当前项目使用以下环境开发和验证：
The current project is developed and verified with the following environment:

| 项目 / Item | 版本 / Version |
| :--- | :--- |
| Minecraft | `1.20.1` |
| Forge | `47.4.20`, compatible with `47.x` |
| Java | `17` |
| GeckoLib | `4.8.4` |
| CustomNPCs | `CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711` |
| CNPC Gecko Addon | 推荐 / Recommended `CNPC-Gecko-Addon-CE 1.2.2` |

安装步骤：
Installation steps:

1. 安装 Minecraft Forge 1.20.1。
   Install Minecraft Forge 1.20.1.
2. 将 GeckoLib、CustomNPCs、CNPC Gecko Addon 和本模组的 JAR 放入游戏的 `mods` 目录。
   Place the JARs for GeckoLib, CustomNPCs, CNPC Gecko Addon, and this mod into the game's `mods` directory.
3. 客户端和服务端均安装相同版本的模组及依赖。OBB 定义、动画变换和 Boss 血条会通过网络同步，不能只在单侧安装。
   Install the same versions of the mods and dependencies on both client and server. OBB definitions, animation transforms, and Boss bars are synced over the network and cannot be installed on only one side.
4. 将 NPC 使用的模型、贴图和血条素材放入资源包，并在客户端启用该资源包。
   Place the models, textures, and boss bar assets used by the NPC into a resource pack and enable that resource pack on the client.

CNPC Gecko Addon 是本模组 OBB 渲染链路的必要依赖。仓库中的 `CNPC MoreRenderSuppot` 仅用于开发环境下的可选兼容测试，不是玩家安装本模组时的必要依赖。
CNPC Gecko Addon is a required dependency for this mod's OBB rendering pipeline. `CNPC MoreRenderSuppot` in the repository is only for optional compatibility testing in the development environment and is not a required dependency for players installing this mod.

## 快速开始 / Quick Start

1. 在 Blockbench 的 GeckoLib 模型中创建一个带 cube 的骨骼，例如 `hdb_body`。
   Create a bone with a cube in a Blockbench GeckoLib model, e.g., `hdb_body`.
2. 通过 CNPC Gecko Addon 将该 `.geo.json` 模型设置给 NPC。
   Assign the `.geo.json` model to the NPC via CNPC Gecko Addon.
3. 进入世界并让 NPC 被客户端渲染，本模组会读取碰撞箱定义和实时骨骼变换。
   Enter the world and let the NPC be rendered by the client; this mod will read the collision box definitions and real-time bone transforms.
4. 按下 `F3+B`，检查 OBB 的位置、朝向、类型颜色和重叠状态。
   Press `F3+B` to check the OBB position, orientation, type color, and overlap status.
5. 攻击 NPC，在其 `damaged(e)` 脚本中读取 `e.hitboxName`。
   Attack the NPC and read `e.hitboxName` in its `damaged(e)` script.
6. 如需攻击动画中的武器判定，调用 `ImmersiveBossAPI.activateHitboxDamage(...)` 临时激活对应碰撞箱。
   To enable weapon hit detection during an attack animation, call `ImmersiveBossAPI.activateHitboxDamage(...)` to temporarily activate the corresponding hitbox.

## 自定义 Boss 血条 / Custom Boss Bar

### 显示模式 / Display Modes

本模组扩展了 NPC 显示设置中的 Bossbar 选项：
This mod extends the Bossbar option in the NPC Display settings:

| 值 / Value | GUI 含义 / GUI Meaning | 行为 / Behavior |
| :--- | :--- | :--- |
| `0` | 隐藏 / Hide | 不显示血条 / No bar displayed |
| `1` | 原版常显 / Vanilla Always | 使用 CustomNPCs 原有血条 / Use original CustomNPCs bar |
| `2` | 原版战斗显示 / Vanilla Combat | 使用 CustomNPCs 原有战斗血条 / Use original CustomNPCs combat bar |
| `3` | 显示自定义血条 / Custom Always | 使用本模组素材常显，超过玩家 `128` 格时不渲染 / Use mod assets, hides beyond 128 blocks |
| `4` | 战斗时显示自定义血条 / Custom Combat | NPC 进入战斗后显示，脱战后隐藏 / Shows in combat, hides when out of combat |

模式 `4` 会在 NPC 有攻击目标、受到伤害、进行近战攻击或触发目标事件时进入战斗状态。连续约 `100 tick`（正常 20 TPS 下约 5 秒）没有目标、受伤或造成伤害记录后，血条会隐藏。
Mode `4` enters combat state when the NPC has an attack target, takes damage, performs a melee attack, or triggers a target event. The bar hides after approximately `100 ticks` (about 5 seconds at normal 20 TPS) without a target, damage taken, or damage dealt record.

屏幕顶部最多同时渲染 `3` 条自定义血条，第一条距顶部 `10` 像素，血条之间间隔 `6` 像素。模式 `3` 具有明确的 128 格渲染限制；模式 `4` 仍要求对应 NPC 实体已加载到客户端。
A maximum of `3` custom boss bars can be rendered at the top of the screen simultaneously. The first bar is `10` pixels from the top, with `6` pixels spacing between bars. Mode `3` has a clear 128-block rendering limit; Mode `4` still requires the corresponding NPC entity to be loaded on the client.

### 配置方法 / Configuration Method

1. 打开 CustomNPCs 的 NPC 显示设置。
   Open the NPC Display settings in CustomNPCs.
2. 将 Bossbar 切换到"显示自定义血条"或"战斗时显示自定义血条"。
   Switch Bossbar to "Show Custom Bar" or "Show Custom Bar in Combat".
3. 点击同一页面新增的"编辑"按钮。
   Click the new "Edit" button on the same page.
4. 填写纹理、颜色、偏移、缩放和尺寸，点击"保存"。
   Fill in the texture, color, offset, scale, and dimensions, then click "Save".

纹理选择器会列出当前资源管理器中 `textures` 路径下的所有 `.png`。也可以直接输入完整资源位置。
The texture selector lists all `.png` files under the `textures` path in the current resource manager. You can also enter the full resource location directly.

### 血条素材格式 / Bar Asset Format

一条自定义血条只使用一张 PNG。图片在垂直方向平均分为上下两半：
A custom boss bar uses a single PNG. The image is vertically divided into two equal halves:

```text
+--------------------------------------+
| 上半部分：边框、背景和固定装饰 / Top: Border, background, and fixed decoration |
+--------------------------------------+
| 下半部分：生命值填充层 / Bottom: Health fill layer |
+--------------------------------------+
```

- 上半部分先以白色原样绘制。
  The top half is drawn first in white as-is.
- 下半部分覆盖在相同位置，并按当前生命百分比从左向右裁切。
  The bottom half is overlaid at the same position and cropped from left to right according to the current health percentage.
- 下半部分会乘以配置的 RGB 颜色，因此建议填充层使用白色或灰度素材。
  The bottom half is multiplied by the configured RGB color, so it is recommended to use white or grayscale assets for the fill layer.
- 上下两半应具有相同宽度和相同高度，PNG 总高度最好使用偶数。
  Both halves should have the same width and height, and the total PNG height should preferably be an even number.
- 图片透明区域会正常保留，可以制作不规则边框、端帽和装饰。
  Transparent areas in the image are preserved, allowing for irregular borders, end caps, and decorations.

例如资源包名称空间为 `mypack` 时：
For example, if the resource pack namespace is `mypack`:

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
Enter in the GUI:

```text
mypack:textures/gui/bossbar.png
```

不要填写磁盘绝对路径，也不要省略名称空间。未写名称空间时，代码会使用 `cnpc_immersiveboss` 作为默认名称空间，通常只适用于模组自己的内置资源。
Do not enter an absolute disk path, and do not omit the namespace. If no namespace is written, the code will use `cnpc_immersiveboss` as the default namespace, which usually only applies to the mod's own built-in resources.

### 血条参数 / Bar Parameters

| 参数 / Parameter | 默认值 / Default | 说明 / Description |
| :--- | :--- | :--- |
| 纹理路径 / Texture Path | 空 / Empty | 资源位置，例如 `mypack:textures/gui/bossbar.png` / Resource location, e.g., `mypack:textures/gui/bossbar.png` |
| 颜色 / Color | `FFFFFF` | 六位 RGB 十六进制，不带 `#`；空值或非法值按白色处理 / 6-digit RGB hex, no `#`; empty or invalid values treated as white |
| X 偏移 / X Offset | `0` | 生命填充左右端预留的基准宽度，可用于保护端帽 / Base width reserved for left/right ends of fill, useful for protecting end caps |
| X 缩放 / X Scale | `0.5` | 水平显示缩放，必须大于 0 / Horizontal display scale, must be > 0 |
| Y 缩放 / Y Scale | `0.5` | 垂直显示缩放，必须大于 0 / Vertical display scale, must be > 0 |
| 宽度 / Width | `516` | PNG 的基准宽度参数，必须大于 0 / Base width parameter of PNG, must be > 0 |
| 高度 / Height | `95` | PNG 上下两层合计的基准高度参数，必须大于 0 / Base height parameter of combined PNG layers, must be > 0 |

非法或非正数缩放会恢复为 `0.5`，非法或非正数宽高会恢复为 `516 × 95`。
Invalid or non-positive scales will revert to `0.5`, and invalid or non-positive dimensions will revert to `516 x 95`.

实际显示尺寸为：
Actual display dimensions:

```text
显示宽度 / Display Width = 宽度 × X缩放 / Width x X Scale
整张纹理显示高度 / Full Texture Display Height = 高度 × Y缩放 / Height x Y Scale
屏幕中单条血条高度 / Single Bar Height on Screen = 整张纹理显示高度 / 2 / Full Texture Display Height / 2
```

生命填充裁切宽度为：
Health fill crop width:

```text
进度宽度 / Progress Width = (显示宽度 - 2 x X偏移 × X缩放) × 生命百分比 + X偏移 × X缩放
= (Display Width - 2 x X Offset x X Scale) x Health % + X Offset x X Scale
```

`X偏移` 适合素材左右带固定端帽的情况。普通矩形填充层保持 `0` 即可。
`X Offset` is suitable for assets with fixed end caps on the left and right. Keep it `0` for a standard rectangular fill layer.

## OBB 碰撞箱 / OBB Collision Boxes

### 命名规则 / Naming Convention

碰撞箱通过 `.geo.json` 中的骨骼名称识别，推荐使用以下规范格式：
Collision boxes are identified by bone names in `.geo.json`. The recommended format is:

```text
h[a][d][b|s]_名称 / h[a][d][b|s]_name
```

| 标记 / Flag | 含义 / Meaning |
| :--- | :--- |
| `h` | 表示这是本模组碰撞箱，必须位于名称开头 / Indicates this is a mod collision box, must be at the start |
| `a` | appearance，保留该骨骼 cube 的模型外观；不带 `a` 时会隐藏该碰撞箱骨骼 / appearance, keeps the model appearance of the bone's cube; hides the bone if `a` is absent |
| `d` | detectable，可被准星选择、攻击或交互；大型模型扩展距离必须使用该标记 / detectable, can be selected by crosshair, attacked, or interacted with; required for extended range on large models |
| `b` | blocking，实体碰撞箱，会参与物理推挤 / blocking, entity collision box, participates in physical pushing |
| `s` | sensor，传感器，只检测重叠，不产生物理推挤 / sensor, detects overlap only, no physical pushing |
| `_名称` / `_name` | 自定义部位名称，例如 `_body`、`_head`、`_sword` / Custom part name, e.g., `_body`, `_head`, `_sword` |

`b` 和 `s` 必须二选一并放在前缀末尾，`a`、`d` 是可选属性。建议固定使用 `a` 在前、`d` 在后的规范顺序，便于模型和脚本维护。
`b` and `s` are mutually exclusive and must be at the end of the prefix; `a` and `d` are optional attributes. It is recommended to consistently use the order `a` then `d` for easier model and script maintenance.

### 全部碰撞箱类型 / All Collision Box Types

| 前缀示例 / Prefix Example | 可见 / Visible | 可检测/交互 / Detectable/Interact | 物理推挤 / Physical Push | 典型用途 / Typical Use |
| :--- | :--- | :--- | :--- | :--- |
| `hb_body` | 否 / No | 否 / No | 是 / Yes | 纯实体阻挡范围 / Pure entity blocking volume |
| `hs_trigger` | 否 / No | 否 / No | 否 / No | 纯重叠触发区 / Pure overlap trigger zone |
| `hab_weapon` | 是 / Yes | 否 / No | 是 / Yes | 可见且有实体阻挡的模型部件 / Visible model part with entity blocking |
| `has_wing` | 是 / Yes | 否 / No | 否 / No | 可见、可重叠但不阻挡的部件 / Visible, overlap-able but non-blocking part |
| `hdb_core` | 否 / No | 是 / Yes | 是 / Yes | 隐藏的实体与受击判定箱 / Hidden entity and hitbox box |
| `hds_range` | 否 / No | 是 / Yes | 否 / No | 隐藏的攻击、交互或触发区域 / Hidden attack, interaction, or trigger area |
| `hadb_shield` | 是 / Yes | 是 / Yes | 是 / Yes | 可见、可攻击且会阻挡的部件 / Visible, attackable, and blocking part |
| `hads_sword` | 是 / Yes | 是 / Yes | 否 / No | 可见武器或攻击区域，不推开目标 / Visible weapon or attack area, non-blocking |

这里的"可检测"专指 `d` 标记。为了兼容旧模型，当前近战受击归属和弹射物检测也会接受所有 `b` 型碰撞箱；但是让准星在 NPC 原版 AABB 或实体原点很远时仍能选中大型模型，必须使用带 `d` 的 `hdb_`、`hds_`、`hadb_` 或 `hads_`。
The term "detectable" specifically refers to the `d` flag. For compatibility with older models, current melee hit attribution and projectile detection also accept all `b` type hitboxes; however, to allow the crosshair to select large models when far from the NPC's vanilla AABB or entity origin, you must use `d`-marked `hdb_`, `hds_`, `hadb_`, or `hads_`.

### 模型示例 / Model Example

以下是 `.geo.json` 中 `bones` 数组的简化片段：
Here is a simplified snippet of the `bones` array in `.geo.json`:

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
`hdb_body` is a hidden, detectable, and pushable body hitbox; `hds_sword_range` is a sensor that follows the right arm animation, can be attacked or interacted with, but does not push the target.

制作模型时需要注意：
Notes for model creation:

- 碰撞箱骨骼必须包含至少一个有效 cube，只有骨骼而没有 cube 不会生成 OBB。
  Hitbox bones must contain at least one valid cube; a bone without a cube will not generate an OBB.
- Bedrock/Gecko `.geo.json` 中骨骼 `pivot` 是绝对模型空间坐标，不是相对父骨骼的增量坐标。
  In Bedrock/Gecko `.geo.json`, bone `pivot` is an absolute model space coordinate, not a relative increment to the parent bone.
- cube 自身旋转按 Blockbench/GeckoLib 的 `Z -> Y -> X` 顺序应用。
  Cube self-rotation is applied in `Z -> Y -> X` order per Blockbench/GeckoLib.
- 骨骼可以跟随父骨骼动画，OBB 会使用实际渲染矩阵同步位置和旋转。
  Bones can follow parent bone animations; OBBs will sync position and rotation using the actual render matrix.
- 资源包可以覆盖模型；切换模型后会按新的模型资源重新建立碰撞箱数据。
  Resource packs can override models; switching models will rebuild hitbox data based on the new model resources.
- 不要只修改贴图中的视觉大小，碰撞范围由 cube 的 `origin`、`size`、`pivot`、`rotation` 和骨骼动画共同决定。
  Do not only modify the visual size in textures; collision range is determined by cube `origin`, `size`, `pivot`, `rotation`, and bone animations.

### 一个骨骼包含多个 cube / Multiple Cubes in One Bone

同一个碰撞箱骨骼中的所有 cube 都会被解析。第一个 cube 使用原骨骼名，后续 cube 在内部自动命名：
All cubes in the same hitbox bone will be parsed. The first cube uses the original bone name, subsequent cubes are auto-named internally:

```text
hadb_tail
hadb_tail__1
hadb_tail__2
```

脚本中的普通受击名称和定时伤害激活会归一化为基础骨骼名 `hadb_tail`。因此通常只需要在脚本里判断或传入基础名称，不要依赖 `__1`、`__2`。
Normal hit names and timed damage activation in scripts will be normalized to the base bone name `hadb_tail`. Therefore, you usually only need to check or pass the base name in scripts; do not rely on `__1`, `__2`.

## 攻击、交互与碰撞行为 / Attacks, Interaction & Collision

### 玩家攻击和交互 / Player Attacks and Interaction

客户端会使用玩家当前的实体触及距离，对带 `d` 的 OBB 进行准星射线检测。服务端收到攻击或交互包后，如果原版基于实体 AABB 的距离检查失败，会改用"玩家眼睛到目标可检测 OBB 的最短距离"再次验证。
The client uses the player's current entity reach distance to perform crosshair ray detection on OBBs with `d`. After the server receives the attack or interaction packet, if the vanilla distance check based on entity AABB fails, it will re-verify using "the shortest distance from the player's eyes to the target detectable OBB".

因此，大型模型的 NPC 原点即使离玩家很远，只要玩家实际靠近并指向带 `d` 的模型部位，攻击和交互仍可生效。普通实体仍使用原版距离规则，方块命中点比 OBB 更近时也不会被 OBB 穿透抢占。
Therefore, even if the NPC origin of a large model is far from the player, as long as the player actually approaches and points at a model part with `d`, attacks and interactions can still take effect. Regular entities still use vanilla distance rules, and block hit points closer than OBBs will not be overridden by OBB penetration.

### 近战和弹射物 / Melee and Projectiles

- 玩家或其他生物直接伤害 NPC 时，会用攻击者视线对可攻击 OBB 再次射线检测，并把命中的基础骨骼名写入 `e.hitboxName`。
  When a player or other mob directly damages an NPC, the attacker's line of sight will re-raycast against attackable OBBs and write the hit base bone name to `e.hitboxName`.
- 弹射物会检测一帧移动线段与 OBB 的相交，包含延伸到 NPC 原版 AABB 之外的 OBB。
  Projectiles detect intersection between the one-frame movement segment and OBBs, including OBBs extending beyond the NPC's vanilla AABB.
- 高速弹射物的路径会分段检查，降低穿过薄碰撞箱而漏判的概率。
  High-speed projectile paths are checked in segments to reduce the probability of missing thin hitboxes.
- 物理 `b` 箱和带 `d` 的传感器都可以参与受击检测，存在物理箱命中时优先归属物理箱。
  Physical `b` boxes and `d`-marked sensors can both participate in hit detection; when a physical box is hit, priority is given to the physical box.
- 三叉戟命中 OBB 后会反弹并短暂减速；穿透箭会按"同一支箭对同一个 NPC 只伤害一次"去重。
  Tridents bounce off OBBs and briefly slow down; piercing arrows are deduplicated by "one arrow damages the same NPC only once".

### 实体重叠与推挤 / Entity Overlap and Pushing

服务端每 tick 检测 NPC 的 OBB 与附近实体：
The server detects NPC OBBs against nearby entities every tick:

- OBB 对普通生物或玩家时，使用对方原版 AABB 检测。
  For OBB against regular mobs or players, the target's vanilla AABB is used for detection.
- OBB 对另一个具有 OBB 的 NPC 时，逐对进行 OBB 相交检测。
  For OBB against another NPC with OBBs, pairwise OBB intersection detection is performed.
- `s` 型传感器会正常参与重叠、`collide(e)` 和碰撞伤害，但不会推挤。
  `s` type sensors normally participate in overlap, `collide(e)`, and collision damage, but do not push.
- 只有本次相交的双方碰撞箱都为 `b` 型时，双方才会被互相推开。
  Only when both colliding hitboxes are `b` type will both parties be pushed apart.
- 无物理效果的实体、同一载具上的乘客等会被过滤。
  Entities without physics effects, passengers on the same vehicle, etc. are filtered out.

## F3+B 调试颜色 / F3+B Debug Colors

按下 `F3+B` 后，本模组会在原版碰撞箱之外绘制 NPC 的 OBB 线框：
After pressing `F3+B`, this mod will draw NPC OBB wireframes in addition to vanilla hitboxes:

| 颜色 / Color | 属性组合 / Property Combo | 对应前缀 / Prefix |
| :--- | :--- | :--- |
| 白色 / White | 可推挤，可检测/攻击/交互 / Pushable, detectable/attackable/interactable | `hdb_`, `hadb_` |
| 蓝色 / Blue | 可推挤，不带显式检测标记 / Pushable, no explicit detect flag | `hb_`, `hab_` |
| 黄色 / Yellow | 不推挤，可检测/攻击/交互 / Non-pushable, detectable/attackable/interactable | `hds_`, `hads_` |
| 绿色 / Green | 不推挤，不带显式检测标记 / Non-pushable, no explicit detect flag | `hs_`, `has_` |
| 红色 / Red | 当前正在与其他实体 AABB 或 OBB 重叠 / Currently overlapping with other entity AABB or OBB | 覆盖上述任意类型颜色 / Overrides any of the above colors |

红色是实时重叠状态，不代表该箱子一定会推挤或造成伤害。传感器与玩家重叠时同样会变红。OBB 中心还会显示三条短轴线：红、绿、蓝分别表示其局部 X、Y、Z 方向。
Red indicates real-time overlap status and does not mean the box will necessarily push or deal damage. Sensors overlapping with players will also turn red. The OBB center also displays three short axes: red, green, and blue representing local X, Y, Z directions respectively.

每个 OBB 的正中心会显示该碰撞箱的名称，文本颜色与当前线框颜色一致；发生重叠时，线框和名称会同时变为红色。文本使用全亮调试渲染并始终面向玩家。一个骨骼包含多个 cube 时，后续 OBB 会显示 `__1`、`__2` 等内部名称。
The name of each hitbox is displayed at the exact center of its OBB, with text color matching the current wireframe color; when overlapping, both the wireframe and name turn red. Text uses full-brightness debug rendering and always faces the player. When a bone contains multiple cubes, subsequent OBBs will display internal names like `__1`, `__2`.

名称显示可在客户端配置文件 `config/cnpc_immersiveboss-client.toml` 中控制，默认开启：
Name display can be controlled in the client config file `config/cnpc_immersiveboss-client.toml`, enabled by default:

```toml
[debug]
showObbNames = true
```

将其改为 `false` 后，`F3+B` 只绘制 OBB 线框和局部轴，不再绘制名称。
After changing it to `false`, `F3+B` will only draw OBB wireframes and local axes, without drawing names.

## CustomNPCs 脚本事件 / CustomNPCs Script Events

CustomNPCs 1.20.1 使用 Nashorn JavaScript，脚本按 ES5 编写。请使用 `var` 和普通 `function`，不要使用 `let`、`const`、箭头函数、可选链等新语法。
CustomNPCs 1.20.1 uses Nashorn JavaScript; scripts are written in ES5. Please use `var` and regular `function`, and avoid new syntax such as `let`, `const`, arrow functions, and optional chaining.

### `damaged(e)`：获取受击碰撞箱 / Get Hit Hitbox

普通玩家近战或弹射物命中时，直接读取 `e.hitboxName`：
For normal player melee or projectile hits, directly read `e.hitboxName`:

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
`e.hitboxName` is usually the base bone name. When damage does not come from an OBB, the ray does not hit any attackable OBB, or the current dependency version does not resolve to the corresponding injection point, this value may be `null`; scripts should handle null values.

### `interact(e)`：获取交互碰撞箱 / Get Interaction Hitbox

玩家右键交互 NPC 时也可以读取 `e.hitboxName`。服务端会沿玩家视线检测带 `d` 的 OBB，并返回最近命中的基础骨骼名：
You can also read `e.hitboxName` when players right-click to interact with the NPC. The server will detect OBBs with `d` along the player's line of sight and return the closest hit base bone name:

```javascript
function interact(e) {
    if (e.hitboxName == "hds_control_panel") {
        e.npc.say("控制面板已启动");
    }
}
```

没有命中可检测 OBB 时，该值为 `null`。需要响应交互的模型部位必须带 `d` 标记，例如 `hds_control_panel` 或 `hadb_switch`。
When no detectable OBB is hit, the value is `null`. Model parts that need to respond to interaction must have the `d` flag, such as `hds_control_panel` or `hadb_switch`.

### `collide(e)`：获取重叠碰撞箱 / Get Overlapping Hitbox

NPC 脚本的 `collide(e)` 事件增加了以下字段：
The `collide(e)` event in NPC scripts adds the following fields:

| 字段 / Field | 类型 / Type | 说明 / Description |
| :--- | :--- | :--- |
| `e.npc` | `ICustomNpc` | 拥有碰撞箱和当前脚本的 NPC / The NPC owning the hitbox and current script |
| `e.entity` | `IEntity` | 与 NPC 重叠的另一实体 / The other entity overlapping with the NPC |
| `e.hitboxAName` | `String` | 当前 NPC 的 OBB 名称 / The current NPC's OBB name |
| `e.hitboxBName` | `String` | 对方 OBB 名称；普通实体使用固定值 `AABB` / The other entity's OBB name; regular entities use the fixed value `AABB` |

示例：
Example:

```javascript
function collide(e) {
    if (e.hitboxAName == "hds_warning_range") {
        e.npc.getStoreddata().put("lastDetectedEntity", e.entity.getUUID());
    }
}
```

所有实际重叠的碰撞箱组合都可能触发事件，同一 NPC、实体和骨骼名称组合在同一 tick 内会去重。因此多个 OBB 同时重叠时，一 tick 仍可能收到多个 `collide(e)`。不要在事件中无条件播放声音、刷粒子或写入大量数据，必要时在脚本中自行增加冷却。
All actually overlapping hitbox combinations may trigger the event; the same NPC, entity, and bone name combination will be deduplicated within the same tick. Therefore, when multiple OBBs overlap simultaneously, multiple `collide(e)` calls may still be received in one tick. Do not unconditionally play sounds, spawn particles, or write large amounts of data in events; add cooldowns in scripts when necessary.

## ImmersiveBossAPI

脚本通过 `Java.type` 获取静态 API：
Scripts obtain the static API via `Java.type`:

```javascript
var ImmersiveBossAPI = Java.type(
    "sweda.cnpc_immersiveboss.api.ImmersiveBossAPI"
);
```

建议在脚本文件顶部定义一次并复用。
It is recommended to define this once at the top of the script file and reuse it.

### `damageHitbox`

方法签名：
Method signature:

```text
boolean damageHitbox(ICustomNpc npc, float amount, String hitboxName)
boolean damageHitbox(ICustomNpc npc, ICustomNpc source, float amount, String hitboxName)
```

示例：
Example:

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

该方法用于脚本主动指定"哪个部位受到伤害"。它会先以 `HitboxDamagedEvent` 运行目标 NPC 的 `damaged(e)`，脚本可以修改 `e.damage` 或取消事件；未取消时再直接减少 NPC 生命值。
This method allows scripts to actively specify "which part takes damage". It first runs the target NPC's `damaged(e)` with a `HitboxDamagedEvent`; scripts can modify `e.damage` or cancel the event; if not canceled, it directly reduces the NPC's health.

需要注意：这条 API 路径不是一次完整的原版 `LivingEntity.hurt`，不会自动等同于武器、护甲、无敌帧等原版伤害结算；最终生命值通过 CustomNPCs 包装器设置为整数。需要原版伤害规则的持续碰撞攻击应使用下一节的 `activateHitboxDamage`。
Note: This API path is not a complete vanilla `LivingEntity.hurt`; it does not automatically equate to weapon, armor, invincibility frames, and other vanilla damage calculations; the final health is set as an integer through the CustomNPCs wrapper. For continuous collision damage requiring vanilla damage rules, use `activateHitboxDamage` in the next section.

带 `source` 的重载只接受另一个 `ICustomNpc` 作为来源，来源可以传 `null`。
The overload with `source` only accepts another `ICustomNpc` as the source; the source can be `null`.

### 最近受击名称 / Last Hitbox Name

方法签名：
Method signature:

```text
String getLastHitboxName(ICustomNpc npc)
void clearLastHitboxName(ICustomNpc npc)
```

普通 `damaged(e)` 应优先直接读取 `e.hitboxName`。这两个方法主要用于高级脚本或兼容逻辑：
For normal `damaged(e)`, prefer reading `e.hitboxName` directly. These two methods are mainly for advanced scripts or compatibility logic:

```javascript
function damaged(e) {
    var name = ImmersiveBossAPI.getLastHitboxName(e.npc);
    if (name != null) {
        e.npc.say("最后命中: " + name);
        ImmersiveBossAPI.clearLastHitboxName(e.npc);
    }
}
```

### 碰撞箱名称查询 / Hitbox Name Queries

这些方法读取 NPC 当前模型的碰撞箱定义，返回按名称排序、按基础骨骼名去重后的 Java `String[]`：
These methods read the hitbox definitions of the NPC's current model and return a Java `String[]` sorted by name and deduplicated by base bone name:

```text
String[] getHitboxNames(ICustomNpc npc)
String[] getPhysicalHitboxNames(ICustomNpc npc)
String[] getDetectableHitboxNames(ICustomNpc npc)
String[] getSensorHitboxNames(ICustomNpc npc)
String[] getVisibleHitboxNames(ICustomNpc npc)
boolean hasHitbox(ICustomNpc npc, String hitboxName)
```

多 cube 骨骼产生的 `__1`、`__2` 内部名称不会重复出现在结果中。`physical` 对应 `b` 后缀，`sensor` 对应 `s` 后缀，`detectable` 对应 `d` 标记，`visible` 对应 `a` 标记。同一个碰撞箱可以同时出现在多个分类中。
Internal names `__1`, `__2` generated by multi-cube bones will not appear repeatedly in the results. `physical` corresponds to the `b` suffix, `sensor` to the `s` suffix, `detectable` to the `d` flag, and `visible` to the `a` flag. The same hitbox can appear in multiple categories simultaneously.

Nashorn 中可以使用 `Java.from(...)` 转成普通 JavaScript 数组：
In Nashorn, you can use `Java.from(...)` to convert to a regular JavaScript array:

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
If the NPC is invalid, has no custom model, the model has no hitboxes, or both the definition and real-time OBB are unavailable, an empty array is returned. Queries prefer the current model definition; only when the definition is unavailable will it fall back to real-time OBB names, avoiding returning stale names after model switches.

### `activateHitboxDamage`

该 API 在服务端为一个碰撞箱开启临时伤害窗口。窗口存续期间，只要该 OBB 与玩家、普通生物或其他 CustomNPCs 生物发生重叠，就会尝试造成伤害。
This API opens a temporary damage window for a hitbox on the server. While the window is active, as long as the OBB overlaps with players, regular mobs, or other CustomNPCs entities, it will attempt to deal damage.

完整方法签名：
Complete method signature:

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
Parameters must be passed in order, and can only be omitted sequentially from the end:

| 参数 / Parameter | 必传 / Required | 默认值 / Default | 说明 / Description |
| :--- | :--- | :--- | :--- |
| `npc` | 是 / Yes | 无 / None | 拥有该碰撞箱的 NPC，脚本中通常为 `e.npc` / The NPC owning the hitbox, usually `e.npc` in scripts |
| `hitboxName` | 是 / Yes | 无 / None | 碰撞箱基础骨骼名，例如 `hds_sword` / Hitbox base bone name, e.g., `hds_sword` |
| `startDelayTicks` | 是 / Yes | `0` | 从调用到窗口启用前等待的 tick 数；负数按 0 处理，无需延迟时必须传 `0` / Ticks to wait before window enables from call; negatives treated as 0, must pass `0` when no delay |
| `durationTicks` | 否 / No | `20` | 伤害窗口持续 tick 数，必须大于 0 / Damage window duration in ticks, must be > 0 |
| `damage` | 否 / No | `1.0` | 每次成功命中的伤害值，必须为有限正数 / Damage value per successful hit, must be finite positive |
| `repeatCount` | 否 / No | `1` | 同一个目标在本次窗口内最多成功受伤次数；小于等于 0 表示无限 / Max successful hits per target in this window; <= 0 means infinite |
| `repeatIntervalTicks` | 否 / No | `10` | 同一目标两次成功伤害之间的最短 tick 数；负数按 0 处理 / Minimum ticks between two successful hits on same target; negatives treated as 0 |
| `maxTargets` | 否 / No | `0` | 本次窗口最多伤害的不同目标数；小于等于 0 表示无限 / Max different targets damaged in this window; <= 0 means infinite |
| `callback` | 否 / No | 无 / None | 每次成功造成碰撞伤害后执行；参数依次为攻击方 NPC 和受伤目标的 CNPC 实体包装器 / Executed after each successful collision damage; parameters are attacker NPC and damaged target CNPC entity wrappers |

对应的所有可用重载为：
All corresponding available overloads:

```text
activateHitboxDamage(npc, hitboxName, startDelayTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount, repeatIntervalTicks)
activateHitboxDamage(npc, hitboxName, startDelayTicks, durationTicks, damage, repeatCount, repeatIntervalTicks, maxTargets)
```

上述任一重载都可以在末尾追加 `callback`。除了 `startDelayTicks` 必须传入，其余参数均可使用默认值：
The `callback` can be appended to the end of any of the above overloads. Except for `startDelayTicks` which is required, all other parameters can use default values:

```javascript
ImmersiveBossAPI.activateHitboxDamage(e.npc, "hds_sword", 0);
```

同一组重载也直接提供在 `e.npc` 上。下面两种写法完全等价：
The same group of overloads is also directly available on `e.npc`. The following two写法 are completely equivalent:

```javascript
ImmersiveBossAPI.activateHitboxDamage(e.npc, "hds_sword", 0, 20, 1);
e.npc.activateHitboxDamage("hds_sword", 0, 20, 1);
```

直接调用同样支持末尾的回调函数和所有完整参数。
Direct calls also support the trailing callback function and all complete parameters.

这表示 `hds_sword` 在接下来 20 tick 内，对每个目标造成 1 点伤害，每个目标最多成功受伤 1 次，不限制不同目标数量。
This means `hds_sword` will deal 1 damage to each target over the next 20 ticks, with each target taking at most 1 successful hit, with no limit on different targets.

完整配置示例：
Complete configuration example:

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
This means:

- 调用后先等待 `6 tick`，等待期间碰撞不会造成伤害。
  After calling, wait `6 ticks` first; collisions during the wait will not deal damage.
- 窗口持续 `20 tick`。
  The window lasts for `20 ticks`.
- 每次造成 `4` 点伤害。
  Each hit deals `4` damage.
- 同一个目标最多成功受伤 `3` 次。
  Each target can take at most `3` successful hits.
- 同一目标每次受伤至少间隔 `10 tick`。
  Each hit on the same target must be at least `10 ticks` apart.
- 只允许第一个成功受到伤害的目标占用本次窗口的名额，后续不同目标不再受伤。
  Only the first target to successfully take damage occupies this window's slot; subsequent different targets will not take damage.
- 每次实际成功造成伤害后都执行回调；同一目标允许循环受伤时，每次成功伤害都会执行一次。
  The callback is executed after each actual successful damage; when a target is allowed cyclic damage, the callback runs once per successful hit.

回调是普通 Nashorn 函数，会保留定义它时捕获的脚本变量；额外提供的 `attacker` 和 `target` 都是 CNPC 脚本实体包装器：
Callbacks are regular Nashorn functions that retain script variables captured at definition; the additional `attacker` and `target` are both CNPC script entity wrappers:

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
Callbacks only run when `hurt(...)` actually returns success. They are not called when the target is in invincibility frames, the damage event is canceled, or the damage does not take effect. If a callback throws an exception, it will be written to the server log but will not interrupt collision detection or subsequent window loops.

可以将调用放在攻击动画开始或武器进入有效帧的脚本逻辑中：
You can place the call in script logic at the start of attack animations or when weapons enter active frames:

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
Call and counting rules:

- 返回 `true` 表示成功创建窗口；名称无效、持续时间或伤害无效、NPC 已死亡、在客户端调用等情况返回 `false`。
  Returns `true` on successful window creation; returns `false` for invalid names, invalid duration or damage, NPC death, client-side calls, etc.
- `startDelayTicks` 从 API 调用所在 tick 开始计时，持续时间从延迟结束、窗口正式启用时才开始计算。
  `startDelayTicks` starts counting from the tick the API is called; duration starts counting from when the delay ends and the window officially enables.
- 延迟等待中的窗口仍可被查询或取消，但不会记录目标、命中次数或重复间隔。
  Windows during the delay wait can still be queried or canceled, but will not record targets, hit counts, or repeat intervals.
- 再次激活同一个 NPC 的同一基础骨骼，会重置持续时间、目标列表和重复计数，不会与旧窗口叠加。
  Re-activating the same base bone on the same NPC will reset duration, target list, and repeat count, without stacking with the old window.
- 传入 `bone__1` 等多 cube 内部名时会归一化到基础骨骼；建议直接传基础名称。
  Passing multi-cube internal names like `bone__1` will be normalized to the base bone; it is recommended to pass the base name directly.
- `repeatCount` 针对每个目标分别计数，`maxTargets` 针对整个调用周期计数。
  `repeatCount` counts per target separately; `maxTargets` counts across the entire call cycle.
- 只有 `hurt(...)` 实际返回成功后，才消耗一次重复次数并占用目标名额。被取消、处于无敌帧或未实际受伤不会占用名额。
  Only when `hurt(...)` actually returns success does it consume one repeat count and occupy a target slot. Canceled, invincibility frame, or non-effective damage does not occupy a slot.
- 目标名额按首次成功伤害的先后顺序占用。
  Target slots are occupied in the order of first successful damage.
- 即使重复间隔为 0，同一个目标每 tick 最多尝试伤害一次。
  Even with a repeat interval of 0, each target can be damaged at most once per tick.
- NPC 死亡、移除或窗口到期后，相关状态会自动清理。
  NPC death, removal, or window expiration will automatically clean up related state.
- 伤害源使用 NPC 的 `mobAttack`，因此会经过原版护甲、无敌帧、Forge 伤害事件和 CustomNPCs 伤害流程。
  The damage source uses the NPC's `mobAttack`, so it goes through vanilla armor, invincibility frames, Forge damage events, and CustomNPCs damage processing.
- 碰撞伤害不依赖 NPC 是否启用了脚本；脚本只负责调用一次来开启窗口。
  Collision damage does not depend on whether the NPC has scripts enabled; scripts are only responsible for calling once to open the window.

### 伤害窗口查询与中断 / Hitbox Damage Window Queries and Cancellation

每个窗口都可以独立查询或立即中断：
Each window can be queried or canceled independently:

```text
boolean cancelHitboxDamageWindow(ICustomNpc npc, String hitboxName)
int cancelAllHitboxDamageWindows(ICustomNpc npc)
boolean isHitboxDamageWindowActive(ICustomNpc npc, String hitboxName)
int getHitboxDamageWindowRemainingTicks(ICustomNpc npc, String hitboxName)
String[] getActiveHitboxDamageWindows(ICustomNpc npc)
```

这组 API 也可以直接通过 `e.npc` 调用，此时无需传入第一个 `npc` 参数：
This group of APIs can also be called directly through `e.npc`, without needing to pass the first `npc` parameter:

```javascript
e.npc.cancelHitboxDamageWindow("hds_sword");
e.npc.cancelAllHitboxDamageWindows();
e.npc.isHitboxDamageWindowActive("hds_sword");
e.npc.getHitboxDamageWindowRemainingTicks("hds_sword");
var names = Java.from(e.npc.getActiveHitboxDamageWindows());
```

单窗口中断成功时返回 `true`；窗口不存在、已到期或参数无效时返回 `false`。全部中断返回实际取消的活动窗口数量。剩余时间在窗口不存在或已到期时为 `0`，活动窗口名称同样按基础骨骼名排序。
Single window cancellation returns `true` on success; returns `false` when the window does not exist, has expired, or parameters are invalid. Cancel all returns the actual number of active windows canceled. Remaining time is `0` when the window does not exist or has expired; active window names are also sorted by base bone name.

例如，动画提前结束或攻击被打断时关闭剑的伤害窗口：
For example, to close the sword's damage window when an animation ends early or the attack is interrupted:

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
Cancellation will simultaneously discard the target slots, hit counts, and repeat interval state recorded by that window, but will not affect other windows on the same NPC, nor will it revoke damage already dealt. Window queries and controls are only effective in server-side script contexts.

### NPC 转向速度限制 / NPC Turn Speed Limit

可以为大型 NPC 设置水平转向速度上限，使移动、战斗锁定、空闲观察和脚本旋转都逐步到达目标朝向：
You can set a horizontal turn speed limit for large NPCs, allowing movement, combat locking, idle observation, and script rotation to gradually reach the target orientation:

```javascript
function init(e) {
    e.npc.setTurnSpeedLimit(3.0);
}
```

单位为"度/tick"。Minecraft 每秒运行 20 tick，因此 `3.0` 表示每秒最多旋转 `60` 度，完成一次 180 度转身至少需要 3 秒。该限制同时作用于实体移动朝向、模型身体朝向和头部水平朝向，Gecko 模型及其 OBB 碰撞箱会随身体逐步旋转。导航移动会沿当前允许朝向形成转弯轨迹；朝向与路径偏差较大时，移动速度最低降至原速度的 20%，对准后恢复全速。
The unit is "degrees per tick". Minecraft runs at 20 ticks per second, so `3.0` means a maximum rotation of `60` degrees per second, requiring at least 3 seconds to complete a 180-degree turn. This limit applies simultaneously to entity movement orientation, model body orientation, and head horizontal orientation; Gecko models and their OBB hitboxes will gradually rotate with the body. Navigation movement will form turning trajectories along the currently allowed orientation; when the orientation deviates significantly from the path, movement speed drops to a minimum of 20% of the original speed, recovering to full speed once aligned.

可用方法为：
Available methods:

```text
boolean setTurnSpeedLimit(float degreesPerTick)
float getTurnSpeedLimit()
boolean hasTurnSpeedLimit()
boolean clearTurnSpeedLimit()
void setRotationImmediate(float rotation)
```

启用限制后，CNPC 原有的 `setRotation(...)` 会变成渐进转向：
After enabling the limit, CNPC's original `setRotation(...)` will become progressive turning:

```javascript
e.npc.setTurnSpeedLimit(2.0);
e.npc.setRotation(180);          // 以每 tick 最多 2 度逐步转到 180 度 / Gradually turn to 180 degrees at max 2 degrees per tick
e.npc.setRotationImmediate(90); // 忽略限制，立即转到 90 度 / Ignore limit, instantly turn to 90 degrees
```

规则如下：
Rules:

- `setTurnSpeedLimit(...)` 只接受有限的非负数；成功时返回 `true`。
  `setTurnSpeedLimit(...)` only accepts finite non-negative numbers; returns `true` on success.
- `0` 表示完全冻结水平转向；`180` 及以上实际上不会限制最短角度转向。
  `0` means completely freeze horizontal turning; `180` and above effectively do not limit the shortest angle turn.
- `getTurnSpeedLimit()` 在没有启用限制时返回 `-1`。
  `getTurnSpeedLimit()` returns `-1` when no limit is enabled.
- `clearTurnSpeedLimit()` 取消限制并丢弃尚未完成的 `setRotation(...)` 目标；确实取消了限制时返回 `true`。
  `clearTurnSpeedLimit()` cancels the limit and discards unfinished `setRotation(...)` targets; returns `true` when the limit is indeed canceled.
- `setRotationImmediate(...)` 用于生成、传送或剧情重置，同时对齐实体、身体和头部朝向。
  `setRotationImmediate(...)` is used for spawning, teleporting, or plot resets, aligning entity, body, and head orientation simultaneously.
- 目标接近正后方时会锁定一次左转或右转选择，脱离背后扇区后再解除，避免在两个等长方向之间来回抖动。
  When the target approaches directly behind, a left or right turn choice is locked once, and released after leaving the rear sector, to avoid oscillillation between two equal-length directions.
- 限速值保存在 NPC 的持久化数据中，保存并重新载入世界后仍然有效。
  The speed limit value is saved in the NPC's persistent data and remains effective after saving and reloading the world.
- 限制只改变水平转向速度，不限制头部俯仰角，也不会额外修改 CNPC 的攻击时机。
  The limit only changes horizontal turning speed, does not limit head pitch angle, and does not additionally modify CNPC's attack timing.

同一功能也可以通过静态 API 调用，例如 `ImmersiveBossAPI.setTurnSpeedLimit(e.npc, 3.0)`。
The same functionality can also be invoked via static API, e.g., `ImmersiveBossAPI.setTurnSpeedLimit(e.npc, 3.0)`.

## Throw scripting quick reference / 投技脚本快速参考

Throw animations are server-authoritative. The `target` must be a player `IEntity` wrapper and the `animation` must exist in the NPC GeckoLib model. Durations use ticks (`20` ticks = 1 second).
投技动画由服务器权威控制。`target` 必须是玩家实体包装器（`IEntity`），`animation` 必须是 NPC GeckoLib 模型中已定义的动画名。时长单位为 tick（`20` tick = 1 秒）。

### Basic usage / 基础调用

Activate a hitbox-window to trigger the throw on contact:
通过激活碰撞箱伤害窗口，在命中时触发投技：

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

### Static API with callbacks / 带回调的静态 API

The static API accepts escape and finish callbacks:
静态 API 额外接受**挣脱（Escape）**和**完成（Finish）**回调：

```javascript
var BossAPI = Java.type("sweda.cnpc_immersiveboss.api.ImmersiveBossAPI");

function attack(e) {
    var target = e.npc.getAttackTarget();
    if (target == null || BossAPI.isThrowActive(target)) return;

    BossAPI.startThrow(e.npc, target, "grab", 60, true, "ad", 5,
        function(npc, player) { npc.say("挣脱了"); },
        function(npc, player) { npc.say("投技结束"); });
}
```

### Full signature / 完整签名

```
startThrow(npc, target, animation, durationTicks, returnToStart,
           struggleMode, difficulty, onEscape, onFinish)
```

| Parameter | Description |
| :--- | :--- |
| `returnToStart` | `true` -> teleport player back to start position on finish/escape; `false` -> keep server-recorded end position / `true` 时将玩家送回起始位置；`false` 时保留结束位置 |
| `struggleMode` | `none` / `ad` / `space` / `shift` (or numeric `0`-`3`, static API only) |
| `difficulty` | Positive number / 正数 |
| `onEscape`, `onFinish` | Callbacks with signature `(npc, player)`, fired only on normal completion / 仅在对应流程正常完成时触发 |

### Utility methods / 实用方法

```javascript
BossAPI.stopThrow(target);       // Cancel and restore the player / 取消并恢复玩家状态
BossAPI.isThrowActive(target);   // Check if a throw is in progress / 是否仍在投技中
```

### Struggle-mode pitfall / 挣扎模式注意事项

Use `none`, `ad`, `space`, or `shift` for struggle modes. Numeric modes `0`-`3` are supported by the static API only. When calling the wrapper directly with two `null` callbacks, always pass a **string** mode to avoid Nashorn overload ambiguity.
挣扎模式支持字符串 `none`、`ad`、`space`、`shift`，数字 `0`-`3` 仅限静态 API。直接使用包装器调用且回调为 `null` 时，**必须使用字符串**模式以避免 Nashorn 重载歧义。

### Configuration / 配置

Players are prevented from attacking while controlled by a throw by default. To allow attacks, edit `config/cnpc_immersiveboss-common.toml`:
默认情况下玩家在投技期间无法攻击。如需允许攻击，修改配置文件：

```toml
[throw]
disableTargetAttack = false
```

### Auto-cleanup / 自动清理

NPC death, player disconnect, or dimension change will automatically clean up throw state. **These abnormal interruptions do NOT fire escape or finish callbacks** - plan your fallback (兜底) logic accordingly.
NPC 死亡、玩家断开连接或切换维度时系统会自动清理投技状态。**这些异常中断不会触发挣脱或完成回调**，请据此设计兜底逻辑。

---

See [Scripting API](docs/wiki/Scripting-API.md) for all hitbox-window and throw signatures.
完整碰撞箱窗口与投技方法签名请参阅 [Scripting API](docs/wiki/Scripting-API.md)。

## 数据同步与服务端说明 / Data Sync & Server Notes

模型碰撞箱定义由客户端资源管理器解析，因此资源包可以覆盖 `.geo.json`。客户端会在模型首次出现或模型变更时向服务端同步定义，并在渲染过程中同步实时动画 OBB 变换。服务端使用这些数据进行实体碰撞、脚本事件、弹射物判定和碰撞伤害。
Model hitbox definitions are parsed by the client resource manager, so resource packs can override `.geo.json`. The client will sync definitions to the server when the model first appears or when the model changes, and sync real-time animation OBB transforms during rendering. The server uses this data for entity collision, script events, projectile detection, and collision damage.

当前网络协议要求客户端和服务端模组版本一致。用于多人服务器时应确保：
The current network protocol requires client and server mod versions to match. When used on multiplayer servers, ensure:

- 两侧安装相同版本的本模组、CustomNPCs、GeckoLib 和 CNPC Gecko Addon。
  Both sides install the same versions of this mod, CustomNPCs, GeckoLib, and CNPC Gecko Addon.
- 玩家使用的资源包包含正确的模型文件，且资源位置与 NPC 配置一致。
  Player resource packs contain the correct model files, and resource locations match NPC configuration.
- 模型更换后让客户端重新加载并渲染该 NPC；必要时使用 `F3+T` 重载资源包。
  After changing models, have the client reload and render the NPC; use `F3+T` to reload resource packs if necessary.
- 不要把只存在于某个客户端的旧模型当作服务端权威配置。
  Do not treat old models that only exist on a certain client as server-authoritative configuration.

## 常见问题 / Troubleshooting

### `F3+B` 看不到 OBB / Cannot See OBB with `F3+B`

- 检查骨骼名是否满足 `h[a][d][b|s]_名称`，尤其是末尾必须为 `b` 或 `s` 后再接下划线。
  Check if the bone name satisfies `h[a][d][b|s]_name`, especially that the end must be `b` or `s` followed by an underscore.
- 检查骨骼是否至少包含一个有效 cube。
  Check if the bone contains at least one valid cube.
- 确认 NPC 实际使用的是当前编辑的 `.geo.json`，资源包名称空间和路径无误。
  Confirm the NPC is actually using the currently edited `.geo.json`, and the resource pack namespace and path are correct.
- 使用 `F3+T` 重载资源，或让 NPC 离开并重新进入渲染范围。
  Use `F3+T` to reload resources, or have the NPC leave and re-enter render range.

### 大型模型看得到，但不能攻击或交互 / Large Model Visible But Cannot Attack or Interact

- 给需要被准星选中的部位增加 `d`，例如将 `hs_hand` 改为 `hds_hand`。
  Add `d` to parts that need to be selected by crosshair, e.g., change `hs_hand` to `hds_hand`.
- `b` 型旧碰撞箱可参与部分受击归属，但只有 `d` 型 OBB 会扩展客户端准星选择和服务端交互距离。
  `b` type legacy hitboxes can participate in some hit attribution, but only `d` type OBBs extend client crosshair selection and server interaction distance.
- 确认玩家眼睛到 OBB 表面的距离没有超过当前实体触及距离，并确认中间没有更近的方块。
  Confirm the distance from the player's eyes to the OBB surface does not exceed the current entity reach distance, and confirm no closer blocks are in between.

### `hs_` 传感器没有推开玩家 / `hs_` Sensors Do Not Push Players

这是预期行为。`s` 表示只检测重叠，不做物理推挤。需要推挤时改用 `b`；需要攻击和交互时再增加 `d`。
This is expected behavior. `s` means detect overlap only, no physical pushing. Use `b` when pushing is needed; add `d` when attack and interaction are needed.

### 传感器没有触发 `collide(e)` 或不变红 / Sensors Do Not Trigger `collide(e)` or Do Not Turn Red

- 确认双方实际发生体积重叠，而不是仅仅视觉接触。
  Confirm both parties actually have volume overlap, not just visual contact.
- 检查 NPC 已在客户端渲染并同步 OBB。
  Check that the NPC is rendered on the client and OBB is synced.
- `collide(e)` 需要 NPC 脚本已启用且至少存在一个脚本；红色调试显示不要求脚本开启。
  `collide(e)` requires NPC scripts to be enabled with at least one script present; red debug display does not require scripts to be enabled.
- `noPhysics` 实体和同一载具中的实体可能被过滤。
  `noPhysics` entities and entities on the same vehicle may be filtered out.

### 定时碰撞伤害没有生效 / Timed Collision Damage Not Working

- 确认 API 在服务端脚本事件中调用并返回 `true`。
  Confirm the API is called in a server-side script event and returns `true`.
- 传入完整、有效的碰撞箱名称，持续时间和伤害必须大于 0。
  Pass complete, valid hitbox names; duration and damage must be greater than 0.
- 检查目标是否处于原版无敌帧、是否取消了伤害事件，以及目标是否仍在 OBB 内。
  Check if the target is in vanilla invincibility frames, if the damage event was canceled, and if the target is still within the OBB.
- `maxTargets` 已被先前目标占满时，后续目标不会受伤；重新调用 API 会开始一个全新窗口。
  When `maxTargets` is already full from previous targets, subsequent targets will not take damage; re-calling the API will start a completely new window.

### 自定义血条不显示或显示错误 / Custom Boss Bar Not Displaying or Displaying Incorrectly

- Bossbar 模式必须为 `3` 或 `4`，纹理路径不能为空。
  Bossbar mode must be `3` or `4`, and the texture path cannot be empty.
- 模式 `4` 只在战斗计时有效时显示；模式 `3` 超过 128 格不会显示。
  Mode `4` only displays when combat timer is valid; mode `3` does not display beyond 128 blocks.
- 当前最多同时绘制 3 条自定义血条。
  Currently, a maximum of 3 custom boss bars can be rendered simultaneously.
- 检查 PNG 是否严格按上下两半组织，并确认宽高、缩放为正数。
  Check if the PNG is strictly organized in upper and lower halves, and confirm width/height and scale are positive numbers.
- 颜色不带 `#`，例如红色填写 `FF4040`。
  Color without `#`, e.g., red is `FF4040`.
- 路径使用 `modid:textures/...png`，不要填写 `assets/` 前缀。
  Path uses `modid:textures/...png`, do not include `assets/` prefix.

## 开发与构建 / Development & Building

项目使用 Gradle 8.8、ForgeGradle 和 Java 17。依赖 JAR 位于仓库的 `libs/`，这些文件是构建所需内容，不要删除或替换为旧版本。
The project uses Gradle 8.8, ForgeGradle, and Java 17. Dependency JARs are located in the repository's `libs/`; these files are required for building and should not be deleted or replaced with older versions.

Windows 下常用命令：
Common commands on Windows:

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于：
Build artifacts are located at:

```text
build/libs/cnpc_immersiveboss-<version>.jar
```

仓库没有自动化测试。修改碰撞、网络包或 mixin 后，至少应启动客户端或服务端，并检查 `run/logs/latest.log` 与 `run/crash-reports/`。
There are no automated tests in the repository. After modifying collision, network packets, or mixins, at least start the client or server and check `run/logs/latest.log` and `run/crash-reports/`.

### 可选的 CNPC MoreRenderSuppot / Optional CNPC MoreRenderSuppot

开发环境中，如果 `libs/cnpc_morerendersuppot.jar` 存在，Gradle 会将其作为可选运行时依赖加载。它不属于本模组生产 JAR 的强制依赖，也不应直接放入开发实例的 `run/mods`，因为生产 SRG JAR 未经 ForgeGradle 重映射时可能出现 `NoSuchFieldError`。
In the development environment, if `libs/cnpc_morerendersuppot.jar` exists, Gradle will load it as an optional runtime dependency. It is not a required dependency for the mod's production JAR and should not be placed directly into the development instance's `run/mods`, because the production SRG JAR may encounter `NoSuchFieldError` when not remapped by ForgeGradle.

### 可选的 TaCZ 兼容 / Optional TaCZ Compatibility

TaCZ 不是本模组的强制依赖。检测到 TaCZ 时，对应兼容 mixin 才会启用：枪械射线会纳入 NPC 的动画 OBB，包括超出原版 AABB 的部分，并将命中部位传给 `damaged(e)` 的 `e.hitboxName`。开发环境可使用 `-PtaczDevRuntime` 挂载测试依赖：
TaCZ is not a required dependency for this mod. When TaCZ is detected, the corresponding compatibility mixin will be enabled: gun rays will include the NPC's animated OBBs, including parts extending beyond the vanilla AABB, and pass the hit part to `e.hitboxName` in `damaged(e)`. The development environment can use `-PtaczDevRuntime` to mount test dependencies:

```powershell
.\gradlew.bat runClient -PtaczDevRuntime
```

当前兼容代码按 TaCZ `1.1.8-hotfix` API 开发；其他版本应在实际游戏中验证。
The current compatibility code is developed against TaCZ `1.1.8-hotfix` API; other versions should be verified in actual gameplay.

### 可选的 Better Combat 与 Iron's Spellbooks 兼容 / Optional Better Combat and Iron's Spellbooks Compatibility

Better Combat 和 Iron's Spellbooks 不是本模组的强制依赖。检测到对应模组时，兼容 mixin 才会启用，并将其近战、法术投射物、范围法术和链式闪电等攻击路径纳入动画 OBB 检测。未安装这些模组时，本模组仍可独立构建和运行。
Better Combat and Iron's Spellbooks are not required dependencies for this mod. When the corresponding mods are detected, the compatibility mixins will be enabled, incorporating their melee, spell projectiles, area spells, chain lightning, and other attack paths into animated OBB detection. Without these mods installed, this mod can still be built and run independently.

开发环境可使用以下参数关闭可选兼容运行时，验证不安装第三方战斗模组时的启动流程：
The development environment can use the following parameters to disable optional compatibility runtimes, verifying the startup process without installing third-party combat mods:

```powershell
.\gradlew.bat runClient -PwithoutCombatMods
.\gradlew.bat runClient -PwithoutOptionalMods
```

当前兼容代码按 Better Combat `1.9.0` 和 Iron's Spellbooks `1.20.1-3.16.2` 的开发环境进行适配；不同版本应在实际游戏中验证。
The current compatibility code is adapted against Better Combat `1.9.0` and Iron's Spellbooks `1.20.1-3.16.2` development environments; different versions should be verified in actual gameplay.

## 许可证 / License

本项目使用 MIT 许可证。
This project uses the MIT License.
