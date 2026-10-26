# 01 权限控制器

## 用途

在不给客机 OP 的情况下，让主机把指定命令放行给客机；主机自己也可以用它降低命令的权限门槛。

仅单人存档或作为局域网主机时可用。

## 规则

```text
/carpet AllowSharePermission true
```

开启后，客机也能使用主机通过 `/allow` 放行的命令。`/allow` 本身仍然只有主机能使用。

## 用法

```text
/allow
/allow <命令>
/allow <命令> false
```

- `/allow`：列出当前已放行的命令。
- `/allow <命令>`：放行命令。
- `/allow <命令> false`：取消放行。
- 命令不需要带 `/`，例如 `/allow gamemode`。
- 被放行的命令在客机客户端也会正常解析实体选择器。
