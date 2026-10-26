# 05 投影粘贴机

## 用途

按当前 Litematica 投影收集材料、粘贴投影并返还材料，相当于在生存中消耗材料直接使用投影的模式 5。

- 单人游戏使用原有的客户端/服务端一体化路径。
- 专用服务器或局域网客机上，由服务端保存并校验材料，客户端通过 CWA 数据包转发 Litematica 的粘贴命令。
- 客户端需要安装 CWA 和 Litematica；服务端需要安装 CWA；不需要 Servux。
- 专用服务器上普通玩家即可使用 `/paster`，不需要 OP；是否开启由服务器管理员决定。

## 规则

```text
/carpet LitematicaPaster true
```

## 命令

| 命令 | 说明 |
| --- | --- |
| `/paster collect self` | 从玩家背包收集当前投影所需材料，包含背包内潜影盒的内容 |
| `/paster collect <起点> <终点>` | 从选区内的容器收集材料，支持箱子、木桶、放置的潜影盒及容器内潜影盒的内容 |
| `/paster collect list [页码]` | 显示当前投影的材料列表，每页 10 种，可查看需求、已收和缺失数量 |
| `/paster paste` | 检查材料是否充足，然后粘贴当前投影 |
| `/paster discard in <容器 ID> <物品 ID>` | 忽略当前投影中指定容器内的指定物品，粘贴后也会移除 |
| `/paster return self` | 将暂存材料返还到玩家背包，放不下的继续保留在数据中 |
| `/paster return <起点> <终点>` | 将暂存材料返还到选区内的容器，放不下的继续保留在数据中 |

示例：

```text
/paster collect self
/paster collect 100 60 100 140 80 140
/paster collect list
/paster paste
/paster return self
```

## 材料记录

- 只收集当前仍缺少的数量，材料按投影和物品组件精确匹配。
- `collect list` 使用表格显示需求、已收、缺失；缺失为红色，已满足为绿色。
- 模式 9 修改投影后如果出现多余材料，列表会以 `*` 标记为额外暂存材料。
- `/paster return <起点> <终点>` 会输出实际接收到物品的容器坐标。

## 粘贴与校验

- 第一次执行 `/paster` 时，服务端向客户端请求投影快照，包括投影 UUID、文件 SHA-256、子区域范围和材料需求。
- 服务端的投影身份是 `玩家 UUID + Litematica 投影 UUID`，不使用投影名称。
- 每个玩家同一时间最多进行一个粘贴任务；不同玩家可以并行粘贴。
- 每次粘贴前都会重新核对投影文件指纹，收集材料后修改本地投影文件会导致粘贴被拒绝。
- 粘贴开始时会在投影各子区域建立临时更新抑制，粘贴结束后恢复。
- 粘贴成功后才扣除材料；失败或中止不会扣除，之后仍可返还。
- 粘贴进行中禁止 `/paster return`。

## 投影卸载保护

只要对应投影仍有 paster 暂存材料，就不能从 Litematica 投影列表卸载它；尝试卸载时会收到提示。材料全部返还后保护自动解除。

## 调试命令

```text
/paster debug backpack_info
/paster debug return self
/paster debug return <起点> <终点>
```

用于排查暂存材料和背包数据，正常使用不需要。
