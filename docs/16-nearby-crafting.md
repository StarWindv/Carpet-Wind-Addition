# 16 就近合成

## 用途

在工作台使用配方书放置配方时，自动从附近的容器中取用材料。

## 规则

```text
/carpet NearbyCrafting true
/carpet NearbyCraftingSearchSize 8
```

- `NearbyCraftingSearchSize` 是工作台向各方向搜索的格数，默认 8。
- 搜索范围不能超过当前模拟距离可加载的范围。

## 说明

- 只搜索箱子、木桶和潜影盒，不搜索漏斗、末影箱等其它容器。
- 取用后剩余的材料会放回原容器。
