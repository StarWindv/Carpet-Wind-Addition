# 01 权限控制器

## 用途

让主机把指定命令放行给客机。

仅单人存档或作为局域网主机时可用。

## 规则

```text
/carpet AllowSharePermission true
```

开启后，客机也能使用主机通过 `/allow` 放行的命令。

## 用法

```text
/allow
/allow <命令>
/allow <命令> false
```

- `/allow`：列出已放行的命令。
- `/allow <命令>`：放行命令。
- `/allow <命令> false`：取消放行。
- 命令不需要带 `/`，例如 `/allow gamemode`。
