# CWA 功能文档

每个功能一份独立文档，按编号排列。

> 本文档对应 26.3 分支。1.21.1 分支的功能以该分支实际规则为准。

| 编号 | 功能 | 主要规则 / 命令 |
| --- | --- | --- |
| [01](01-permissions.md) | 权限控制器 | `/allow`、`AllowSharePermission` |
| [02](02-no-update.md) | 禁止区域内方块更新 | `/carpet NoUpdate ...` |
| [03](03-never-full.md) | 吃不停 | `NeverFull` |
| [04](04-piston-harvest-amethyst.md) | 活塞采集紫水晶簇 | `PistonHarvestAmethyst` |
| [05](05-litematica-paster.md) | 投影粘贴机 | `LitematicaPaster`、`/paster` |
| [06](06-furnace-cleaner.md) | 熔炉清理 | `FurnacesCleaner`、`/furnace` |
| [07](07-container-cleaner.md) | 容器清理 | `ContainerCleaner`、`/cc` |
| [08](08-stack-anything.md) | 物品堆叠上限 | `StackAnything`、`MaxStackAnythingSize`、`/stack` |
| [09](09-mob-equipment.md) | 怪物装备 | `MobsNoEquipmentDrop`、`MobsNoSpear` |
| [10](10-obsidian-to-lava.md) | 黑曜石与岩浆 | `BucketTansOsidianToLava`、`ObsidianToLavaRecipe` |
| [11](11-quick-ice-to-water.md) | 快捷冰化水 | `QuickIceToWater`、`QuickITWCheck` |
| [12](12-quick-place-obsidian.md) | 快捷放置黑曜石 | `QuickPlaceObsidian` |
| [13](13-hot-tnt.md) | 高温 TNT | `HotTNT` |
| [14](14-waterproof-blocks.md) | 防水方块 | `WaterproofRedstoneComponents`、`WaterproofCarpet` |

## 权限说明

`/furnace`、`/cc`、`/stack` 默认需要权限等级 2。单人存档/局域网主机不受此限制；客机可以通过 `/allow` 放行，详见 [权限控制器](01-permissions.md)。

`/paster` 在专用服务器上不要求 OP，是否可用由 `LitematicaPaster` 规则决定。
