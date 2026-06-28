# Ponderer-server 文档

Ponderer-server 为 [Ponderer](https://github.com/Nobodiiiii/Ponderer) 提供 Bukkit 系服务端支持，并补上适合服务器管理的同步、审核、权限、举报、备份和 AI 代理能力。

这个文档站面向服主和管理员，重点说明实际部署时最常用的三件事：

- 玩家和管理员可以使用哪些命令
- 每个权限节点默认给谁
- 常用配置项控制什么行为

## 组成

| 部分 | 放在哪里 | 作用 |
| --- | --- | --- |
| 服务端插件 | `plugins` | 保存、同步、审核和管理思索场景 |
| 客户端 all-in-one | `mods` | 包含 Ponderer 和客户端附加包，推荐普通玩家安装 |
| 客户端 standalone addon | `mods` | 只包含附加包，需要玩家另外安装 Ponderer |

## 基本要求

| 项目 | 要求 |
| --- | --- |
| Minecraft | 1.21.1 |
| Java | 21 |
| 服务端 | Bukkit 系服务端，推荐 Paper |
| Fabric Loader | 0.16.14+ |
| NeoForge | 推荐 21.1.219+ |
| LuckPerms | 可选，推荐用于持久权限和上传上限管理 |

## 客户端安装建议

普通玩家推荐安装 all-in-one 客户端包。它包含 Ponderer 本体和 Ponderer-server 的客户端附加包，可以正常使用服务器下发的同步、上传、编辑、AI 代理等功能。

如果玩家只安装 Ponderer，没有安装客户端附加包，Ponderer 的本地功能仍可使用，但服务器需要通过客户端附加包执行的命令不会完整工作。

如果玩家完全不安装客户端相关 mod，仍可以使用一部分由服务端直接处理的命令，例如举报、订阅、锁定、可见性设置；但不能进行客户端场景同步、编辑桥接和 AI 代理调用。

## 文档入口

- [命令](./commands.md)：玩家命令、客户端桥接命令、管理员命令
- [权限](./permissions.md)：权限节点、默认权限、推荐 LuckPerms 分组
- [配置](./config.md)：功能开关、审核、举报、AI、备份和上传限制
