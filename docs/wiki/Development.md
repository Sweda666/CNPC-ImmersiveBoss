# 开发者指南

## 项目布局

- `src/main/java/sweda/cnpc_immersiveboss/`：模组源码。
- `hitbox/`：模型解析、OBB 数学与伤害窗口。
- `event/`：碰撞、弹射物、同步回退与清理逻辑。
- `mixin/`：CustomNPCs、Gecko Addon、GeckoLib 与可选模组注入。
- `client/`：调试渲染、Boss 血条与配置 GUI。
- `network/`：碰撞箱定义、动画变换与血条同步包。
- `src/main/resources/`：mixin 配置、语言和内置纹理。
- `libs/`：构建所需的本地依赖 JAR，不要删除。

## 构建与运行

项目使用 Java 17、Gradle 8.8 与 ForgeGradle。Windows 命令：

```powershell
.\gradlew.bat compileJava
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runServer
```

构建产物位于 `build/libs/cnpc_immersiveboss-<version>.jar`。仓库没有自动化测试；行为修改应启动游戏，并检查 `run/logs/latest.log` 与 `run/crash-reports/`。

不要并行运行 Gradle 任务。项目关闭了持久 daemon，每次启动较慢，多个进程还会争用缓存锁。

## 可选兼容测试

TaCZ 开发运行环境：

```powershell
.\gradlew.bat runClient -PtaczDevRuntime
```

若 `libs/cnpc_morerendersuppot.jar` 存在，Gradle 会自动将其作为开发运行时依赖。该生产 SRG JAR 必须经 `fg.deobf()`，不要直接复制到 `run/mods/`。

## Mixin 约束

- 新 mixin 必须列入 `src/main/resources/cnpc_immersiveboss.mixins.json`。
- 客户端专用 mixin 放入 `client` 列表。
- 注入 CNPC、Gecko Addon 或 GeckoLib 时使用 `remap = false`、`require = 0`。
- 易漂移的 mixin 路径应保留 Forge 事件回退，并保证重复执行仍幂等。
- 不要在 mixin 类本身保存实体间共享状态；使用 `@Unique` 实例字段或 `api/` 接口。

## 验证清单

修改碰撞逻辑后至少验证：玩家近战、交互、箭/三叉戟、大型 AABB 外 OBB、传感器不推挤、两个 OBB NPC 相交、NPC 死亡/移除清理。修改网络载荷不兼容时同步提升协议版本，并验证客户端与独立服务端。

[上一页：常见问题](Troubleshooting) · [返回 Wiki 首页](Home)
