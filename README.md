# Ponderer Server / Ponderer AI Addon 项目说明

这个目录是围绕 Ponder 与 Ponderer 的服务器端与客户端附加开发工作区。

Ponder 是 Minecraft 中用于展示交互式教程/思索场景的基础功能；Ponderer 是在 Ponder 之上做的可视化编辑器，让玩家可以在游戏内创建、编辑、预览和同步思索场景。本目录中真正新增的两部分是：

- `Ponderer_server`：Paper 服务器插件，负责场景集中存储、权限控制、上传审核、可见性、协作、举报、订阅通知、备份、过期归档，以及服务器托管的 AI API 代理。
- `PondererAI_addon`：Fabric 1.21.1 客户端附加包，依赖 Ponderer 本体，用来接入服务器 AI 代理、接收服务器下发的客户端命令，并提示本地场景文件变化。

旁边的 `Ponder-mc1.21.1-dev`、`Ponderer-1.20.1`、`Ponderer-1.21.1`、`Ponderer-1.2.2-1.21.1neoforge` 是 Ponder/Ponderer 本体或多版本代码，用作依赖、参考或开发背景。下面的重点是服务端插件和客户端附加包已经实现的功能。

## 目录结构

```text
.
├─ Ponderer_server/                 Paper 1.21.1 服务端插件
│  ├─ src/main/java/com/ponderer/server/
│  │  ├─ ai/                         服务器 AI 代理与 provider
│  │  ├─ commands/                   /ponderer 与 /ponderer-admin
│  │  ├─ config/                     config.yml / messages.yml 读取
│  │  ├─ handler/                    同步、上传、结构导入、审核 AI 处理
│  │  ├─ listener/                   玩家上线通知
│  │  ├─ network/                    Bukkit Plugin Message 协议
│  │  ├─ permissions/                LuckPerms 权限和 meta 管理
│  │  ├─ ratelimit/                  上传频率限制
│  │  ├─ stats/                      活动日志
│  │  └─ storage/                    场景与各种元数据存储
│  └─ src/main/resources/
│     ├─ plugin.yml                  Paper 插件声明、命令、权限
│     ├─ config.yml                  服务端配置
│     └─ messages.yml                消息文本
├─ PondererAI_addon/                 Fabric 1.21.1 客户端附加包
│  ├─ src/main/java/com/ponderer/addon/
│  │  ├─ mixin/                      接管 Ponderer AI 生成入口
│  │  └─ network/                    客户端自定义 payload
│  └─ src/main/resources/
│     ├─ fabric.mod.json
│     └─ ponderer_addon.mixins.json
├─ Ponderer-1.21.1/                  Ponderer 多平台主体，1.21.1
├─ Ponderer-1.20.1/                  Ponderer 多平台主体，1.20.1
├─ Ponderer-1.2.2-1.21.1neoforge/    旧/单平台 Ponderer 代码
└─ Ponder-mc1.21.1-dev/              Ponder 本体开发源码
```

## 整体工作流

1. 玩家在客户端 Ponderer 中创建或编辑 JSON 思索场景和 `.nbt` 结构。
2. 玩家执行 `/ponderer push`，客户端把场景 JSON、引用结构、同步哈希、上传模式发给服务器。
3. `Ponderer_server` 检查权限、限流、大小、物品/NBT 规则、锁定、所有权、协作者和冲突。
4. 服务端根据审核模式直接保存、进入人工审核队列，或先调用审核 AI。
5. 通过审核后，服务端把场景保存到世界目录的 `ponderer/scripts/`，结构保存到 `ponderer/structures/`，并记录哈希、所有者、统计、备份和订阅通知。
6. 玩家执行 `/ponderer pull` 时，客户端请求服务器，服务器按权限、审核状态、可见性筛选后返回可见场景和结构。
7. 如果客户端本地没有配置 AI API key，`PondererAI_addon` 会把 Ponderer 的 AI 生成请求转交给服务器 AI 代理，由服务器统一持有 API key 和 token 余额。

