# Carpet Wind Addition

本模组作为 Carpet 的扩展模组, 为其增加了部分增强功能

## ToC

- [Carpet Wind Addition](#carpet-wind-addition)
  - [ToC](#toc)
  - [I. 功能概览](#i-功能概览)
    - [1.1 权限控制器](#11-权限控制器)
    - [1.2 禁止区域内方块更新](#12-禁止区域内方块更新)
    - [1.3 吃不停](#13-吃不停)
    - [1.4 活塞采集紫水晶簇](#14-活塞采集紫水晶簇)
    - [1.5 投影**粘贴**机](#15-投影粘贴机)
  - [II. License](#ii-license)

---

## I. 功能概览

### 1.1 权限控制器

仅单人存档或作为局域网主机可用.

该功能只降低主机使用Command时的权限门槛, 不会自动授权给客机.

Usage: 

```text
/allow
/allow <Command> [true|false]
```

- `/allow`: 列出当前允许的Command. 
- `/allow <Command>`: 允许对应CommandCommand. 
- `/allow <Command> false`: 取消允许. 
- Command 不需要带 `/`, 例如 `/allow gamemode` 

### 1.2 禁止区域内方块更新

通过 `/carpet NoUpdate` 管理按维度保存的更新抑制选区. 选区默认创建为禁用状态. 

```text
/carpet NoUpdate range <起点> <终点> [可选别名]
/carpet NoUpdate list
/carpet NoUpdate enable <编号|别名>
/carpet NoUpdate disable <编号|别名>
/carpet NoUpdate remove <编号|别名>
```

- `<起点>`、`<终点>`: 使用方块坐标, 支持 `~` 等相对坐标. 
- `[别名]`: 可选, 不能是 `-`、纯数字或已有别名. 
- 同一维度内不允许创建范围重叠的选区. 
- `list` 显示当前维度的全部选区; `enable` / `disable` 控制是否生效. 
- 选区保存于世界数据, 重进世界后仍会保留. 

更新抑制在**目标方块**位于已启用选区内时生效, 目前拦截: 

- `handleNeighborChanged` 邻接方块更新
- `updateShape` 形状更新

该功能不会取消计划刻、流体更新、实体更新等其它更新来源. 

### 1.3 吃不停

Carpet 规则: 

```text
/carpet NeverFull true
```

默认关闭. 开启后, 玩家在饥饿值已满时仍然可以进食, 食物效果和饱和度正常结算. 

### 1.4 活塞采集紫水晶簇

Carpet 规则: 

```text
/carpet PistonHarvestAmethyst true
```

默认关闭. 开启后, 活塞破坏紫水晶簇时会掉落紫水晶簇物品, 而不是紫水晶碎片. 

### 1.5 投影**粘贴**机

该功能用于按当前 Litematica 投影收集材料、粘贴投影以及返还材料, 安装投影和投影打印机时可用, 相当于在生存中消耗材料直接使用投影的模式 5

Usage:

```text
/carpet LitematicaPaster <true|false>
```

默认关闭.

启用后, 主机可使用 `/paster` Command. 

值得注意的是, 粘贴器并不需要权限或者创造模式.

**Command**

| Command | 说明 |
| --- | --- |
| `/paster collect self` | 从玩家背包收集当前投影所需材料, 包含背包内潜影盒的内容, 并扣除对应物品 |
| `/paster collect <起点> <终点>` | 从选区内的容器收集材料并扣除, 支持箱子、木桶、放置的潜影盒, 以及容器内潜影盒物品的内容 |
| `/paster collect list [页码]` | 显示当前投影的材料列表, 每页 10 种, 可查看需求、已收和缺失数量 |
| `/paster paste` | 检查材料是否充足, 然后粘贴当前投影 |
| `/paster return self` | 将暂存材料返还到玩家背包, 放不下的继续保留在数据中 |
| `/paster return <起点> <终点>` | 将暂存材料返还到选区内的容器, 放不下的继续保留在数据中 |

示例: 

```text
/paster collect self
/paster collect 100 60 100 140 80 140
/paster collect list
/paster collect list 2
/paster paste
/paster return self
```

**材料记录与提示**

- 只收集当前仍缺少的数量, 不会多收. 
- 材料按投影和物品组件精确匹配. 
- 收集结果会显示本次收集到的材料, 以及每种材料在本次收集后仍缺多少. 
- `collect list` 使用对齐表格显示需求、已收、缺失; 缺失数量为红色, 已满足为绿色, 行使用灰白交错颜色. 
- 模式 9 修改投影后如果出现多余材料, 列表会以 `*` 标记为额外暂存材料. 
- 粘贴开始前会检查当前投影的材料是否充足; 不足时拒绝粘贴, 并提示第一种缺失材料及数量, 完整缺失列表可用 `collect list` 查看. 

**粘贴与材料消耗**

- 粘贴开始时会在投影各子区域建立临时更新抑制, 粘贴结束后恢复. 
- 粘贴成功后会扣除本次投影所需的材料, 只保留额外多收集的部分. 
- 粘贴失败或中止不会扣除材料, 之后仍可返还. 
- 粘贴进行中禁止 `/paster return`, 避免在任务完成前取回材料. 
- 一次粘贴完成后, 所需材料会被消耗; 需要再次粘贴时必须重新收集. 

**投影卸载保护**

收集材料后, 模组会禁止玩家在 Litematica 配置页面卸载相关投影.

只要对应投影仍有 paster 暂存材料, 就不能从投影列表卸载它; 尝试卸载时会收到提示. 材料全部返还后, 保护自动解除. 

---

## II. License

本项目以 Apache-2.0 协议开源
