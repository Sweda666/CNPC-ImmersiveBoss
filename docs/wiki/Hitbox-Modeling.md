# 碰撞箱建模

## 骨骼命名

模组从 `.geo.json` 的骨骼名识别碰撞箱：

```text
h[a][d][b|s]_名称
```

| 标记 | 含义 |
| --- | --- |
| `h` | 碰撞箱前缀，必须位于开头 |
| `a` | appearance，保留 cube 的可见外观 |
| `d` | detectable，可被准星、攻击和交互检测 |
| `b` | blocking，参与实体物理推挤 |
| `s` | sensor，只检测重叠，不产生推挤 |

`b` 与 `s` 必须二选一并位于属性末尾；`a`、`d` 可省略。推荐按 `a`、`d` 的顺序命名，例如：

| 名称 | 行为 | 用途 |
| --- | --- | --- |
| `hb_body` | 隐藏、阻挡 | 纯物理范围 |
| `hdb_head` | 隐藏、可检测、阻挡 | 头部受击箱 |
| `hds_warning` | 隐藏、可检测、不阻挡 | 交互或预警区域 |
| `hads_sword` | 可见、可检测、不阻挡 | 动画武器与攻击范围 |
| `hadb_shield` | 可见、可检测、阻挡 | 可攻击的盾牌部件 |

## Blockbench 制作规则

- 碰撞箱骨骼至少包含一个有效 cube；空骨骼不会生成 OBB。
- 骨骼 `pivot` 是绝对模型空间坐标，不是相对父骨骼的位移。
- cube 旋转按 Blockbench/GeckoLib 的 `Z -> Y -> X` 顺序应用。
- 碰撞箱可以成为动画骨骼的子骨骼；运行时 OBB 会跟随实际渲染矩阵。
- 碰撞范围取自 cube 的 `origin`、`size`、`pivot` 和 `rotation`，与贴图可见像素无关。

简化示例：

```json
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

## 多 cube 骨骼

同一骨骼的所有 cube 都会生成 OBB。首个使用原名，后续内部名称依次为 `hadb_tail__1`、`hadb_tail__2`。脚本事件与 API 会归一化到基础名 `hadb_tail`，脚本不要依赖 `__1` 后缀。

## 设计建议

- 身体主要部位使用少量 `hdb_`，兼顾受击和阻挡。
- 武器、技能范围和预警区优先使用 `hds_`/`hads_`，避免攻击动画把玩家推开。
- 大型模型需要远离 NPC 原点仍可选中的部位必须带 `d`。
- OBB 越多，实体重叠与 OBB 对 OBB 检测成本越高；不要用大量小 cube 描摹纯视觉细节。

[上一页：安装](Installation-and-Quick-Start) · [下一页：战斗与交互](Combat-and-Interaction)