## `Ponderer_server` 功能

### 基础定位

- Paper 插件，`plugin.yml` 中声明 `api-version: 1.21`。
- 依赖 LuckPerms；启动时找不到 LuckPerms 会禁用插件。
- Java 21 构建。
- 使用 Bukkit Plugin Message 与客户端 mod 通信。
- 第三方库包括 OkHttp、Gson、Netty Buffer，并通过 shadowJar relocate 到插件包内。

### 场景与结构集中存储

服务端把数据写入当前主世界目录下的 `ponderer/`：

- `ponderer/scripts/`：场景 JSON。
- `ponderer/structures/`：结构 `.nbt`。
- 命名空间为 `ponderer` 的资源直接放在根目录；其他命名空间放到子目录。
- 场景和结构 ID 必须是 `namespace:path` 形式。
- 路径有基础安全检查，拒绝 `..`、反斜杠和不符合规则的路径。
- 支持递归收集全部 `.json` 场景和 `.nbt` 结构并返回给客户端。
- 提供场景存在性检查、读取、保存、删除、总数统计。

### 同步与冲突处理

客户端上传时会带 `lastSyncHash` 和上传模式：

- `check`：默认模式，服务端检测哈希冲突。
- `force`：跳过冲突检测，强制覆盖。
- 如果服务器端场景未变化且哈希相同，直接返回 `ok:<hash>`。
- 如果服务器端与客户端上次同步哈希不同，返回 `conflict`。
- 上传成功后服务端重新计算 SHA-256，并写入 `.sync_hashes.json`。

客户端拉取时：

- 必须有 `ponderer.pull` 权限。
- 服务端收集场景和结构。
- 场景会经过审核状态与可见性过滤。
- 拉取到的场景会记录访问时间，用于过期归档。

### 上传权限、限流与规则检查

上传前会检查：

- `ponderer.upload` 权限。
- 每小时上传频率限制，默认 `20` 次/小时，`0` 表示不限。
- 玩家上传总数上限，可来自 LuckPerms meta `ponderer-upload-limit`，否则用 `default_upload_limit`。
- 坐标/区域触发型场景需要 `ponderer.create.region`。
- 场景 JSON 最大字节数 `max_scene_size_bytes`。
- 玩家 push 冷却 `push_cooldown_seconds`。
- 载体物品过滤：
  - `scene_filter.mode: off | whitelist | blacklist`
  - `scene_filter.items`
- NBT 规则：
  - `nbt_restriction.enabled`
  - `nbt_restriction.mode: whitelist | blacklist`
- 场景锁定状态。
- 场景所有者、场景协作者、全局协作者和管理员权限。

### 所有权、协作者与锁定

- 首次创建场景时记录所有者到 `scene_owners.json`。
- 已存在且有所有者的场景，只有所有者、协作者或管理员可编辑。
- 协作数据存在 `scene_collabs.json`：
  - 场景级协作者：指定某个场景可被某玩家编辑。
  - 全局协作者：指定某个 owner 的全部场景可被某玩家编辑。
- 锁定数据存在 `scene_locks.json`。
- 玩家命令：
  - `/ponderer lock <sceneId>`
  - `/ponderer unlock <sceneId>`
- 锁定/解锁要求玩家有 `ponderer.lock`，且是 owner 或管理员。

### 可见性与分组

每个场景支持三种可见性：

- `public`：所有有拉取权限的玩家可见。
- `private`：仅 owner 可见。
- `group`：owner 和指定可见性组成员可见。

数据文件：

- `scene_visibility.json`
- `visibility_groups.json`

玩家命令：

- `/ponderer visibility <sceneId> <public|private|group> [groups...]`

管理员命令：

- `/ponderer-admin group create <name>`
- `/ponderer-admin group delete <name>`
- `/ponderer-admin group addplayer <group> <player>`
- `/ponderer-admin group removeplayer <group> <player>`
- `/ponderer-admin group list <group>`

