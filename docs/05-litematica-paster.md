# 05 投影粘贴机

## 用途

按当前 Litematica 投影收集材料、粘贴投影并返还材料。

需要客户端安装 Litematica；在专用服务器上使用 `/paster` 时，服务端也需要安装 CWA。

## 规则

```text
/carpet LitematicaPaster true
```

## 命令

| 命令 | 说明 |
| --- | --- |
| `/paster collect self` | 从玩家背包收集材料 |
| `/paster collect <起点> <终点>` | 从选区内的容器收集材料 |
| `/paster collect list [页码]` | 查看需求、已收和缺失材料 |
| `/paster paste` | 粘贴当前投影 |
| `/paster discard in <容器 ID> <物品 ID>` | 忽略投影中指定容器内的指定物品 |
| `/paster return self` | 将暂存材料返还到玩家背包 |
| `/paster return <起点> <终点>` | 将暂存材料返还到选区内的容器 |

## 说明

- 只收集当前缺少的材料。
- 粘贴成功后才扣除材料，失败或中止不会扣除。
- 每次粘贴前会校验投影文件；修改投影后需要重新收集材料。
- 有暂存材料时，不能卸载对应投影。
