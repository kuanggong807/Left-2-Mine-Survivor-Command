# Left 2 Mine: Survivor Command（求生之路：幸存者指令）

> **Left 2 Mine: Survivor Command**（求生之路：幸存者指挥）为 **Left 2 Mine** 增加管理员、复活、载具保护、人机跟随与中英双语的 NeoForge 扩展。
>

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-62b47a)
![NeoForge](https://img.shields.io/badge/NeoForge-21.1.264-f16436)
![Left 2 Mine](https://img.shields.io/badge/Left%202%20Mine-2.0.2-c0392b)

---

## 简介

Left 2 Mine 是一套把《求生之路》玩法带进 Minecraft 的模组：战役地图、安全屋、特殊感染者、AI 幸存者和枪械战斗。

本扩展解决服务器和单人长期游玩中比较常见的几个问题：

- 管理员需要自由进出对局，但不能被自动加入、安全屋人数和团灭检测误算
- 玩家倒地后需要 OP 手动复活到指定坐标
- 玩家进入 MTR 电梯/火车后，感染者仍然锁在车内玩家身上
- 所有人进电梯后，AI 幸存者不会跟着进去；出来时又可能被落在车里
- Left 2 Mine 有大量 lang 文件覆盖不到的硬编码英文提示

扩展本身是**纯附加逻辑**，不打包、不修改 Left 2 Mine 原文件。

---

## 功能

### 1. 管理员系统

```text
/leftmine admin add <目标选择器>
/leftmine admin remove <目标选择器>
```

- 管理员使用标签 `leftmine_admin`
- 被添加后，未参战时自动保持**创造模式**
- Left 2 Mine 的自动加入、BotRoster、安全屋人数、团灭检测都会排除创造玩家
- 移除管理员后恢复添加前的游戏模式

### 2. 管理员手动加入/退出

```text
/leftmine game join
/leftmine game leave
```

- `join`：仅管理员可用；有进行中的对局时切入冒险模式，并传送到当前 `respawn` 点
- `leave`：仅管理员可用；退出对局并切回创造模式，不计入安全屋人数
- 管理员登录、被添加或每秒兜底检查时都会回到创造模式（未参战状态）

### 3. 坐标复活

```text
/leftmine fuhuo <目标选择器> <x> <y> <z>
```

- 支持 `@a`、`@s`、玩家名等原版目标选择器
- 对每个目标切回冒险模式，并调用 Left 2 Mine 的 `GameSessions.resetPlayer`
- 恢复生命/饥饿/装备，清除异常状态，传送到指定坐标
- 未参战管理员会先被标记为参战，再执行复活

示例：

```text
/leftmine fuhuo @a 0 64 0
/leftmine fuhuo Steve ~ ~ ~
```

### 4. 载具内感染者保护

玩家进入以下载具后，普通/特殊感染者不再攻击该玩家：

- 原版船、矿车等 passenger 载具
- NeoMTR 列车、电梯（安装 NeoMTR 时）

具体行为：

- 感染者原本锁定车内玩家时，会改锁载具外最近的玩家
- 所有玩家都在载具内时，感染者目标清空，不再攻击玩家
- 载具内玩家免疫 Left 2 Mine 感染者的直接伤害、投射物伤害和无实体来源伤害
- `PINNED`、`SMOKED`、`CHARGED`、`JOCKEYED`、`BILE` 等控制效果会被拒绝或清除
- 离开载具后恢复正常可被锁定和攻击

### 5. 人机跟随载具

当同一维度内所有存活玩家进入载具后：

- Left 2 Mine 的 `SurvivorBot` 自动收拢到车厢内
- 按 bot 数量分配不同局部位置，多人/多 bot 不会全部叠在一个玩家身上
- bot 在车厢内保留自己的前后左右小范围移动，不会跳跃
- 电梯：水平锚点固定在首次进入时的车厢中心，只跟随垂直移动
- 火车：跟随车内玩家群体的平均位置，但保留各自局部位置

当所有玩家离开载具后：

- bot 恢复 AI、重力和受伤状态
- 传送到车外玩家身旁
- 如果载具已经开走、bot 没有跟上，系统会直接把 bot 传送回玩家身边，不会把它留在原地

载具保护同样适用于 bot：

- 普通/特殊感染者不会锁定车内 bot
- Left 2 Mine 伤害不会落到车内 bot
- 控制/胆汁效果不会应用到车内 bot

### 6. 中英双语与硬编码汉化

扩展会根据 Minecraft 当前语言自动选择：

- `en_us`：默认英文
- `zh_cn`：中文
- 其他语言：回退英文

扩展自身命令、状态、帮助均使用语言文件：

```text
assets/leftmineext/lang/en_us.json
assets/leftmineext/lang/zh_cn.json
```

同时处理 Left 2 Mine 的 lang 文件覆盖不到的硬编码英文：

- 地图编辑：选择/链接/断开节点、安全屋门、玩家出生点
- 生成数量/预设/自定义生成提示
- 父节点破坏二次确认
- `/left2mine help` 用法
- 特殊感染者控制 HUD（如 “Pinned by a Hunter!”）
- 旧地图导入/检查提示
- 2.0.2 新增的地图浏览器、下载、存档校验错误

语言规则：

- 中文客户端/中文玩家：套用中文词典
- 英文或其他语言：保持英文原文
- 服务端按每名玩家的客户端语言判断，多人服务器可混用中英文客户端
- Left 2 Mine 2.0.2 自带的官方 `zh_cn.json` 优先，扩展不会覆盖官方翻译键

---

## 命令与权限

| 命令                                 | 权限      | 说明                      |
| ---------------------------------- | ------- | ----------------------- |
| `/leftmine`                        | 所有人     | 显示帮助                    |
| `/leftmine help`                   | 所有人     | 显示帮助                    |
| `/leftmine status`                 | 所有人     | 查看管理员/参战/载具/bot 跟随/游戏模式 |
| `/leftmine fuhuo <目标> <x> <y> <z>` | OP 等级 2 | 复活目标到坐标                 |
| `/leftmine admin add <目标>`         | OP 等级 2 | 设置管理员                   |
| `/leftmine admin remove <目标>`      | OP 等级 2 | 移除管理员                   |
| `/leftmine game join`              | 管理员标签   | 手动加入对局                  |
| `/leftmine game leave`             | 管理员标签   | 手动退出对局                  |

`/leftmine status` 示例：

```text
Left 2 Mine 状态：管理员=否，参战=否，载具=是，bot跟随=2，模式=冒险
```

---

## 环境要求

| 项目          | 版本               |
| ----------- | ---------------- |
| Minecraft   | 1.21.1           |
| NeoForge    | 21.1.264         |
| Left 2 Mine | **1.21.1-2.0.2** |
| Java        | 21               |
| Mod id      | `leftmineext`    |

必需前置：

```text
left2mine-1.21.1-2.0.2.jar
```

可选联动前置（用于 MTR 列车/电梯识别）：

```text
NeoMTR-300.6.1+1.21.1+neoforge.jar
architectury-13.0.11-neoforge.jar
```

不装 NeoMTR 时扩展仍可启动，只保护原版船/矿车等载具。

---

## 安装

1. 安装 Minecraft 1.21.1 + NeoForge 21.1.264
2. 把 `left2mine-1.21.1-2.0.2.jar` 放入 `mods/`
3. 把 `leftmineext-1.0.0.jar` 放入 `mods/`
4. 可选：把 NeoMTR 和 Architectury 放入 `mods/`
5. 启动游戏

多人服务器需要把扩展同时安装在服务端和客户端；载具保护、bot 跟随和汉化都由服务端逻辑配合客户端显示。

---

## 兼容性与已知限制

- 扩展针对 **Left 2 Mine 2.0.2** 编译；旧版 2.0.0 不再作为目标版本
- 原版载具（矿车）测试通过
- NeoMTR 列车/电梯的载具代码已编译并接入，但本地开发环境受 Architectury 13.0.11 的 Mixin 崩溃限制，尚未完成真实 MTR 车厢内实机测试
- 尚未在真实 Left 2 Mine 地图房间内完成安全屋人数/开门实测；管理员人数过滤依据前置模组反编译代码中的 `isCreative` 路径实现
- MTR 只移动 `Player`，不移动 `SurvivorBot`，因此 bot 采用“车厢局部位置 + 定期传送”的方式，而不是成为 MTR 的真正乘客
- 如果服务器安装了其他修改玩家游戏模式/载具乘坐状态的模组，可能需要额外适配

遇到问题请附上：

- `latest.log`
- 是否安装 NeoMTR / Architectury
- 单人或多人
- 使用的 Left 2 Mine 地图/载具类型

---

## 源码与构建

源码采用 Gradle + ModDevGradle，目标 NeoForge 21.1.264。

```powershell
.\gradlew.bat -p leftmine-ext build --no-daemon --no-watch-fs
```

产物：

```text
leftmine-ext/build/libs/leftmineext-1.0.0.jar
```

构建前需要把 Left 2 Mine、NeoMTR、Architectury 的 jar 放入 `leftmine-ext/libs/`。

---

## 更新日志

### 1.0.0 — Survivor Command

- 管理员标签与创造模式隔离
- 管理员手动加入/退出对局
- `/leftmine fuhuo` 坐标复活
- 载具内感染者目标/伤害/控制效果保护
- SurvivorBot 跟随进出载具、车厢内局部移动、掉队传送
- 中英双语：默认英文，中文环境自动切换
- Left 2 Mine 硬编码英文汉化（含 2.0.2 地图浏览器/下载/存档校验提示）
- 适配 Left 2 Mine 1.21.1-2.0.2

---

## 致谢

- **Left 2 Mine** — TheCallunxz
- **NeoMTR** — Jonathan Ho & Zbx1425
- **Architectury** — Architectury Team
- 扩展作者 — jamlisbloxe

本扩展与 Left 2 Mine、NeoMTR 官方无隶属关系。

---

## English

**Left 2 Mine: Survivor Command** (求生之路：幸存者指挥) is a NeoForge add-on for Minecraft 1.21.1 / Left 2 Mine 2.0.2.

Features:

- Admin tag (`leftmine_admin`) with creative-mode isolation
- Manual admin join/leave commands
- Coordinate revive command
- Infected targeting/damage/effect protection inside vehicles (vanilla and NeoMTR)
- SurvivorBot follow-in/out of vehicles, local cabin movement, teleport fallback
- English/Chinese localization selected automatically by the Minecraft language, defaulting to English
- Runtime translation for Left 2 Mine hardcoded English outside its lang files

Requirements:

```text
Minecraft 1.21.1
NeoForge 21.1.264
Left 2 Mine 1.21.1-2.0.2
Java 21
```

Optional:

```text
NeoMTR 300.6.1+1.21.1
Architectury 13.0.11
```

Commands:

```text
/leftmine help
/leftmine status
/leftmine fuhuo <targets> <x> <y> <z>
/leftmine admin add <targets>
/leftmine admin remove <targets>
/leftmine game join
/leftmine game leave
```

`fuhuo` and `admin` require permission level 2. `game join/leave` require the `leftmine_admin` tag.

The add-on does not modify or repackage Left 2 Mine.

---

## License

All Rights Reserved.

Left 2 Mine, NeoMTR and Architectury remain the property of their respective authors.
