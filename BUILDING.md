# Left 2 Mine: Survivor Command 源码构建说明

## 环境

- Java 21（本项目使用 JDK 21.0.10 测试）
- Minecraft 1.21.1
- NeoForge 21.1.264
- Gradle wrapper 已包含在根目录

## 放置前置 jar

源码包不包含体积较大的前置模组 jar。构建前把以下文件放入 `leftmine-ext/libs/`：

```text
leftmine-ext/libs/left2mine-1.21.1-2.0.2.jar
leftmine-ext/libs/NeoMTR-300.6.1+1.21.1+neoforge.jar
leftmine-ext/libs/architectury-13.0.11-neoforge.jar
```

- `left2mine` 是必需前置。
- `NeoMTR` / `Architectury` 只用于编译载具联动代码；运行时它们是可选联动。
- 文件名必须与上面一致，`leftmine-ext/build.gradle` 按文件名引用。

## 构建

在源码包根目录执行：

```powershell
.\gradlew.bat -p leftmine-ext build --no-daemon --no-watch-fs
```

Linux/macOS：

```bash
./gradlew -p leftmine-ext build --no-daemon --no-watch-fs
```

产物：

```text
leftmine-ext/build/libs/leftmineext-1.0.0.jar
```

## 依赖说明

2.0.2 的依赖元数据中，`net.neoforged:fancymodloader:loader:4.0.45`、
`earlydisplay:4.0.45`、`modlauncher:11.0.5`、`mergetool:2.0.0`、
`neoform-runtime:1.0.32` 等 POM 在部分镜像缺失。

源码包内已带最小 `tools/maven-local/` 本地仓库；`leftmine-ext/build.gradle`
会自动把 `../tools/maven-local` 加入仓库，并把 NeoForge/Mojang 库重定向到
BMCLAPI/Aliyun 镜像。

如果所在网络需要代理，在 `leftmine-ext/gradle.properties` 临时加入：

```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7892
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7892
```

（端口按实际代理修改。）

## 目录

```text
survivor-command-1.0.0-source/
├─ readme.md                                 模组介绍
├─ gradlew / gradlew.bat / gradle/wrapper/
├─ tools/maven-local/
└─ leftmine-ext/
   ├─ build.gradle
   ├─ gradle.properties
   ├─ settings.gradle
   ├─ README.md
   ├─ libs/
   └─ src/main/java / src/main/resources
```

## 代码结构

```text
com.qiusheng.leftmineext
├─ LeftMineExt
├─ LeftMineCommands
├─ AdminManager
├─ VehicleGuard
├─ MtrVehicleDetector
├─ BotVehicleFollow
├─ LeftMineLocalization
├─ ClientLocalizationEvents
├─ client/ClientLanguage
└─ mixin/
   ├─ ServerPlayerMessageMixin
   ├─ MutableComponentMixin
   └─ client/GuiGraphicsStringMixin
```

## 语言

- 扩展自身消息：`assets/leftmineext/lang/en_us.json` / `zh_cn.json`
- 默认英文；Minecraft 选择 `zh_cn` 时自动切换中文
- Left 2 Mine 硬编码英文只在中文客户端/中文玩家上翻译；英文环境保持原文
- 服务端按每名玩家的客户端语言处理，可混用中英文客户端

## 说明

源码包不包含 `run-client/`、`test-server/`、构建产物、游戏存档或前置模组 jar。