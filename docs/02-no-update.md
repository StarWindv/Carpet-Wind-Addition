# 02 禁止区域内方块更新

## 用途

按维度保存选区，阻止目标方块接收邻接更新和形状更新。

## 用法

```text
/carpet NoUpdate range <起点> <终点> [别名]
/carpet NoUpdate list
/carpet NoUpdate enable <编号|别名>
/carpet NoUpdate disable <编号|别名>
/carpet NoUpdate remove <编号|别名>
```

- 坐标支持 `~` 等相对坐标。
- `[别名]` 可选，不能是 `-`、纯数字或已有别名。
- 新选区默认禁用，用 `enable` 开启。
- 同一维度内不能创建重叠选区；`list` 只显示当前维度。
- 只抑制邻接更新和形状更新，不取消计划刻、流体更新和实体更新。
