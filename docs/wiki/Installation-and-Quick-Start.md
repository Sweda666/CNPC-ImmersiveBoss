# 安装与快速开始

## 环境要求

| 组件 | 版本或要求 |
| --- | --- |
| Minecraft | `1.20.1` |
| Forge | `47.x`，开发版本为 `47.4.20` |
| Java | `17` |
| GeckoLib | `4.8.4` |
| CustomNPCs | Goodbird 1.20.1 非官方移植版 |
| CNPC Gecko Addon | 推荐 CE `1.2.2` |

GeckoLib、CustomNPCs、CNPC Gecko Addon 和 CNPC-ImmersiveBoss 都是必要组件。TaCZ 与 CNPC MoreRenderSuppot 仅为可选兼容项。

## 安装

1. 安装 Forge 1.20.1，并确认启动器使用 Java 17。
2. 将四个必要模组的 JAR 放入实例的 `mods/`。
3. 多人游戏中，客户端和服务端安装相同版本的模组及依赖。
4. 将 NPC 的 GeckoLib 模型、贴图和 Boss 血条素材放入资源包，确保所有玩家启用同一资源包。
5. 启动游戏；若加载失败，先查看 `logs/latest.log` 中的依赖或 mixin 错误。

## 第一个碰撞箱

1. 在 Blockbench 的 GeckoLib 模型中新增一个骨骼，命名为 `hdb_body`。
2. 在该骨骼中放入至少一个 cube，并让 cube 覆盖 NPC 身体。
3. 导出 `.geo.json`，通过 CNPC Gecko Addon 将模型设置给 NPC。
4. 进入世界并看见该 NPC，等待客户端完成一次渲染与数据同步。
5. 按 `F3+B`。`hdb_body` 应显示为白色 OBB 线框。
6. 给 NPC 添加 `damaged(e)` 脚本，攻击模型并检查部位名：

```javascript
function damaged(e) {
    e.npc.say("命中: " + e.hitboxName);
}
```

`hdb_body` 中的 `d` 表示可被准星、攻击和交互检测，`b` 表示参与实体推挤。需要更多类型时继续阅读[碰撞箱建模](Hitbox-Modeling.md)。

## 资源重载

修改模型后使用 `F3+T` 重载资源包，并让 NPC 重新进入渲染范围。模型定义来自客户端资源包；多人服务器上只修改服务端文件不会更新玩家看到和同步的 OBB。

[返回 Wiki 首页](Home.md) · [下一页：碰撞箱建模](Hitbox-Modeling.md)
