# Left 2 Mine: Survivor Command（leftmineext）

Minecraft **1.21.1** / NeoForge **21.1.264** / Left 2 Mine **1.21.1-2.0.2** 的管理员、复活、载具保护、bot 跟随与中英双语扩展。

显示名：**Left 2 Mine: Survivor Command**（求生之路：幸存者指挥）。mod id 仍为 `leftmineext`，jar 文件名保持 `leftmineext-1.0.0.jar`。

## 前置

- **必须**：`left2mine-1.21.1-2.0.2.jar`
- **可选联动**：`NeoMTR-300.6.1+1.21.1+neoforge.jar` + `architectury-13.0.11-neoforge.jar`
  - Architectury 13.0.11 是 NeoMTR 的必需前置
  - 安装后启用 MTR 列车/电梯内的载具保护和 bot 跟随
  - 未安装时扩展仍可启动，只保护原版船/矿车等 passenger 载具
- 本模组只包含扩展逻辑，不修改前置模组文件
- 管理员标签：`leftmine_admin`

### 2.0.2 适配

- 扩展已按 `left2mine-1.21.1-2.0.2.jar` 重新编译，并通过客户端/服务端加载
- 2.0.2 自带官方 `zh_cn.json`；扩展已停止覆盖翻译键，物品名/字幕/HUD/配置标签优先使用官方中文
- 新增 2.0.2 地图浏览器、下载、存档校验、地图目录错误的硬编码英文翻译
- 管理员、复活、载具保护、bot 跟随在 2.0.2 上回归通过

## 语言 / Language

扩展根据 Minecraft 当前语言自动选择中英文，**默认英文**：

- `en_us`：扩展命令/状态/帮助全部英文
- `zh_cn`：扩展命令/状态/帮助全部中文
- 其他语言：回退到英文 `en_us`

实现：

- 扩展自身消息使用 `Component.translatable` + `assets/leftmineext/lang/en_us.json` / `zh_cn.json`
- Left 2 Mine 的硬编码英文只在中文客户端/中文玩家上套用中文词典
- 英文环境下不翻译 Left 2 Mine 硬编码文本，保留英文原文
- 服务端按 `ServerPlayer.getLanguage()` 判断每个玩家，多人服务器可同时存在中文/英文客户端

示例：

```text
# English (default)
/leftmine help
Left 2 Mine: Survivor Command
...
Left 2 Mine Status: Admin=No, Playing=No, Vehicle=No, Bots=0, Mode=Adventure

# 中文
/leftmine help
求生之路：幸存者指挥命令
...
Left 2 Mine 状态：管理员=否，参战=否，载具=否，bot跟随=0，模式=冒险
```

## 命令

```text
/leftmine
/leftmine help
/leftmine status
/leftmine fuhuo <目标选择器> <x> <y> <z>
/leftmine admin add <目标选择器>
/leftmine admin remove <目标选择器>
/leftmine game join
/leftmine game leave
```

### 权限

- `fuhuo`、`admin add/remove`：需要 OP 权限等级 2
- `game join/leave`：需要拥有 `leftmine_admin` 标签的玩家
- `status`、`help`：所有玩家可用

### 状态

```text
/leftmine status
```

输出示例：

```text
Left 2 Mine 状态：管理员=否，参战=否，载具=是，bot跟随=2，模式=survival
```

`载具=是` 包含原版 passenger 载具，以及 MTR 的列车/电梯乘坐状态。`bot跟随` 是当前被收拢到载具内的 Left 2 Mine 人机数量。

## 载具保护

### 问题

Left 2 Mine 的普通感染者和特殊感染者会锁定玩家。玩家进入 NeoMTR 列车或电梯后，感染者仍然锁定该玩家，特感还能在载具外使用 Smoker 舌头、Charger 冲撞、Jockey 骑乘、Spitter 酸液等技能攻击车内玩家。

### 修复行为

1. 玩家进入载具后，不再作为感染者的可选目标
2. 已经锁定该玩家的普通/特殊感染者，会改锁最近的**载具外**玩家
3. 如果所有玩家都在载具内，感染者目标清空，停止攻击玩家
4. 玩家在载具内时，Left 2 Mine 感染者的直接伤害、投射物伤害和 `left2mine` 伤害类型会被取消
5. 玩家在载具内时，`PINNED`/`SMOKED`/`CHARGED`/`JOCKEYED`/`BILE` 效果会被拒绝或清除，避免“先被控再上车”
6. 玩家离开载具后恢复正常可被锁定和攻击

