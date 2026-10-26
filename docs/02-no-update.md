# 02 禁止区域内方块更新

## 用途

按维度保存更新抑制选区，阻止目标方块接收邻接更新和形状更新，适合测试或保护特定区域。

## 用法

```text
/carpet NoUpdate range <起点> <终点> [别名]
/carpet NoUpdate list
/carpet NoUpdate enable <编号|别名>
/carpet NoUpdate disable <编号|别名>
/carpet NoUpdate remove <编号|别名>
```

- `<起点>`、`<终点>`：方块坐标，支持 `~` 等相对坐标。
- `[别名]`：可选，不能是 `-`、纯数字或已有别名。
- 同一维度内不允许创建范围重叠的选区。
- 新选区默认是禁用状态，保存在世界数据中，重进世界后仍保留。
- `list` 只显示当前维度的选区。

## 拦截范围

目前只拦截：

- `handleNeighborChanged` 邻接方块更新
- `updateShape` 形状更新

不会取消计划刻、流体更新、实体更新等其它更新来源。