### 审核系统

配置项：

- `review.mode: auto | ai | manual`

行为：

- `auto`：上传通过检查后直接保存。
- `manual`：上传进入 `review_queue.json`，返回 `pending`，等待管理员审核。
- `ai`：上传先进入审核队列，再调用独立的审核 AI 配置；AI 回复 `yes` 开头则自动提交，否则拒绝。
- 审核队列保存场景 JSON、提交者 UUID、提交时间、状态、拒绝原因和结构字节。

管理员命令：

- `/ponderer-admin review list`
- `/ponderer-admin review approve <id>`
- `/ponderer-admin review reject <id> [reason]`

审核 AI 使用独立配置：

- `review_ai.provider`
- `review_ai.api_base_url`
- `review_ai.api_key`
- `review_ai.model`
- `review_ai.system_prompt`

### 举报与自动隐藏

玩家命令：

- `/ponderer report <sceneId> [reason]`

功能：

- 需要 `ponderer.report`。
- 举报记录保存到 `scene_reports.json`。
- 记录举报者 UUID 和原因。
- 达到阈值后标记为 flagged，并把场景可见性改为 `private`。

管理员命令：

- `/ponderer-admin reports list`
- `/ponderer-admin reports dismiss <id>`

注意：配置里有 `report_threshold`，但当前 `PondererCommand` 中自动隐藏阈值写死为 `3`。

### 订阅与离线通知

玩家命令：

- `/ponderer subscribe <sceneId>`
- `/ponderer unsubscribe <sceneId>`

功能：

- 订阅关系保存到 `subscriptions.json`。
- 场景上传成功后通知订阅者。
- 不在线的订阅者会写入 `pending_notifications.json`。
- 玩家上线时 `PlayerJoinListener` 会取出离线通知并发送。

### 备份、历史与回滚

单场景/结构备份：

- 场景被覆盖前备份到 `ponderer/.backups/<scene_safe_name>/<timestamp>.json`。
- 结构被覆盖前备份到 `ponderer/.backups/structures/<structure_safe_name>/<timestamp>.nbt`。
- 每个场景/结构保留数量由 `backup.max_backups` 控制，默认 `10`，`0` 表示禁用。

管理员命令：

- `/ponderer-admin history <sceneId>`：列出场景历史备份。
- `/ponderer-admin rollback <sceneId> <timestamp>`：回滚到指定备份。

定时全量备份：

- `scheduled_backup.enabled`
- `scheduled_backup.interval_hours`
- 输出到 `ponderer/.backups/full/<timestamp>.zip`。
- 打包整个 `ponderer/`，排除 full backup 输出目录本身。

### 过期归档

配置项：

- `expiry.enabled`
- `expiry.days`

功能：

- 服务端在玩家拉取场景时记录访问时间到 `scene_access.json`。
- 定时扫描超过指定天数未被拉取的场景。
- 过期场景复制到 `ponderer/.archive/<scene_safe_name>_<timestamp>.json`。
- 归档后删除原场景并移除访问记录。

### 服务端 AI API 代理

用途：

- 让服务器统一持有 AI API key。
- 玩家客户端无需配置自己的 API key。
- 服务器通过权限和 token 余额控制 AI 调用。

权限：

- `ponderer.ai.use_server_api`
- 管理员充值需要 `ponderer.admin.topup`

支持 provider：

- `anthropic`
- `openai`，兼容 OpenAI Chat Completions 风格接口。

主要配置：

- `ai.provider`
- `ai.api_base_url`
- `ai.api_key`
- `ai.model`
- `ai.max_tokens`
- `ai.pricing.*`
- `logging.log_ai_calls`

实现细节：

- Anthropic 默认 base URL：`https://api.anthropic.com`
- Anthropic 默认模型：`claude-sonnet-4-6`
- OpenAI 默认 base URL：`https://api.openai.com`
- OpenAI 默认模型：`gpt-4o`
- token 统计使用字符数估算：
  - 英文约 4 字符/token。
  - CJK 约 1.5 字符/token。
