# 配置

第一次启动服务端插件后，会生成 `config.yml` 和 `messages.yml`。`config.yml` 控制功能开关、审核、举报、AI、上传校验和备份；`messages.yml` 控制玩家能看到的提示文本。

## 功能开关

`features` 控制主要功能是否启用。关闭某项功能后，相关命令或客户端消息会返回“功能已关闭”提示。

| 配置                           | 默认     | 作用                  |
| ---------------------------- | ------ | ------------------- |
| `features.sync`              | `true` | 场景同步和拉取             |
| `features.upload`            | `true` | 场景上传、编辑、删除等写入能力     |
| `features.command_relay`     | `true` | 是否允许服务端把命令转发给客户端附加包 |
| `features.structure_import`  | `true` | 是否允许下载和导入结构文件       |
| `features.server_ai`         | `true` | 是否启用服务器 AI 代理       |
| `features.review`            | `true` | 是否启用上传审核            |
| `features.visibility`        | `true` | 是否启用场景可见性           |
| `features.visibility_groups` | `true` | 是否启用分组可见性           |
| `features.collaboration`     | `true` | 是否启用协作者管理           |
| `features.reports`           | `true` | 是否启用举报              |
| `features.subscriptions`     | `true` | 是否启用订阅通知            |
| `features.locks`             | `true` | 是否启用场景锁定            |
| `features.backups`           | `true` | 是否启用场景备份            |
| `features.history`           | `true` | 是否启用历史查看和回滚         |
| `features.permission_admin`  | `true` | 是否启用权限管理命令          |
| `features.player_stats`      | `true` | 是否记录玩家统计            |

## AI 代理

`ai` 控制客户端通过服务器调用 AI 时使用的服务商、模型和 token 统计。

| 配置                                  | 默认          | 说明                                                 |
| ----------------------------------- | ----------- | -------------------------------------------------- |
| `ai.provider`                       | `anthropic` | 可选 `anthropic` 或 `openai`                          |
| `ai.api_base_url`                   | 空           | 留空使用服务商默认地址；DeepSeek 可填 `https://api.deepseek.com` |
| `ai.api_key`                        | 空           | 服务端保存的 API key                                     |
| `ai.model`                          | 空           | 留空使用默认模型                                           |
| `ai.max_tokens`                     | `16384`     | 单次响应 token 上限                                      |
| `ai.http_timeout_seconds`           | `300`       | AI 请求超时时间                                          |
| `ai.fail_if_api_key_missing`        | `true`      | 未配置 key 时是否直接失败                                    |
| `ai.allow_client_provider_override` | `false`     | 是否允许客户端覆盖服务商配置                                     |

价格字段用于估算 token 花费：

| 配置                            | 默认     |
| ----------------------------- | ------ |
| `ai.pricing.anthropic_input`  | `3.0`  |
| `ai.pricing.anthropic_output` | `15.0` |
| `ai.pricing.openai_input`     | `5.0`  |
| `ai.pricing.openai_output`    | `15.0` |

## 上传限制

| 配置                                            | 默认     | 说明                        |
| --------------------------------------------- | ------ | ------------------------- |
| `rate_limit.uploads_per_hour`                 | `20`   | 每小时上传次数，`0` 表示不限制         |
| `default_upload_limit`                        | `0`    | 默认上传总数上限，`0` 或 `-1` 表示不限制 |
| `upload_validation.max_scene_size_bytes`      | `0`    | 场景大小限制，`0` 表示不限制          |
| `upload_validation.push_cooldown_seconds`     | `0`    | 上传冷却，`0` 表示关闭             |
| `upload_validation.require_region_permission` | `true` | 区域或坐标触发场景是否要求额外权限         |
| `upload_validation.check_conflicts`           | `true` | 是否检查客户端和服务端版本冲突           |
| `upload_validation.allow_force_push`          | `true` | 是否允许强制上传                  |

## 物品和 NBT 过滤

| 配置                        | 默认          | 说明                                          |
| ------------------------- | ----------- | ------------------------------------------- |
| `scene_filter.enabled`    | `true`      | 是否启用思索场景物品过滤                                |
| `scene_filter.mode`       | `off`       | 可选 `off`、`whitelist`、`blacklist`            |
| `scene_filter.items`      | `[]`        | 过滤物品列表                                      |
| `nbt_restriction.enabled` | `false`     | 是否启用 NBT 限制                                 |
| `nbt_restriction.mode`    | `whitelist` | `whitelist` 表示必须带 NBT，`blacklist` 表示禁止带 NBT |

## 审核

| 配置                         | 默认        | 说明                                      |
| -------------------------- | --------- | --------------------------------------- |
| `review.mode`              | `auto`    | 可选 `auto`、`ai`、`manual`                 |
| `review.ai_error_fallback` | `pending` | AI 审核失败时可选 `pending`、`reject`、`approve` |

`review_ai` 是单独用于场景审核的 AI 配置。它可以使用和普通 AI 代理不同的服务商、key、模型和提示词。

## 举报

| 配置                  | 默认     | 说明             |
| ------------------- | ------ | -------------- |
| `reports.threshold` | `3`    | 同一场景达到多少次举报后标记 |
| `reports.auto_hide` | `true` | 达到阈值后是否自动隐藏    |

自动隐藏会把场景可见性设为 `private`。管理员可以用 `/ponderer-admin reports list` 查看被标记的场景，用 `/ponderer-admin reports dismiss <sceneId>` 忽略标记。

## 备份和归档

| 配置                                | 默认      | 说明                   |
| --------------------------------- | ------- | -------------------- |
| `backup.max_backups`              | `10`    | 每个场景保留的备份数量，`0` 表示关闭 |
| `scheduled_backup.enabled`        | `false` | 是否启用定时完整备份           |
| `scheduled_backup.interval_hours` | `24`    | 定时完整备份间隔，单位小时        |
| `expiry.enabled`                  | `false` | 是否启用长期未访问场景归档        |
| `expiry.days`                     | `30`    | 超过多少天未访问后归档          |

## 客户端命令允许列表

`client_commands.allowed` 控制哪些命令可以被服务端转发给客户端。默认允许：

```yaml
client_commands:
  allowed:
    - pull
    - push
    - reload
    - new
    - list
    - edit
    - export
    - import
    - delete
    - copy
    - download
    - convert
    - unregister_pack
```
