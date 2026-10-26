# Carpet Wind Addition

Carpet Wind Addition（简称 CWA）是 Carpet 的 Fabric 扩展模组，提供一些不那么香草的功能扩展。

## 安装

1. 安装 Fabric Loader 和 Fabric API。
2. 将 Carpet 和 CWA 放入 `mods` 目录。
3. 需要使用投影粘贴时，在客户端额外放入 Litematica。
4. 启动游戏。

## 规则

所有 CWA 开关型规则默认关闭，使用 `/carpet <规则> true` 开启。规则位于 `/carpet` 的 `CWA` 分类中。

## 文档

完整的功能列表、规则和命令见 [docs/index.md](docs/index.md)。

常用入口：

- [投影粘贴机](docs/05-litematica-paster.md)
- [熔炉清理](docs/06-furnace-cleaner.md)
- [容器清理](docs/07-container-cleaner.md)
- [物品堆叠上限](docs/08-stack-anything.md)

## 从源码构建

```bash
./gradlew build
```

## License

本项目以 Apache-2.0 协议开源。