- 调用前检查玩家 token 余额是否大于 0。
- 调用完成后扣除估算的输入 + 输出 token。
- 调用日志写入 `activity.log`。
- 玩家余额和调用次数保存到 `player_data.json`。

管理员 AI 额度命令：

- `/ponderer-admin topup <player> <amount>`
- `/ponderer-admin settokens <player> <amount>`
- `/ponderer-admin tokens <player>`
- `/ponderer-admin stats <player>`

### 结构导入/下载

客户端可以请求服务端导入指定结构：

- 通道：`ponderer:download_structure`
- 命令转发入口：`/ponderer download <id>`

服务端逻辑：

- 需要上传权限 `ponderer.upload`。
- 优先从服务端已有 `ponderer/structures/` 查找。
- 找不到时尝试从世界 `generated/<namespace>/structures/<path>.nbt` 查找。
- 导入后写成 `ponderer:<path>`。
- 成功后向客户端发送新的同步响应和结构导入结果。

### 普通玩家命令

`/ponderer` 中部分命令由服务端直接处理，部分会转发给客户端执行。

服务端直接处理：

- `/ponderer report <sceneId> [reason]`
- `/ponderer lock <sceneId>`
- `/ponderer unlock <sceneId>`
- `/ponderer subscribe <sceneId>`
- `/ponderer unsubscribe <sceneId>`
- `/ponderer visibility <sceneId> <public|private|group> [groups...]`

转发给客户端附加包/客户端 Ponderer 执行：

- `/ponderer pull [force|keep_local]`
- `/ponderer push [force] [id]`
- `/ponderer reload`
- `/ponderer new hand`
- `/ponderer new <item>`
- `/ponderer list`
- `/ponderer export`
- `/ponderer import`
- `/ponderer delete <id>`
- `/ponderer delete item <item_id>`
- `/ponderer copy <id> <target_item>`
- `/ponderer download <id>`
- `/ponderer convert to_ponderjs|from_ponderjs all|<id>`
- `/ponderer unregister_pack <pack_name>`

服务端转发前会检查部分权限：

- `pull` 需要 `ponderer.pull`。
- `push/new/delete/copy/download` 需要 `ponderer.upload`。
- `export` 需要 `ponderer.pack.export`。
- `import` 需要 `ponderer.pack.import`。

### 管理员命令

主权限：`ponderer.admin`

已实现子命令：

- `/ponderer-admin topup <player> <amount>`：增加玩家 AI token。
- `/ponderer-admin settokens <player> <amount>`：设置玩家 AI token。
- `/ponderer-admin tokens <player>`：查看玩家 token 和 AI 调用次数。
- `/ponderer-admin setlimit <player> <limit>`：设置玩家上传上限。
- `/ponderer-admin resetcount <player>`：重置玩家上传计数。
- `/ponderer-admin stats <player>`：查看玩家上传数、AI 调用数、token 余额。
- `/ponderer-admin scenes`：查看服务器场景总数。
- `/ponderer-admin grant <player> <permission>`：通过 LuckPerms 授权。
- `/ponderer-admin revoke <player> <permission>`：通过 LuckPerms 撤权。
- `/ponderer-admin reload`：重载服务端配置。
- `/ponderer-admin review <list|approve|reject> ...`：审核队列管理。
- `/ponderer-admin group <create|delete|addplayer|removeplayer|list> ...`：可见性分组管理。
- `/ponderer-admin collab <add-scene|remove-scene|add-global|remove-global> ...`：协作者管理。
- `/ponderer-admin reports <list|dismiss> ...`：举报管理。
- `/ponderer-admin history <sceneId>`：查看备份历史。
- `/ponderer-admin rollback <sceneId> <timestamp>`：回滚历史版本。

### 权限节点

