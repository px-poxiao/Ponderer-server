# 权限

权限默认值来自服务端插件的 `plugin.yml`。如果安装了 LuckPerms，可以用它持久管理权限和上传上限。

## 权限节点

| 权限                           | 默认  | 作用                          |
| ---------------------------- | --- | --------------------------- |
| `ponderer.pull`              | 所有人 | 允许从服务器拉取思索场景                |
| `ponderer.report`            | 所有人 | 允许举报思索场景                    |
| `ponderer.upload`            | OP  | 允许上传、创建、编辑、删除和复制思索场景        |
| `ponderer.lock`              | OP  | 允许锁定或解锁自己的思索场景              |
| `ponderer.create.region`     | OP  | 允许创建区域或坐标触发型思索场景            |
| `ponderer.blueprint`         | OP  | 允许使用蓝图工具                    |
| `ponderer.pack.import`       | OP  | 允许导入思索场景包                   |
| `ponderer.pack.export`       | OP  | 允许导出思索场景包                   |
| `ponderer.ai.use_server_api` | OP  | 允许使用服务器提供的 AI API           |
| `ponderer.admin`             | OP  | 允许使用 `/ponderer-admin` 管理命令 |
| `ponderer.admin.topup`       | OP  | 允许管理玩家服务器 AI token          |
| `ponderer.admin.review`      | OP  | 允许处理审核队列                    |
| `ponderer.admin.groups`      | OP  | 允许管理可见性分组                   |
| `ponderer.admin.collab`      | OP  | 允许管理协作者                     |
| `ponderer.admin.reports`     | OP  | 允许处理举报                      |

## 推荐分组

### 普通玩家

适合只需要浏览、拉取、订阅和举报的玩家。

```text
/lp group default permission set ponderer.pull true
/lp group default permission set ponderer.report true
```

### 创作者

适合可以创建、上传和维护思索场景的玩家。

```text
/lp group creator permission set ponderer.pull true
/lp group creator permission set ponderer.report true
/lp group creator permission set ponderer.upload true
/lp group creator permission set ponderer.lock true
/lp group creator permission set ponderer.pack.import true
/lp group creator permission set ponderer.pack.export true
```

如果需要区域或坐标触发型场景，再加：

```text
/lp group creator permission set ponderer.create.region true
```

### AI 使用者

适合允许使用服务器 AI 代理的玩家。

```text
/lp group ai permission set ponderer.ai.use_server_api true
```

### 审核员

适合只负责审核和举报处理的管理成员。

```text
/lp group reviewer permission set ponderer.admin true
/lp group reviewer permission set ponderer.admin.review true
/lp group reviewer permission set ponderer.admin.reports true
```

### 管理员

适合完整管理 Ponderer-server 的成员。

```text
/lp group ponderer-admin permission set ponderer.admin true
/lp group ponderer-admin permission set ponderer.admin.topup true
/lp group ponderer-admin permission set ponderer.admin.review true
/lp group ponderer-admin permission set ponderer.admin.groups true
/lp group ponderer-admin permission set ponderer.admin.collab true
/lp group ponderer-admin permission set ponderer.admin.reports true
```

## 上传上限

默认上传上限由 `config.yml` 里的 `default_upload_limit` 控制。值为 `0` 或 `-1` 时表示不限制。

安装 LuckPerms 后，可以用玩家元数据覆盖单个玩家的上传上限：

```text
/lp user <玩家> meta set ponderer-upload-limit 50
```

也可以使用插件命令：

```text
/ponderer-admin setlimit <玩家> <上限>
```