### 实现位置

- `VehicleGuard`：载具判定、目标重定向、伤害/效果过滤
- `MtrVehicleDetector`：只在 NeoMTR 已加载时访问 MTR 类
- `LeftMineExt`：
  - `LivingChangeTargetEvent`：感染者尝试锁定时立即改目标
  - `ServerTickEvent.Post`：每 5 tick 清理已存在的错误锁定
  - `LivingIncomingDamageEvent`：载具内伤害免疫
  - `MobEffectEvent.Applicable`：拒绝控制效果

### MTR 乘坐状态判定

MTR 的乘客不是原版 `player.getVehicle()`，而是由 `RailwayDataCoolDownModule` 的 riding cooldown 驱动：

```java
RailwayData data = RailwayData.getInstance(player.serverLevel());
boolean riding = !data.railwayDataCoolDownModule.canRide(player);
```

`canRide=false` 表示玩家正在乘坐 MTR 列车/电梯（或刚下车 2 tick）。原版载具则直接使用 `player.isPassenger()`。

### 重要兼容性说明

本地 NeoForge **21.1.264 开发环境**加载 NeoMTR 300.6.1 + Architectury 13.0.11 时，会在启动阶段触发：

```text
Critical injection failure: LVT in net/minecraft/world/entity/LightningBolt::tick()V
at architectury-common.mixins.json:MixinLightningBolt
```

这是 Architectury 在开发映射下的 Mixin LVT 问题，不是本扩展触发；即使不安装本扩展也会出现。生产整合包若遇到同样崩溃，需要更新/替换 Architectury 或使用未安装 NeoMTR 的客户端。

因此：

- 原版载具分支已在 21.1.264 开发客户端实测通过
- MTR 分支已通过编译验证，但本地开发客户端无法在 Architectury 崩溃前进入游戏，未做 MTR 列车/电梯实机测试

## 机器人跟随载具

### 行为

- 当同一维度内**所有存活玩家**都进入载具（电梯/火车/原版载具）后，Left 2 Mine 的 `SurvivorBot` 会自动收拢：
  - 按 bot 数量分配到车厢内不同局部位置，多人/多 bot 不会全部叠在同一个玩家身上
  - 保存每个 bot 的局部偏移，在车厢内做前后左右的小范围移动，**不跳跃**
  - 冻结 AI、关闭重力、设为无敌，避免乱跑、掉出载具或被感染者攻击
- 电梯：
  - 水平锚点固定在首次进入时的车厢中心，不跟随玩家在车厢内走动
  - 垂直位置跟随电梯/玩家整体上下移动
- 火车：
  - 跟随车内玩家群体的平均位置移动，bot 仍保留自己的局部位置
- 玩家全部离开载具后，bot 自动释放：
  - 恢复 AI/重力/受伤状态
  - 传送到车外玩家身旁
- 载具开走、bot 没有跟上时，释放逻辑会直接把 bot 传送回玩家身旁，不会留在原地
- 如果部分玩家仍在车内，已进入的 bot 会继续在车厢内活动，不会反复进出
- 载具保护同样适用于 bot：
  - 普通/特殊感染者不会锁定载具内 bot
  - Left 2 Mine 伤害不会落到载具内 bot
  - `PINNED`/`SMOKED`/`CHARGED`/`JOCKEYED`/`BILE` 不会应用到载具内 bot

### 实现位置

- `BotVehicleFollow`：
  - 每 2 tick 检查一次玩家载具状态
  - 4 tick 进入/离开防抖，避免 MTR 上下车瞬间抖动
  - 多人/多 bot 的车厢网格分配
  - 电梯水平锚点 + 垂直跟随；火车群体平均位置
  - 用 `SurvivorBot` 的 `noAi`/`noGravity`/`invulnerable` 状态隔离，并保存原状态用于恢复
- `MtrVehicleDetector.bounds`：读取 MTR `Lift.liftWidth/liftDepth`，给电梯一个更窄的真实车厢范围
- `VehicleGuard.isInVehicle(Entity)`：玩家和已进入载具的 bot 统一走目标/伤害/效果过滤

### 为什么用传送而不是让 bot 真的坐进 MTR

