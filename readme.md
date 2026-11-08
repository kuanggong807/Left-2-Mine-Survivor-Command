# Left 2 Mine: Survivor Command 1.3.0（求生之路：幸存者指令）

> **Left 2 Mine: Survivor Command**（求生之路：幸存者指令）是 **Left 2 Mine** 的 NeoForge 扩展，提供管理员、复活、载具保护、人机跟随、任务油桶与加油口系统，并支持中英双语。
>
> **1.3.0** 新增：任务油桶、加油口、长按加油、全局任务计数、加油音效、白色/橙色油桶高亮。

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62b47a)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.264-f16436)
![Left 2 Mine](https://img.shields.io/badge/Left%202%20Mine-2.0.7-c0392b)
![Version](https://img.shields.io/badge/Addon-1.3.0-2f80ed)

---

## 简介

本扩展解决服务器和单人长期游玩中的常见问题，并在 1.3.0 中加入完整的任务加油玩法：

- 管理员自由进出对局，不被自动加入、安全屋人数和团灭检测误算
- 玩家倒地后由 OP 复活到指定坐标
- 玩家进入原版载具或 NeoMTR 电梯/列车后，感染者不会继续锁在车内
- SurvivorBot 跟随玩家进出载具
- Left 2 Mine 硬编码英文的运行时汉化
- **任务油桶**：不可点燃、落地后不可被摧毁、不会误触推开动作
- **加油口**：设置、删除、长按加油、红石块完成、全局任务计数
- **多人规则**：每个加油口同一时间只允许一名玩家使用
- 加油循环音效、每桶完成音效
- 全图白色/橙色任务油桶高亮

扩展是**纯附加逻辑**，不打包、不修改 Left 2 Mine 原文件。

---

## 1.3.0 新增内容

### 1. 任务油桶

物品 ID：

```text
leftmineext:quest_gascan
```

名称：

- 中文：**任务油桶**
- English: **Quest Gas Can**

特性：

- 基于 Left 2 Mine 原生 `CarryableItem` / `GAS_CAN`，保留拾取、携带、投掷行为
- 使用 `gascan_Z` 模型与材质
- 不会被任何来源点燃
- 落地后不会被近战、箭、爆炸、火焰、岩浆或普通伤害摧毁
- 不会触发 Left 2 Mine 的推开动作
- 只出现在 Left 2 Mine 的**用品（combat）**创造标签页
- **每完成一桶油消耗主手 1 个任务油桶**，油桶不是无限使用

### 2. 加油口

设置加油口：

```text
/leftmine gas set <方块坐标> <所需数量> <红石块坐标>
```

删除加油口：

```text
/leftmine gas remove <方块坐标>
```

设置加油时间：

```text
/leftmine gas time <秒>
```

说明：

- 默认加油时间：**5 秒**
- “所需数量”表示该加油口总共需要完成的桶数
- 每完成一桶，消耗主手 1 个任务油桶
- 达到所需数量后，在指定红石块坐标放置红石块
- 加油口上方生成高亮任务油桶标记
- 高亮会持续存在，直到全部加油完成；完成时标记与加油口一起移除
- 管理员可以直接破坏加油口方块来删除；非管理员破坏会被取消
- 每个加油口同一时间只允许一名玩家加油；其他玩家会收到“已有其他玩家正在使用”的提示

### 3. 长按加油

- 手持任务油桶右击加油口开始
- 必须持续对准加油口并按住右键
- 松开右键、移开视线、切换物品或受到攻击时立即中断
- 中断不消耗油桶
- 长按期间玩家被锁定为第三人称，完成后恢复第一人称
- 进度条固定居中显示，满进度严格等于 `progressbar_2.png`
- 屏幕上方显示动态计数：

```text
已加数量 / 所需数量
```

- 设置加油口后，**所有在线玩家**都会看到计数面板
- 每完成一桶，所有玩家计数同步 +1
- 全部完成后计数面板自动消失
- 重新登录会同步当前加油口进度
- 计数面板大小约为进度条宽度的 24%，位置在屏幕上方居中

### 4. 加油音效

- 开始加油播放 `gas_can_fill_pour_01.ogg` 循环音效
- 只有距离加油玩家 **6 格内**的玩家能听见
- 中途停止加油会立即停止音效
- 下次加油从头重新播放
- 每完成一桶立即播放 `survival_teamrec.ogg`
- 完成音效使用独立声音实例，可以和其他音效同时播放，不会顶掉其他音效

### 5. 任务油桶高亮

扩展每 10 tick 实时扫描任务油桶：

- **白色边框**：由 Left 2 Mine 物品生成点生成、没有被玩家动过的任务油桶
- **橙色高亮**：玩家捡起后扔出/丢下的任务油桶
- 物品被玩家捡起并重新扔出后，会实时切换为橙色
- 加油口上方的高亮标记会持续到任务完成

### 6. 中英双语

- `zh_cn`：中文
- `en_us`：英文
- 其他语言回退英文
- 物品名、命令、提示、计数、进度条和加油口信息都使用语言文件
- 语言文件：

```text
assets/leftmineext/lang/zh_cn.json
assets/leftmineext/lang/en_us.json
```

---

## 原有功能

### 管理员系统

```text
/leftmine admin add <目标选择器>
/leftmine admin remove <目标选择器>
```

- 管理员标签：`leftmine_admin`
- 未参战时保持创造模式
- Left 2 Mine 的自动加入、BotRoster、安全屋人数和团灭检测会排除创造玩家
- 移除管理员后恢复之前的游戏模式

### 管理员手动加入/退出

```text
/leftmine game join
/leftmine game leave
```

- `join`：管理员切入冒险模式并传送到 `respawn` 点
- `leave`：退出对局并切回创造模式，不计入安全屋人数

### 坐标复活

```text
/leftmine fuhuo <目标选择器> <x> <y> <z>
```

- 支持 `@a`、`@s`、玩家名等原版选择器
- 恢复生命、饥饿、装备，清除异常状态，传送到指定坐标

### 载具保护与 Bot 跟随

- 原版船、矿车等 passenger 载具
- NeoMTR 列车、电梯（安装 NeoMTR 时）
- 感染者不会锁定车内玩家或 bot
- 车内玩家和 bot 免疫 Left 2 Mine 感染者伤害
- 控制/胆汁效果会被拒绝或清除
- 所有玩家进入载具后，SurvivorBot 跟随进入；离开后恢复 AI 并传送回玩家身边

---

## 命令与权限

| 命令 | 权限 | 说明 |
| --- | --- | --- |
| `/leftmine` | 所有人 | 显示帮助 |
| `/leftmine help` | 所有人 | 显示帮助 |
| `/leftmine status` | 所有人 | 查看管理员/参战/载具/bot/游戏模式 |
| `/leftmine fuhuo <目标> <x> <y> <z>` | OP 等级 2 | 复活目标到坐标 |
| `/leftmine admin add <目标>` | OP 等级 2 | 设置管理员 |
| `/leftmine admin remove <目标>` | OP 等级 2 | 移除管理员 |
| `/leftmine game join` | 管理员标签 | 手动加入对局 |
| `/leftmine game leave` | 管理员标签 | 手动退出对局 |
| `/leftmine gas set <坐标> <数量> <红石块坐标>` | OP 等级 2 | 设置加油口 |
| `/leftmine gas remove <坐标>` | OP 等级 2 | 删除加油口 |
| `/leftmine gas time <秒>` | OP 等级 2 | 设置加油时间 |

---

## 环境要求

| 项目 | 版本 |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264 |
| Left 2 Mine | **1.21.1-2.0.7** |
| Java | 21 |
| Mod id | `leftmineext` |
| 扩展版本 | **1.3.0** |

必需前置：

```text
left2mine-1.21.1-2.0.7.jar
```

可选联动前置：

```text
CustomNPCs-Unofficial-NeoForge-1.21.1.20251230.jar
NeoMTR-300.6.1+1.21.1+neoforge.jar
architectury-13.0.11-neoforge.jar
```

不装 NeoMTR 时扩展仍可启动，只保护原版船/矿车等载具。

---

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.264
2. 把 `left2mine-1.21.1-2.0.7.jar` 放入 `mods/`
3. 把 `leftmineext-1.3.0.jar` 放入 `mods/`
4. 可选：放入 NeoMTR、Architectury 和 CustomNPCs
5. 启动游戏

多人服务器需要同时在服务端和客户端安装扩展；音效、计数 UI 和进度条由客户端显示，加油状态由服务端校验。

---

## 兼容性与已知限制

- 扩展针对 **Left 2 Mine 1.21.1-2.0.7** 编译
- 需要 Java 21
- 加油口一次只允许一名玩家使用
- 音效要求客户端和服务端都安装同一版本的 leftmineext
- 任务油桶高亮基于实体 owner 判定：物品生成点生成且未被玩家持有的为白色；玩家扔出/丢下的为橙色
- 遇到问题请附上 `latest.log`、Left 2 Mine 版本、是否安装 NeoMTR/CustomNPCs

---

## 更新日志

### 1.3.0 — Quest Fuel

- 新增任务油桶 `leftmineext:quest_gascan`
- 任务油桶不可点燃、落地不可摧毁、不会推开
- 新增加油口设置/删除/时间命令
- 新增长按右键加油、第三人称锁定和居中进度条
- 新增屏幕上方动态“已加数量 / 所需数量”
- 新增每桶消耗一个任务油桶
- 新增加油循环音效（6 格范围、停止即停、重新开始）
- 新增每桶完成音效（独立播放，不顶掉其他音效）
- 新增每个加油口单人占用
- 新增加油口持续高亮，全部完成后自动消失
- 新增任务油桶白色/橙色实时高亮
- 适配 Left 2 Mine 1.21.1-2.0.7
- 完善中英双语物品名和界面文本

---

## 源码与构建

源码使用 Gradle + ModDevGradle，目标 NeoForge 21.1.264。

```powershell
.\gradlew.bat -p leftmine-ext build --no-daemon --no-watch-fs
```

构建前需要把 Left 2 Mine、CustomNPCs、NeoMTR、Architectury 的 jar 放进 `leftmine-ext/libs/`。

---

## 致谢

- **Left 2 Mine** — TheCallunxz
- **NeoMTR** — Jonathan Ho & Zbx1425
- **Architectury** — Architectury Team
- 扩展作者 — jamlisbloxe

本扩展与 Left 2 Mine、NeoMTR 官方无隶属关系。

---

# English

**Left 2 Mine: Survivor Command** is a NeoForge add-on for Minecraft 1.21.1 / Left 2 Mine 2.0.7.

Version **1.3.0** adds the quest gas can, gas ports, hold-to-fuel gameplay, a global task counter, fuel sounds, and white/orange quest-can highlights.

## Introduction

The add-on provides:

- Admin tag with creative-mode isolation
- Manual admin join/leave
- Coordinate revive
- Vehicle protection from infected targeting, damage and control effects
- SurvivorBot follow-in/out of vehicles
- Runtime English/Chinese localization
- **Quest gas can**: cannot ignite, cannot be destroyed after dropping, does not trigger shove
- **Gas ports**: set, remove, hold-to-fuel, redstone completion, global counter
- **Single-user ports**: only one player may fuel a given port at a time
- Looping pour sound and per-barrel completion sound
- Map-wide white/orange quest-can highlights

The add-on does not modify or repackage Left 2 Mine.

## Quest gas can

Item ID:

```text
leftmineext:quest_gascan
```

Names:

- Chinese: **任务油桶**
- English: **Quest Gas Can**

Properties:

- Based on Left 2 Mine's native `CarryableItem` / `GAS_CAN`
- Uses the `gascan_Z` model and texture
- Cannot be ignited by any source
- Cannot be destroyed by melee, arrows, explosions, fire, lava or normal damage after dropping
- Does not trigger Left 2 Mine's shove action
- Appears only in the Left 2 Mine **combat / supplies** creative tab
- **Each completed barrel consumes one quest gas can from the main hand**

## Gas ports

Set:

```text
/leftmine gas set <pos> <amount> <redstonePos>
```

Remove:

```text
/leftmine gas remove <pos>
```

Set fill duration:

```text
/leftmine gas time <seconds>
```

- Default fill duration: **5 seconds**
- `amount` is the number of barrels required at this port
- Each completed barrel consumes one quest gas can
- When the required amount is reached, a redstone block is placed at the configured position
- A glowing quest-can marker is created above the port
- The marker stays highlighted until the whole task is complete; then the marker and port are removed
- Admins can break the port block directly to remove it; non-admin breaks are cancelled
- Only one player can fuel a given port at a time; others receive a "port in use" message

## Hold-to-fuel

- Right-click a gas port while holding a quest gas can
- Keep aiming at the port and holding right-click
- Releasing, looking away, switching items or taking damage cancels the fill
- Cancelling does not consume a gas can
- The player is locked to third person while fueling and restored to first person on completion
- The centered progress bar is based on `progressbar_2.png`; 100% is pixel-identical to that image
- A dynamic counter is shown at the top of the screen:

```text
filled / required
```

- After a port is set, **all online players** see the counter
- Each completed barrel updates the counter for everyone
- The counter disappears when the whole task is complete
- Reconnecting resyncs the active port state
- Counter width is about 24% of the progress bar width; it is centered near the top of the screen

## Sounds

- `gas_can_fill_pour_01.ogg` loops while fueling
- Only players within **6 blocks** of the fueling player hear it
- Stopping the fill stops the sound immediately
- The next fill starts from the beginning
- `survival_teamrec.ogg` plays immediately after each completed barrel
- The completion sound uses an independent sound instance and can overlap other sounds; it is not cut off by them

## Quest-can highlights

The server scans quest gas cans every 10 ticks:

- **White outline**: generated by a Left 2 Mine item spawn point and never moved by a player
- **Orange outline**: thrown or dropped by a player
- Picking up and throwing a can switches its outline to orange in real time
- The gas-port marker remains highlighted until the task is complete

## Localization

- `zh_cn`: Chinese
- `en_us`: English
- Other languages fall back to English
- Item names, commands, feedback, counter, progress text and gas-port messages use language files

## Commands and permissions

| Command | Permission | Description |
| --- | --- | --- |
| `/leftmine` | Everyone | Show help |
| `/leftmine help` | Everyone | Show help |
| `/leftmine status` | Everyone | Show admin/session/vehicle/bot/mode status |
| `/leftmine fuhuo <targets> <x> <y> <z>` | OP level 2 | Revive targets at coordinates |
| `/leftmine admin add <targets>` | OP level 2 | Add administrators |
| `/leftmine admin remove <targets>` | OP level 2 | Remove administrators |
| `/leftmine game join` | Admin tag | Join the session manually |
| `/leftmine game leave` | Admin tag | Leave the session manually |
| `/leftmine gas set <pos> <amount> <redstonePos>` | OP level 2 | Create a gas port |
| `/leftmine gas remove <pos>` | OP level 2 | Remove a gas port |
| `/leftmine gas time <seconds>` | OP level 2 | Set fill duration |

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.264 |
| Left 2 Mine | **1.21.1-2.0.7** |
| Java | 21 |
| Mod id | `leftmineext` |
| Addon version | **1.3.0** |

Required:

```text
left2mine-1.21.1-2.0.7.jar
```

Optional:

```text
CustomNPCs-Unofficial-NeoForge-1.21.1.20251230.jar
NeoMTR-300.6.1+1.21.1+neoforge.jar
architectury-13.0.11-neoforge.jar
```

## Installation

1. Install Minecraft 1.21.1 + NeoForge 21.1.264
2. Put `left2mine-1.21.1-2.0.7.jar` into `mods/`
3. Put `leftmineext-1.3.0.jar` into `mods/`
4. Optionally add NeoMTR, Architectury and CustomNPCs
5. Launch the game

For multiplayer, install the add-on on both server and client. Fuel state is server-authoritative; the counter, progress bar and sounds are displayed client-side.

## Changelog

### 1.3.0 — Quest Fuel

- Added quest gas can `leftmineext:quest_gascan`
- Cannot ignite, cannot be destroyed after dropping, no shove action
- Added gas-port set/remove/time commands
- Added hold-to-fuel, third-person lock and centered progress bar
- Added top-screen dynamic `filled / required` counter
- Each completed barrel consumes one quest gas can
- Added looping pour sound (6-block range, stops immediately, restarts from beginning)
- Added per-barrel completion sound (independent, does not cut off other sounds)
- Added single-player reservation per gas port
- Gas-port marker stays highlighted until the task completes
- Added real-time white/orange quest-can highlights
- Adapted to Left 2 Mine 1.21.1-2.0.7
- Completed Chinese/English item and UI localization

## License

All Rights Reserved.

Left 2 Mine, NeoMTR and Architectury remain the property of their respective authors.
