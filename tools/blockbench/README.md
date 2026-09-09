# CNPC ImmersiveBoss Hitbox Preview

这个 Blockbench 插件会把 ImmersiveBoss 碰撞箱骨骼中的每个 cube 显示为与游戏 `F3+B` 调试视图一致的彩色 OBB 边框。边框预览对象只存在于编辑器视口中，不会修改模型几何，也不会写入导出的 `.geo.json`；“全部隐藏”会临时切换碰撞箱组及其 cube 的编辑器可见性，并在关闭开关或卸载插件时恢复。

## 安装

1. 在 Blockbench 中打开“文件 -> 插件”。
2. 选择“从文件加载插件”，加载 `cnpc_immersiveboss_hitbox_preview.js`。
3. 在插件详情页的 Settings 中配置边框、局部 XYZ 轴和游戏内可见性；View 菜单仅提供“Hide All IB hitboxes”。

插件已针对 Blockbench 5.1.6 的 GeckoLib 模型格式验证。

## 摄像机视角预览

进入动画编辑页面后，左上方会显示“摄像机视角预览”面板。面板顶部的下拉框可以切换第一人称、第二人称和第三人称，分别读取 `camera_root`、`camera_second_person` 和 `camera_third_person` 的实时动画变换；摄像机 FOV 则优先读取对应的 Camera 组件。拖动时间线、播放动画或编辑摄像机骨骼时，预览会同步更新模型、材质和玩家皮肤。

预览面板只在动画模式显示。即使没有启用 Cameras 插件，它仍可通过摄像机骨骼预览；如果模型缺少所选骨骼，面板会显示提示。面板可以像 Blockbench 其他面板一样折叠、缩放或移动。

## 预览规则

插件使用与模组 `GeoHitboxDef.classify` 相同的 `h[a][d][b|s]_名称` 规则，只处理碰撞箱骨骼直接包含的 cube。多 cube 骨骼会为每个 cube 分别绘制边框，骨骼动画和 cube 旋转会实时带动边框。

编辑模型或动画后执行撤销/重做时，插件会在 Blockbench 完成视口重建后重新应用碰撞箱外观，不带 `a` 的 cube 不会因为 `Ctrl+Z` 而重新显示。

| 边框颜色 | 属性 |
| --- | --- |
| 白色 | 物理 `b` 且可检测 `d` |
| 蓝色 | 物理 `b`，不带 `d` |
| 黄色 | 传感器 `s` 且可检测 `d` |
| 绿色 | 传感器 `s`，不带 `d` |

“ImmersiveBoss Runtime Visibility”开启时，不带 `a` 的碰撞箱 cube 会隐藏实体表面，只保留调试边框；带 `a` 的 cube 仍保持正常外观。这项设置同样只影响 Blockbench 视口。

勾选“Hide All IB hitboxes”会直接把所有碰撞箱骨骼组以及组内 cube 的 Blockbench 原生可见性暂时设为“不可见”。碰撞箱实体、边框和局部轴都会消失，并且无法在视口中点选；组和 cube 仍可从右侧大纲栏目中选中。取消勾选后会分别恢复每个组和 cube 在勾选前的可见状态。这个总隐藏开关独立于边框预览开关，即使关闭边框预览也会继续隐藏碰撞箱组和 cube。

游戏中的红色表示 OBB 正与另一个实体碰撞，需要场景中的实体状态，因此 Blockbench 静态预览不会模拟红色碰撞态。

## 创建投技玩家模型组

在“工具 -> Create IB Player Rig”执行一键创建投技玩家模型组。操作需要 Blockbench 的 Cameras 插件（`OutlinerElement.types.camera`）处于启用状态。

生成的结构如下：

```text
victim_root
├── victim_body
│   ├── victim_body_cube
│   └── victim_body_layer_cube
├── victim_head
│   ├── victim_head_cube
│   ├── victim_head_layer_cube
│   ├── camera_root
│   │   └── throw_camera
│   ├── camera_second_person
│   │   └── throw_camera_second_person
│   └── camera_third_person
│       └── throw_camera_third_person
├── victim_right_arm (base + layer)
│   └── victim_right_item
├── victim_left_arm (base + layer)
│   └── victim_left_item
├── victim_right_leg (base + layer)
└── victim_left_leg (base + layer)
```

