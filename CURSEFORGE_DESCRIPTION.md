# CNPC-ImmersiveBoss

**Adds animated multi-hitbox OBB collision to GeckoLib NPCs for CustomNPCs / 为 CustomNPCs 的 GeckoLib NPC 提供随骨骼动画变化的 OBB 多碰撞箱。**

Attacks, interactions, projectiles, entity pushing and damage detection now follow the actual model shape. Version 0.3.11 adds optional Better Combat and Iron's Spellbooks compatibility, hitbox-centered damage particles, and a Blockbench preview tool.

让大型或异形 NPC 的受击、交互、弹射物、实体推挤和攻击判定贴合实际模型。0.3.11 新增 Better Combat 与 Iron's Spellbooks 可选兼容、命中特效定位和 Blockbench 碰撞箱预览工具。

**Minecraft 1.20.1 / Forge 47.x / Java 17**

## Features / 特性

- **Animated OBB hitboxes / 动画 OBB 碰撞箱** — Collision volumes follow GeckoLib bone translation, rotation and animation. Multi-cube bones are supported. ／ 碰撞体积跟随 GeckoLib 骨骼平移、旋转和动画，支持多 cube 骨骼。
- **Per-part combat / 分部位战斗** — Crosshair targeting, melee attacks and vanilla projectiles such as arrows and tridents can hit specific parts. Read `e.hitboxName` in scripts. ／ 准星、近战、箭和三叉戟等弹射物可以命中具体部位，并可在脚本中读取 `e.hitboxName`。
- **Physical boxes and sensors / 物理箱与传感器** — `b` boxes push entities apart, while `s` sensors only detect overlap. ／ `b` 箱参与实体互相推挤，`s` 传感器只检测重叠，互不干扰。
- **Large model support / 大型模型支持** — OBBs outside the vanilla NPC AABB can still be attacked, interacted with, and hit by projectiles. ／ OBB 超出 NPC 原版 AABB 时，仍然可以被攻击、交互和弹射物命中。
- **Animated damage windows / 碰撞伤害窗口** — Sword, tail and other animated bones can deal damage on specific frames, with delay, repeat count, target limits and callbacks. ／ 让剑、尾巴等动画骨骼在指定时间造成伤害，支持延迟、重复次数、目标数量限制和回调。
- **Custom boss bars / 自定义 Boss 血条** — Supports two-layer PNG textures, always-on or combat-only display, configurable color, scale and offset. ／ 支持上下分层 PNG 素材、常显或战斗时显示，并可调整颜色、缩放和偏移。
- **Better Combat compatibility / Better Combat 兼容** — Attack ranges, angles and shapes can directly detect animated NPC OBBs, with client target discovery and server-side validation. ／ 攻击范围、角度和形状可以直接检测 NPC 动画 OBB，并支持客户端目标查找和服务端命中验证。
- **Iron's Spellbooks compatibility / Iron's Spellbooks 兼容** — Spell projectiles, cone spells, area attacks and chain lightning can use animated OBB detection. ／ 法术投射物、锥形法术、范围攻击和链式闪电可以使用动画 OBB 检测。
- **Hit effects follow hitboxes / 命中特效跟随部位** — Damage, critical-hit and damage-indicator particles can appear at the actual hit OBB. ／ 伤害、暴击和伤害指示粒子可以显示在实际命中的 OBB 位置。
- **Blockbench hitbox preview / Blockbench 碰撞箱预览** — Includes color-coded OBB outlines, hitbox visibility controls and local-axis previews. ／ 提供彩色 OBB 边框、碰撞箱显示控制和局部坐标轴预览。
- **Debug rendering / 调试渲染** — Press `F3+B` to view OBB types, names, local axes and overlap states. ／ 按下 `F3+B` 可以查看 OBB 类型、名称、局部轴和重叠状态。
- **Optional TaCZ compatibility / 可选 TaCZ 兼容** — [TaCZ](https://www.curseforge.com/minecraft/mc-mods/tacz) gun raytraces can include animated OBBs and preserve the hitbox name. ／ 安装 TaCZ 后，枪械射线可以检测动画 OBB 并传递命中部位名称。

## Quick Start / 快速开始

1. In Blockbench, add a bone named `h[a][d][b|s]_name`, such as `hdb_body` or `hads_sword`. ／ 在 Blockbench 的 GeckoLib 模型中新增骨骼，命名为 `h[a][d][b|s]_名称`，例如 `hdb_body` 或 `hads_sword`。
2. Put at least one cube inside the bone, export the `.geo.json` file, and assign it to the NPC through CNPC Gecko Addon. ／ 在骨骼内放入至少一个 cube，导出 `.geo.json`，然后通过 CNPC Gecko Addon 设置给 NPC。
3. Enter the world and press `F3+B` to verify the OBB wireframes. ／ 进入世界并看到 NPC 后，按下 `F3+B` 检查 OBB 线框。
4. Add a `damaged(e)` script and read `e.hitboxName` to implement per-part logic. ／ 在 NPC 脚本中添加 `damaged(e)`，读取 `e.hitboxName` 实现分部位逻辑。
5. Optionally install Better Combat, Iron's Spellbooks or TaCZ to enable their OBB compatibility. ／ 可选安装 Better Combat、Iron's Spellbooks 或 TaCZ，以启用对应的 OBB 兼容功能。

## Installation / 安装

1. Install Forge 1.20.1 and use Java 17. ／ 安装 Forge 1.20.1，并确保启动器使用 Java 17。
2. Put the three required dependencies and the CNPC-ImmersiveBoss JAR into the `mods` folder. ／ 将三个必要依赖和 CNPC-ImmersiveBoss 的 JAR 放入 `mods` 文件夹。
3. In multiplayer, the client and server must use identical mod and dependency versions. ／ 多人游戏中，客户端和服务端必须安装相同版本的模组及依赖。
4. Put models, textures and boss bar assets in a resource pack enabled by every player. ／ 将模型、贴图和 Boss 血条素材放入资源包，并确保所有玩家启用同一资源包。

## Dependencies / 依赖

### Required / 必要依赖

| Mod / 模组 | Version / 版本 |
| --- | --- |
| [GeckoLib](https://www.curseforge.com/minecraft/mc-mods/geckolib) | 4.8.4 |
| CustomNPCs Goodbird 1.20.1 unofficial port / 非官方移植版 | Compatible 1.20.1 build / 兼容的 1.20.1 版本 |
| CNPC Gecko Addon | CE 1.2.2 recommended / 推荐 CE 1.2.2 |
| CNPC-ImmersiveBoss | 0.3.11 |

### Optional / 可选依赖

| Mod / 模组 | Purpose / 用途 |
| --- | --- |
| Better Combat | Melee attack OBB compatibility / 近战攻击 OBB 兼容 |
| Iron's Spellbooks | Spell attack OBB compatibility / 法术攻击 OBB 兼容 |
| [TaCZ](https://www.curseforge.com/minecraft/mc-mods/tacz) | Gun raytrace OBB compatibility / 枪械射线 OBB 兼容 |
| CNPC MoreRenderSuppot | Negative-size cube UV and outline-culling fixes for development / 开发环境中的负尺寸 cube UV 与轮廓剔除修复 |

> Better Combat, Iron's Spellbooks and TaCZ integrations are optional and are enabled only when the corresponding mod is installed. CNPC-ImmersiveBoss remains fully usable without them.
>
> Better Combat、Iron's Spellbooks 和 TaCZ 兼容功能均为可选，仅在检测到对应模组时启用。未安装这些模组时，CNPC-ImmersiveBoss 仍可正常运行。

> CNPC-ImmersiveBoss cannot be installed on only one side. Hitbox definitions are parsed from the client resource manager, while animated OBBs and boss bar states are synchronized over the network.
>
> CNPC-ImmersiveBoss 不能只安装在客户端或服务端单侧。模型碰撞箱定义由客户端资源管理器读取，动画 OBB 和 Boss 血条状态通过网络同步。

## Compatibility / 兼容性

- Minecraft 1.20.1
- Forge 47.x, developed with Forge 47.4.20
- Java 17
- GeckoLib 4.8.4
- CustomNPCs Goodbird 1.20.1 unofficial port
- CNPC Gecko Addon CE 1.2.2
- Better Combat 1.9.0 optional compatibility
- Iron's Spellbooks 1.20.1-3.16.2 optional compatibility
- TaCZ 1.1.8-hotfix API optional compatibility
- Blockbench 5.1.6 hitbox preview plugin

## Documentation / 文档

- **Wiki:** [CNPC-ImmersiveBoss Wiki](https://github.com/Sweda666/CNPC-ImmersiveBoss/wiki)
- **Source / 源码:** [GitHub Repository](https://github.com/Sweda666/CNPC-ImmersiveBoss)
- **Blockbench preview tool / Blockbench 预览工具:** [tools/blockbench](https://github.com/Sweda666/CNPC-ImmersiveBoss/tree/ce-addon/tools/blockbench)

Documentation includes installation and quick start, hitbox modeling, combat and interaction, custom boss bar setup, scripting API, troubleshooting and the developer guide.

文档包含安装与快速开始、碰撞箱建模、战斗与交互、自定义 Boss 血条配置、脚本 API、常见问题和开发者指南。