`plugin.yml` 和 `PermissionManager` 中出现的权限：

- `ponderer.pull`：允许拉取服务器场景，默认 true。
- `ponderer.upload`：允许上传/创建/删除/复制/下载结构，默认 op。
- `ponderer.admin`：管理员命令主权限，默认 op。
- `ponderer.create.region`：允许创建坐标/区域触发场景，默认 op。
- `ponderer.blueprint`：允许使用蓝图工具，默认 op。
- `ponderer.pack.import`：允许导入场景包，默认 op。
- `ponderer.pack.export`：允许导出场景包，默认 op。
- `ponderer.ai.use_server_api`：允许使用服务器 AI API，默认 false。
- `ponderer.admin.topup`：允许给玩家充值 token，默认 op。
- `ponderer.report`：允许举报场景，默认 true。
- `ponderer.lock`：允许锁定/解锁自己的场景，默认 op。
- `ponderer.admin.review`：允许审核场景，默认 op。
- `ponderer.admin.groups`：允许管理可见性组，默认 op。
- `ponderer.admin.collab`：允许管理协作者，默认 op。
- `ponderer.admin.reports`：允许管理举报，默认 op。

LuckPerms meta：

- `ponderer-upload-limit`：玩家上传总数上限。代码也支持通过命令写入这个 meta。

### 插件消息协议

客户端到服务端：

- `ponderer:sync_request`：请求同步。
- `ponderer:upload_scene`：上传场景 JSON、结构、模式和 lastSyncHash。
- `ponderer:download_structure`：请求导入指定结构。
- `ponderer:ai_request`：请求服务器 AI 代理。

服务端到客户端：

- `ponderer:sync_response`：返回场景 JSON 文件列表和结构 NBT 文件列表。
- `ponderer:upload_response`：返回上传状态，例如 `ok:<hash>`、`conflict`、`pending`、`error`、`cooldown:<seconds>`。
- `ponderer:download_structure_result`：返回结构导入结果。
- `ponderer:ai_response`：返回 AI 请求结果或错误。
- `ponderer:client_command`：把 `/ponderer ...` 命令转发给客户端执行。

底层编码使用 Minecraft `FriendlyByteBuf` 风格：

- VarInt。
- UTF-8 字符串。
- byte array。

## `PondererAI_addon` 功能

### 基础定位

- Fabric 客户端 mod。
- Minecraft `1.21.1`。
- Java 21。
- 依赖 Fabric API、Fabric Loader 和 Ponderer。
- `environment: client`。
- 本身不替代 Ponderer 编辑器，只给 Ponderer 增加服务器联动能力。

### 网络 payload 注册

启动时注册：

- C2S：`ponderer:ai_request`
- S2C：`ponderer:ai_response`
- S2C：`ponderer:client_command`

收到 AI 响应时：

- 根据 requestId 找到 `PendingAiRequests` 中的回调。
- 如果 error 非空，调用失败回调。
- 否则调用成功回调。

收到客户端命令时：

- 调用 `ClientCommandDispatcher.dispatch(payload.command())`。

### 服务端 AI 代理接入

`AiSceneGeneratorMixin` 注入 Ponderer 本体的 `com.nododiiiii.ponderer.ai.AiSceneGenerator.generate`：

- 如果 Ponderer 本地配置里 `AI_API_KEY` 不为空，保持原来的本地 AI 调用，不接管。
- 如果本地没有 API key，取消原生成逻辑，改为向服务器发送 `AiRequestPayload`。

发送给服务器的内容包括：

- requestId。
- provider，来自 Ponderer 配置 `AI_PROVIDER`。
- systemPrompt：
  - tutorial 模式：要求生成 tutorial-style Ponder scene JSON。
  - 普通模式：要求生成 Ponder scene JSON。
- userContent：
  - 目标物品 ID。
  - 用户自然语言要求。
  - 已有场景 JSON。
  - 结构文件名，转成 `ponderer:<name>`。
  - reference URL 列表。

