# CNPC-ImmersiveBoss Wiki

CNPC-ImmersiveBoss 为 CustomNPCs 与 CNPC Gecko Addon 的 GeckoLib NPC 提供随骨骼动画变化的 OBB 多碰撞箱。它让大型或异形 NPC 的受击、交互、弹射物、实体推挤和攻击判定贴合模型，并提供自定义 Boss 血条、分部位脚本事件、碰撞伤害窗口和转向限速 API。

当前文档对应模组版本 `0.3.9`，适用于 Minecraft `1.20.1`、Forge `47.x` 与 Java `17`。

## 从这里开始

1. [安装与快速开始](Installation-and-Quick-Start)：安装依赖并验证第一个 OBB。
2. [碰撞箱建模](Hitbox-Modeling)：理解骨骼命名、Blockbench 坐标和多 cube 规则。
3. [战斗与交互](Combat-and-Interaction)：了解攻击、弹射物、交互、传感器和推挤。
4. [自定义 Boss 血条](Custom-Boss-Bar)：准备 PNG 素材并在 NPC GUI 中配置。
5. [脚本 API](Scripting-API)：编写 `damaged`、`interact`、`collide` 与攻击窗口脚本。
6. [常见问题](Troubleshooting)：使用 `F3+B` 和日志定位问题。
7. [开发者指南](Development)：构建、运行以及修改 mixin 时的注意事项。

## 功能速览

| 功能 | 用途 |
| --- | --- |
| 动画 OBB | 碰撞箱跟随 GeckoLib 骨骼平移、旋转和动画 |
| 分部位事件 | 在 `damaged(e)`、`interact(e)` 中读取 `e.hitboxName` |
| 物理箱与传感器 | 分离阻挡推挤和纯重叠检测 |
| 弹射物兼容 | 支持箭、三叉戟等原版弹射物，以及可选 TaCZ 枪械兼容 |
| 碰撞伤害窗口 | 让武器骨骼在指定动画帧造成伤害并执行回调 |
| 自定义血条 | 用上下分层 PNG 制作常显或战斗时显示的 Boss 血条 |
| 调试渲染 | `F3+B` 查看 OBB 类型、名称、局部轴和重叠状态 |

## 安装范围

客户端和服务端必须安装相同版本的本模组及必要依赖。模型定义由客户端资源管理器读取，动画 OBB 和血条状态通过网络同步，因此本模组不能只安装在单侧。资源包也应向所有玩家分发。

项目首页与完整单页参考见仓库 [README](https://github.com/Sweda666/CNPC-ImmersiveBoss/blob/ce-addon/README.md)。
