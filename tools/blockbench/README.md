# CNPC ImmersiveBoss Hitbox Preview

这个 Blockbench 插件会把 ImmersiveBoss 碰撞箱骨骼中的每个 cube 显示为与游戏 `F3+B` 调试视图一致的彩色 OBB 边框。边框预览对象只存在于编辑器视口中，不会修改模型几何，也不会写入导出的 `.geo.json`；“全部隐藏”会临时切换碰撞箱组及其 cube 的编辑器可见性，并在关闭开关或卸载插件时恢复。

## 安装

1. 在 Blockbench 中打开“文件 -> 插件”。
2. 选择“从文件加载插件”，加载 `cnpc_immersiveboss_hitbox_preview.js`。
3. 在“视图”菜单中控制边框、全部隐藏、局部 XYZ 轴和游戏内可见性预览。

插件已针对 Blockbench 5.1.6 的 GeckoLib 模型格式验证。

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

勾选“Hide All ImmersiveBoss Hitboxes”会直接把所有碰撞箱骨骼组以及组内 cube 的 Blockbench 原生可见性暂时设为“不可见”。碰撞箱实体、边框和局部轴都会消失，并且无法在视口中点选；组和 cube 仍可从右侧大纲栏目中选中。取消勾选后会分别恢复每个组和 cube 在勾选前的可见状态。这个总隐藏开关独立于边框预览开关，即使关闭边框预览也会继续隐藏碰撞箱组和 cube。

游戏中的红色表示 OBB 正与另一个实体碰撞，需要场景中的实体状态，因此 Blockbench 静态预览不会模拟红色碰撞态。