收到成功结果后：

- 从 AI 回复中提取 JSON：
  - 优先提取 Markdown 代码块。
  - 否则提取第一个 `{` 到最后一个 `}`。
  - 否则使用原文本。
- 写入 Ponderer 场景目录。
- 文件名根据 carrier item 生成，例如 `minecraft:xxx` 写为 `xxx.json`。
- 调用 Ponderer 的 `SceneStore.reloadFromDisk()` 重新加载场景。
- 触发原 Ponderer AI 生成界面的成功回调。

### 服务端命令转发到客户端

服务端 `/ponderer` 命令可以通过 `ponderer:client_command` 下发给客户端。附加包收到后调用 Ponderer 本体的 `PondererClientCommands`。

支持的命令：

- `pull [mode]`：调用 `PondererClientCommands.pull(mode)`。
- `push [force] [id]`：推送单个场景或全部场景。
- `reload`：重新加载本地场景。
- `list`：打开 Ponderer 物品/场景列表。
- `export`：反射打开导出界面。
- `import`：反射打开导入界面。
- `new hand`：用手持物品创建新场景。
- `new <item>`：为指定物品创建新场景。
- `delete <id>`：删除场景。
- `delete item <item>`：删除某物品关联场景。
- `copy <id> <target_item>`：复制场景并绑定到目标物品。
- `download <structure_id>`：请求结构导入。
- `convert to_ponderjs|from_ponderjs [all|id]`：PonderJS 双向转换。
- `unregister_pack <pack_name>`：取消注册场景包。

部分 Ponderer 方法是 private，附加包用反射调用：

- `openExportScreen`
- `openImportScreen`
- `convertToPonderJs`
- `convertFromPonderJs`
- `unregisterPack`

### 本地文件变更提醒

`FileWatcherService` 启动后台 daemon 线程监听本地 Ponderer 场景目录：

- 优先通过反射调用 `com.nododiiiii.ponderer.ponder.SceneStore.getSceneDir()`。
- 失败时回退到 `config/ponderer/scripts`。
- 监听文件创建和修改。
- 2 秒 debounce。
- 玩家在游戏内时弹出系统 toast，提示场景文件已修改，建议执行 `/ponderer push` 上传。

### Ponderer 配置读取

`PondererConfigAccess` 通过反射读取 Ponderer 本体 `Config`：

- `AI_API_KEY`
- `AI_PROVIDER`

这个设计避免附加包直接编译依赖 Ponderer 的配置类 API，但也意味着 Ponderer 本体字段名变化会导致附加包读不到配置。

## Ponderer 本体相关能力概览

这些功能主要在 `Ponderer-1.20.1`、`Ponderer-1.21.1` 中，不是 `PondererAI_addon` 自己实现的，但服务端和附加包都围绕它工作：

- 使用 JSON DSL 描述 Ponder 场景。
- 场景文件位于客户端 `config/ponderer/scripts/`。
- 结构文件位于客户端 `config/ponderer/structures/`。
- 游戏内可视化场景编辑器。
- 默认按键：
  - `V`：打开功能页/编辑入口。
  - `C`：触发 Ponder。
- 支持以手持物品或指定物品创建场景。
- 支持 NBT 过滤，同一物品不同 NBT 可显示不同思索。
- 支持多段 scene。
- 支持 pack 场景前缀，例如 `[PackName] ponderer:example`。
- 支持结构池与 `show_structure`。
- 支持坐标/结构触发场景和提示。
- 支持 PonderJS 双向转换。
- 支持资源包格式的场景包导入/导出、版本记录、自动加载、包内脚本/结构隔离。
- 支持蓝图工具选择区域并保存结构。
- 支持 JEI 集成，给物品、方块、实体等 ID 字段填值。
- 支持本地热重载。
- 支持客户端与服务端 push/pull/download 同步。
- 支持 AI 场景生成、参考 URL、mcmod 页面辅助、结构描述、OpenAI/Anthropic 等 provider。

