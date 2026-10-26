# 06 熔炉清理

## 用途

清理熔炉、高炉和烟熏炉中无法烧炼或不能作为燃料的物品。

## 规则

```text
/carpet FurnacesCleaner true
```

## 用法

```text
/furnace <起点> <终点> self
/furnace <起点> <终点> discard
/furnace <起点> <终点> to <起点> <终点>
```

- 坐标支持 `~` 等相对坐标。
- 清理输入槽和燃料槽；输出槽不清理。
- `self` 放入玩家背包，`discard` 丢弃，`to` 放入选区内的容器。
- 接收容器不包括熔炉；放不下的物品留在原槽位。