The generated arm layout follows Blockbench's vanilla player reference:
`victim_right_arm` is on the model's `+X` side and `victim_left_arm` is on
the `-X` side. Re-running `Create IB Player Rig` upgrades an untouched
legacy rig that used the reversed arm layout; customized arm geometry is left
unchanged.

玩家模型方块使用原版 64x64 玩家皮肤 UV 布局，并包含头部帽子、身体外套、左右袖子和左右裤腿的第二层皮肤几何。第二层使用对应的膨胀量和原版 UV 偏移，渲染正版皮肤时会与基础层同步显示。`victim_root` 是投技中显示的玩家人偶。三个摄像机骨骼分别对应第一人称 `camera_root`、第二人称正面视角 `camera_second_person` 和第三人称背面视角 `camera_third_person`；每个骨骼都有一个对应的 Blockbench Camera 组件（`throw_camera`、`throw_camera_second_person`、`throw_camera_third_person`），便于在编辑器内预览和调整投技视角。摄像机骨骼本身的旋转就是游戏内摄像机朝向，不需要 `camera_look` 骨骼。

如果工程中已经存在不完整的 `victim_root`，再次执行 Create IB Player Rig 会从已有身体方块推断替身的实际模型空间位置，自动补齐缺失的身体骨骼、基础方块、第二层皮肤、摄像机骨骼、Camera 组件和 `victim_left_item` / `victim_right_item` 手持物品组，不会重复创建已有内容。旧工程中与身体骨骼同名的基础层/皮肤层方块也会被识别；即使 `victim_root` 嵌套在其他带旋转的骨骼下，补全部件仍会落在现有替身位置。已有组的位置、旋转、缩放和动画保持不变。

新建或补全玩家替身会写入一条完整的 Blockbench 大纲撤销记录。执行一次 `Ctrl+Z` 即可撤销本次新增的所有 Group、Cube 和 Camera；补全旧版左右手布局时产生的位置修正也包含在同一条撤销记录中。

玩家替身皮肤、碰撞箱和摄像机画面都属于当前项目的临时视口预览。点击标签栏 `+` 返回主页或切换到其他项目时，插件会在不访问已卸载 Outliner 的情况下清理这些预览，不会中断 Blockbench 的标签切换。

Camera 组件仅用于 Blockbench 编辑器预览，不会作为 GeckoLib 几何骨骼导出。

### 预览正版玩家皮肤

插件默认使用正版玩家名 `Sweda`：创建投技玩家模型后会自动尝试加载该皮肤，皮肤设置对话框也会预填 `Sweda`。请在插件详情页的 Settings 中使用“Set Official Minecraft Player Skin”改用其他 Minecraft Java 玩家名。插件会依次访问 Mojang 的用户档案、会话纹理资料和官方 `textures.minecraft.net` 图片，并将皮肤直接覆盖到 `victim_root` 的视口模型上。覆盖使用临时 Three.js 网格和原版 64x64 UV，不会创建 Blockbench `Texture` 资源、改写任何 cube 的 `faces.texture`，也不会影响工程中的其他模型；切换工程或卸载插件时会自动恢复原材质。该皮肤只保存在 Blockbench 工程中用于预览；游戏内仍会在投技开始时按实际目标玩家替换材质。

如果将摄像机骨骼移到其他骨骼，插件和游戏仍会按名称全局解析，不要求必须位于 `victim_head` 内。旧模型缺少第二或第三人称摄像机时，游戏会根据 `camera_root` 的位置和朝向计算默认视角。

## 脚本调用

在 NPC 的脚本事件中可通过 `Java.type` 调用投技 API。`animation` 是 GeckoLib 动画名，`durationTicks` 是持续时间（20 tick = 1 秒）：

```js
var ImmersiveBossAPI = Java.type('sweda.cnpc_immersiveboss.api.ImmersiveBossAPI');

function interact(e) {
    // e.player 是被投技的玩家；也可以在 damaged/collide 事件中传入目标实体
    ImmersiveBossAPI.startThrow(npc, e.player, 'throw suplex', 30);

    // NPCWrapper 也提供同样的快捷方法：
    npc.startThrow(e.player, 'throw suplex', 30);
}
```

投技开始后，`victim_root` 自动显示并使用目标玩家皮肤，真实玩家模型会在客户端隐藏；动画结束或调用 `stopThrow(target)` 后恢复玩家位置、重力和旋转。游戏会根据玩家当前的第一/第三人称选项选择对应摄像机骨骼；缺少第二或第三人称骨骼时，会从 `camera_root` 的位置和朝向计算默认视角。
