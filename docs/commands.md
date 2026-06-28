# 命令

## 玩家命令

占位符中的 `hand` 代表手上的物品对应的思索场景。

| 命令                                                                    | 作用           | 主要权限                   | 需要附加包 |
| --------------------------------------------------------------------- | ------------ | ---------------------- |:-----:|
| `/ponderer report <sceneId\|hand> [原因]`                               | 举报思索场景       | `ponderer.report`      | -     |
| `/ponderer lock <sceneId\|hand>`                                      | 锁定自己的思索场景    | `ponderer.lock`        | -     |
| `/ponderer unlock <sceneId\|hand>`                                    | 解锁自己的思索场景    | `ponderer.lock`        | -     |
| `/ponderer subscribe <sceneId\|hand>`                                 | 订阅场景更新       | `ponderer.pull`        | -     |
| `/ponderer unsubscribe <sceneId\|hand>`                               | 取消订阅场景更新     | `ponderer.pull`        | -     |
| `/ponderer visibility <sceneId> public`                               | 设为公开可见       | 场景所有者或管理员              | -     |
| `/ponderer visibility <sceneId> private`                              | 设为仅自己可见      | 场景所有者或管理员              | -     |
| `/ponderer visibility <sceneId> group <分组...>`                        | 设为指定分组可见     | 场景所有者或管理员              | -     |
| `/ponderer pull [force\|keep_local]`                                  | 从服务器拉取思索场景   | `ponderer.pull`        | ✅     |
| `/ponderer push [force] [sceneId\|hand]`                              | 上传或更新思索场景    | `ponderer.upload`      | ✅     |
| `/ponderer reload`                                                    | 让客户端重新加载相关数据 | -                      | ✅     |
| `/ponderer new [itemId\|hand]`                                        | 为物品创建新的思索场景  | `ponderer.upload`      | ✅     |
| `/ponderer list`                                                      | 列出客户端可见的场景信息 | -                      | ✅     |
| `/ponderer edit [sceneId\|hand]`                                      | 打开编辑入口       | `ponderer.upload`      | ✅     |
| `/ponderer export`                                                    | 导出思索场景包      | `ponderer.pack.export` | ✅     |
| `/ponderer import`                                                    | 导入思索场景包      | `ponderer.pack.import` | ✅     |
| `/ponderer delete [sceneId\|hand]`                                    | 删除场景         | `ponderer.upload`      | ✅     |
| `/ponderer delete item <itemId\|hand>`                                | 删除物品关联       | `ponderer.upload`      | ✅     |
| `/ponderer copy <fromSceneId> <toItemId\|hand>`                       | 复制场景到另一个物品   | `ponderer.upload`      | ✅     |
| `/ponderer download <structureId>`                                    | 下载场景引用的结构文件  | `ponderer.upload`      | ✅     |
| `/ponderer convert <to_ponderjs\|from_ponderjs> <sceneId\|hand\|all>` | 转换场景格式       | -                      | ✅     |
| `/ponderer unregister_pack <pack>`                                    | 注销客户端场景包     | -                      | ✅     |

## 管理员命令

执行这些命令首先需要 `ponderer.admin`。

| 命令                                                   | 作用                 | 额外权限                     |
| ---------------------------------------------------- | ------------------ | ------------------------ |
| `/ponderer-admin topup <玩家> <数量>`                    | 增加玩家服务器 AI token   | `ponderer.admin.topup`   |
| `/ponderer-admin settokens <玩家> <数量>`                | 设置玩家剩余服务器 AI token | `ponderer.admin.topup`   |
| `/ponderer-admin tokens <玩家>`                        | 查看玩家服务器 AI token   | `ponderer.admin`         |
| `/ponderer-admin setlimit <玩家> <上限>`                 | 设置玩家上传上限           | `ponderer.admin`         |
| `/ponderer-admin resetcount <玩家>`                    | 重置玩家上传计数           | `ponderer.admin`         |
| `/ponderer-admin stats <玩家>`                         | 查看玩家上传和 AI 统计      | `ponderer.admin`         |
| `/ponderer-admin scenes`                             | 查看服务器保存的场景总数       | `ponderer.admin`         |
| `/ponderer-admin grant <玩家> <权限>`                    | 给玩家添加权限            | `ponderer.admin`         |
| `/ponderer-admin revoke <玩家> <权限>`                   | 移除玩家权限             | `ponderer.admin`         |
| `/ponderer-admin reload`                             | 重载配置和消息文件          | `ponderer.admin`         |
| `/ponderer-admin review list`                        | 查看待审核场景            | `ponderer.admin.review`  |
| `/ponderer-admin review approve <sceneId>`           | 通过审核               | `ponderer.admin.review`  |
| `/ponderer-admin review reject <sceneId> [原因]`       | 拒绝审核               | `ponderer.admin.review`  |
| `/ponderer-admin group create <分组>`                  | 创建可见性分组            | `ponderer.admin.groups`  |
| `/ponderer-admin group delete <分组>`                  | 删除可见性分组            | `ponderer.admin.groups`  |
| `/ponderer-admin group addplayer <分组> <玩家>`          | 添加分组成员             | `ponderer.admin.groups`  |
| `/ponderer-admin group removeplayer <分组> <玩家>`       | 移除分组成员             | `ponderer.admin.groups`  |
| `/ponderer-admin group list <分组>`                    | 查看分组成员             | `ponderer.admin.groups`  |
| `/ponderer-admin collab add-scene <sceneId> <玩家>`    | 添加单场景协作者           | `ponderer.admin.collab`  |
| `/ponderer-admin collab remove-scene <sceneId> <玩家>` | 移除单场景协作者           | `ponderer.admin.collab`  |
| `/ponderer-admin collab add-global <所有者> <协作者>`      | 添加全局协作者            | `ponderer.admin.collab`  |
| `/ponderer-admin collab remove-global <所有者> <协作者>`   | 移除全局协作者            | `ponderer.admin.collab`  |
| `/ponderer-admin reports list`                       | 查看被举报到阈值的场景        | `ponderer.admin.reports` |
| `/ponderer-admin reports dismiss <sceneId>`          | 忽略举报标记             | `ponderer.admin.reports` |
| `/ponderer-admin history <sceneId>`                  | 查看场景备份历史           | `ponderer.admin`         |
| `/ponderer-admin rollback <sceneId> <timestamp>`     | 回滚场景到指定备份          | `ponderer.admin`         |