NeoMTR 的 `VehicleRidingServer.mountRider` 只接受 `Player`，`SurvivorBot` 不是 `Player`，无法直接成为 MTR 乘客。因此扩展采用“车厢局部位置 + 每 2 tick 更新”的方式，让 bot 在视觉和逻辑上都留在载具内；玩家离开后恢复 AI 并传送到车外玩家身旁。

## 汉化

Left 2 Mine 的 `assets/left2mine/lang/en_us.json` 只覆盖带翻译键的文本；命令反馈、GUI 标题、HUD、地图编辑提示等大量文本是硬编码英文，直接改语言文件无效。

2.0.2 已自带官方 `zh_cn.json`。扩展**不覆盖官方翻译键**，只翻译 lang 文件覆盖不到的硬编码文本，包括 2.0.2 新增的地图浏览器/下载/存档校验错误。

扩展在运行时翻译**所有已识别的可见硬编码文本**：

- 命令反馈与 `/left2mine help` 用法
- GUI 标题、按钮、菜单标签
- 物品栏上方 action bar、聊天栏
- 直接绘制的 HUD 文本（如“Pinned by a Hunter!”）
- 物品 tooltip
- 地图编辑、安全屋门、出生点、节点链接
- 特殊感染者控制提示
- 旧地图导入/检查提示
- Bot 状态中的常见英文原因

### 拦截位置

- `mixin/ServerPlayerMessageMixin`
  - `ServerPlayer.displayClientMessage` → action bar
  - `ServerPlayer.sendSystemMessage` → 聊天栏
- `mixin/MutableComponentMixin`
  - `MutableComponent.create` → 所有 literal Component（按钮、标题、HUD、tooltip、聊天）
- `mixin/client/GuiGraphicsStringMixin`
  - `GuiGraphics.drawString` / `drawCenteredString` → 直接以 String 绘制的 HUD 文本
- `ClientLocalizationEvents`
  - `ItemTooltipEvent` → 物品 tooltip
- `LeftMineLocalization`
  - 硬编码英文、节点名、预设名、常见翻译键的词典

### 已覆盖示例

```text
Selected saferoomdoor at 1, 2, 3
→ 已选择 安全门，位置 1, 2, 3

Node selection cleared.
→ 已清除节点选择。

Select a saferoom node before placing its doors.
→ 放置安全门前请先选择安全屋节点。

Saferoom entrance placed.
→ 安全屋入口已放置。

Spawn preset: minor healing
→ 生成预设：小型治疗

Breaking this parent disconnects its child nodes. Break again within 5 seconds to confirm.
→ 破坏此父节点会断开其子节点。5 秒内再次破坏以确认。

Target player count: 4. Bots fill vacancies; extra players are still allowed.
→ 目标玩家数量：4。Bot 会填补空位，仍允许额外玩家加入。

Pinned by a Hunter!
→ 被猎人扑倒！
```

`/left2mine help` 的完整用法行也已翻译，命令名和参数语法保持原样。

说明：`en_us.json` 里已有的普通翻译键（枪械名、字幕、HUD 标签等）不在“lang 文件以外”的范围内；只额外映射了常见战斗提示和地图方块名。玩家名、坐标和未知文本保持原样，不会误改其他模组聊天。

## 复活

```text
/leftmine fuhuo @a 0 64 0
/leftmine fuhuo Steve ~ ~ ~
```

对每个目标：

1. 切回冒险模式
2. 调用前置模组的 `GameSessions.resetPlayer`
3. 重置幸存者状态、清除异常状态、恢复生命/饥饿/装备
4. 传送到指定坐标

如果目标是未参战管理员，会先标记为参战，再执行复活。

## 管理员

```text
/leftmine admin add Steve
/leftmine admin remove Steve
```

添加管理员时：

- 写入 `leftmine_admin` 标签
- 如果玩家尚未参战，立即切换为创造模式
- 创造模式是 Left 2 Mine 内部所有玩家筛选的统一旁路，因此不会被自动加入对局

移除管理员时：

- 删除标签
- 恢复添加前的游戏模式
- 不再享受创造模式旁路

## 手动加入/退出

```text
/leftmine game join
/leftmine game leave
```

- `join`：仅管理员可用；要求当前有进行中的 Left 2 Mine 对局；切入冒险模式并传送到当前 `respawn` 点
- `leave`：仅管理员可用；取消参战标记并切回创造模式；不会计入安全屋人数、团灭检测、BotRoster

## 为什么用创造模式隔离