已出现在 `StepEditorFactory` 的步骤编辑器类型包括：

- `show_structure`
- `idle`
- `text`
- `shared_text`
- `create_entity`
- `create_item_entity`
- `rotate_camera_y`
- `zoom_scene`
- `highlight_section`
- `show_controls`
- `encapsulate_bounds`
- `play_sound`
- `set_block`
- `destroy_block`
- `replace_blocks`
- `hide_section`
- `show_section_and_merge`
- `toggle_redstone_power`
- `rotate_section`
- `move_section`
- `modify_block_entity_nbt`
- `indicate_redstone`
- `indicate_success`
- `clear_entities`
- `clear_item_entities`
- `modify_entities_nbt`
- `modify_item_entities_nbt`

## 构建方式

### 服务端插件

```bash
cd Ponderer_server
gradlew.bat build
```

输出：

```text
Ponderer_server/build/libs/PondererServer-1.0.0.jar
```

运行要求：

- Paper 1.21.1。
- Java 21。
- LuckPerms。
- 如果要使用服务器 AI 代理，需要在 `config.yml` 中配置 provider、API key 和模型。

### 客户端附加包

```bash
cd PondererAI_addon
gradlew.bat build
```

运行要求：

- Minecraft 1.21.1。
- Fabric Loader 0.16.9+。
- Fabric API。
- Ponderer 本体。
- Java 21。

### Ponderer 本体

`Ponderer-1.20.1` 和 `Ponderer-1.21.1` 是多平台 mod 工程，分别对应不同 Minecraft 版本。它们的构建方式按各自子目录 README/Gradle 配置执行。

## 当前半成品注意事项

这个项目明显包含 AI 生成的半成品代码，下面是阅读源码时看到的风险点；这里不做修复，只记录现状：

- `messages.yml` 和部分 Java 注释/字符串存在明显编码乱码，实际游戏内消息可能不可读。
- `PondererCommand` 的举报自动隐藏阈值写死为 `3`，没有使用 `config.yml` 的 `report_threshold`。
- `AiRequestPacket` 中带有 provider 字段，但服务端 `AiProxyHandler` 实际使用的是服务器配置的 `ai.provider`，不是客户端传来的 provider。
- 服务端 `/ponderer` 命令转发依赖客户端安装并启用 `PondererAI_addon`；没有附加包时，服务端下发 `client_command` 后客户端不会执行。
- `ClientCommandDispatcher` 对异常直接忽略，命令转发失败时玩家可能没有错误提示。
- `PondererAI_addon` 通过反射调用 Ponderer 私有方法和配置字段，Ponderer 本体 API/字段名变化会导致功能失效。
- AI 生成结果只做简单 JSON 提取，不校验生成的 JSON 是否符合 Ponderer DSL。
- 服务端 token 计数是估算，不是 provider 返回的真实 token usage。
- 上传审核 AI 的提示词组织较粗糙，且 AI 异常时只保留 pending 队列等待人工审核。
- README 中列出的 Ponderer 本体能力来自旁边 Ponderer 项目源码与已有说明；本仓库当前新增联动能力主要集中在 `Ponderer_server` 与 `PondererAI_addon`。

## 一句话总结

`Ponderer_server` 把 Ponderer 从“单机/本地编辑工具”扩展成“服务器集中托管的多人协作内容系统”，负责权限、审核、同步、备份、可见性和 AI 额度；`PondererAI_addon` 则是客户端桥接层，让 Ponderer 能使用服务器 AI、响应服务器命令，并提醒玩家把本地改动推送到服务器。

## GitHub Actions

This repository includes `.github/workflows/build.yml`.

The workflow runs on every push, pull request, and manual dispatch. It builds:

- `server`: the Paper plugin
- `client`: the Fabric client addon

Successful runs upload downloadable artifacts from:

- `server/build/libs/*.jar`
- `client/build/libs/*.jar`
