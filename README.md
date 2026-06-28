# Ponderer-server

为 Minecraft Java 模组 [Nobodiiiii/Ponderer](https://github.com/Nobodiiiii/Ponderer) 提供了 bukkit 系服务端支持，并添加了许多易于服务器管理的功能。

## 兼容的游戏版本与平台

`-` 代表无支持计划，✅后面的数字代表加载器版本限制

|            | < 1.20.1 | 1.20.1 | 1.21.1         | > 1.21.1 |
|:----------:|:--------:|:------:|:--------------:|:--------:|
| bukkit 服务端 | -        | 有需求后增加 | ✅              | 同下方说明    |
| forge      | -        | 有需求后增加 | -              | 同下方说明    |
| fabric     | -        | 有需求后增加 | ✅ \| 0.16.14+  | 同下方说明    |
| neoforge   | -        | -      | ✅ \| 21.1.219+ | 同下方说明    |

在 1.21.1 后的版本，会在 Ponderer 发布新版本后不久增加支持。

服务器端可选增加 [LuckPerms](https://luckperms.net/)，获得更好的体验。

## 安装

下面的 `[version_id]` 代表插件版本号，例如 `1.0`；

`[loader]` 代表加载器，如 `fabric`；

`[mc_version]` 代表对应的 Minecraft 版本，如 `mc1.21.1`。

### 服务端

将 `ponderer_server_[version_id].jar` 放入`./plugins` 文件夹即可。

第一次启动会自动生成配置文件 `config.yml` 和信息配置文件 `messages.yml`，可随心配置。

### 客户端

本质上来说，这个 mod 其实是一个 Ponderer 的附加包，但由于所有 Ponderer 的命令在 bukkit 系服务器上实现的功能，都是服务端检查权限后，通过 plugin message 下发给附加包执行。导致如果没有附加包，这些指令根本不会执行。因此，这个附加包几乎必装。

提供两个版本：

#### 多合一版本

文件名：`ponderer_client_all_in_one_[version_id]_[mc_version]_[loader]`。

这个 mod 集成了所有 Ponderer 的功能和本 mod 的功能，推荐安装。

将其拖入 `./mods` 文件夹中即可。

#### 单独附加版本

如果你有某些额外的需求，我们提供单独的附加包。

文件名：`ponderer_client_addon_[version_id]_[mc_version]_[loader]`。

将其拖入 `./mods` 文件夹中即可。注意此附加包需要 Ponderer 作为前置。

## 实现的功能

- 在 bukkit 系服务器实现了 `/ponderer pull` `/ponderer push` 的场景上传与拉取功能。

- 支持一并上传引用的 `.nbt` 结构。

- 支持对权限、大小、冷却、频率、上传数量、区域场景权限、NBT 限制、物品过滤进行上传时校验。

- 支持通过同步 hash 判断服务器版本和客户端版本是否冲突。

- 支持设定场景所有权，支持设定可修改场景的协作者，支持创作者锁定场景，协作者可设定全局和单场景。

- 支持设定场景可见性，分为全局可见，组内成员可见，仅自己可见。

- 支持场景审核，可设定为仅管理员审核，AI 自动审核，自动通过。

- 支持玩家举报，在举报达到设定次数后自动隐藏并复审。

- 支持玩家订阅场景，场景更新后通知订阅者。

- 支持自动场景备份和恢复。

- 支持场景自动归档，可对长时间未访问的场景归档。

- 支持服务端 AI 代理，在客户端没有配置 AI api key 时间，统一使用服务器提供的 api key。

- 支持服务器管理单个玩家的服务器 AI 调用 token。

- 支持不同存档/服务器思索场景隔离。

- 在生存模式下也可调用思索编辑。

- 高度可自定义化，所有功能支持自主开关，所有显示文本支持自定义，支持通过 LuckPerms 进行持久权限和上传上限管理。

## 命令，权限组，配置信息

见 [Ponderer-server 文档 | Ponderer-server](https://px-poxiao.github.io/Ponderer-server/)，托管于 GitHub Pages。