反编译 Left 2 Mine 2.0.2 后确认，以下逻辑都显式排除创造模式玩家：

- `GameSessions.resetPlayer`：`if (!player.isCreative())`，自动加入/重置会跳过创造玩家
- `GameSessions.entranceClosed`：安全屋人数筛选 `!p.isCreative() && !p.isSpectator() && p.isAlive()`
- `BotRoster.reconcile`：人类列表 `filter(p -> !p.isCreative())`
- `SurvivorRuntime.checkWipe`：团灭名单 `filter(p -> !p.isCreative())`
- `SurvivorActor.is` / `CombatRuntime`：同样排除创造模式

所以未参战管理员保持创造模式即可被前置模组的所有关键人数统计忽略，而无需修改前置模组。

`LeftMineExt.serverTick` 每秒兜底一次：未参战管理员如果被 `/gamemode` 切回非创造模式，会被自动切回创造，保证安全屋人数检测不会误算。

## 构建

```powershell
.\gradlew.bat -p leftmine-ext build
```

产物：

```text
leftmine-ext/build/libs/leftmineext-1.0.0.jar
```

前置 jar 放在 `leftmine-ext/libs/`，仅作为 `compileOnly`，不会打进扩展 jar。

## 已验证

- NeoForge 21.1.264 客户端加载 `Left 2 Mine 1.21.1-2.0.2` + 扩展
- NeoForge 21.1.264 独立服务端加载 `Left 2 Mine 1.21.1-2.0.2` + 扩展，RCON `/leftmine` 正常
- 2.0.2 官方 `zh_cn` 生效：`/give left2mine:saferoomdoor` 显示官方译名 `[安全屋门]`，未被扩展覆盖
- 2.0.2 下 `/leftmine fuhuo @s 0 100 0` 成功复活并传送
- 未安装 NeoMTR 时扩展正常启动（MTR 为可选联动）
- `/leftmine admin add @s`：`管理员=是，模式=creative`
- `/leftmine admin remove @s`：恢复 `survival`
- `/leftmine game join`（无对局）：正确提示“当前没有进行中的对局”
- `/leftmine game leave`：回到创造模式
- `/leftmine status`：矿车内 `载具=是`，下车 `载具=否`
- 矿车测试：Common Infected 从锁定 `Dev` 变为 `none`，不再攻击车内玩家
- 矿车内 `/damage @s 5 left2mine:maul by <common>`：伤害被取消，生命保持 `20.0`
- 下车后同样伤害：生命降到 `17.67`，恢复正常受伤
- 召唤 2 个 `left2mine:survivor_bot` 后上矿车：日志 `bots entered vehicle: 2 (1 players, cabin 2.20x5.60)`，`bot跟随=2`
- 两个 bot 在车厢内有不同局部位置（`[4.29, 112.59, -8.23]` / `[3.42, 112.59, -8.33]`），不是全体叠在玩家身上
- 载具内对 bot 执行 `/damage ... 5 left2mine:maul by <common>`：`对象免疫指定的伤害类型`，bot 生命保持 `20.0`
- 下矿车后：日志 `bots left vehicle: 2`，`bot跟随=0`；bot 回到玩家附近 `[1.12, 110, -4.45]`，玩家 `[0.42, 109, -6.05]`
- 汉化 Mixin 加载无报错；模拟 `sendSystemMessage` 的 `Selected saferoomdoor at 1, 2, 3` 在中文聊天栏显示为 `已选择 安全门，位置 1, 2, 3`
- 模拟 `displayClientMessage(..., true)` 的 `Node selection cleared.` 在中文 action bar 显示为 `已清除节点选择。`
- 中文环境：`/leftmine help`、`/leftmine status`、`/left2mine playercount` 全部中文
- 英文环境：`/leftmine help`、`/leftmine status` 全部英文；`/left2mine playercount` 保持英文原文
- 英文环境官方物品名显示 `[Saferoom Door]`；中文环境显示 `[安全屋门]`
- `/left2mine help` 中文环境显示中文用法，英文环境保持英文用法
- 施加 `left2mine:hunter_pinned` 后中文 HUD 显示 `被猎人扑倒！`，验证 GuiGraphics 原始 String 汉化

尚未在真实 Left 2 Mine 地图房间内做安全屋人数实测；该部分依据前置模组反编译代码的 `isCreative` 过滤路径实现。MTR 列车/电梯实机测试受上述 Architectury 开发环境崩溃限制。
