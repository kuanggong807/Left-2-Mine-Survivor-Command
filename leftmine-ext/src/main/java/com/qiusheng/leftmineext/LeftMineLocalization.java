package com.qiusheng.leftmineext;

import com.qiusheng.leftmineext.client.ClientLanguage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Left 2 Mine 的硬编码英文提示翻译。
 *
 * <p>覆盖范围：命令反馈、GUI 标题/按钮、action bar、聊天、tooltip、地图编辑、
 * 安全屋门、特殊感染者控制提示、旧地图导入提示等。只替换已知文本，不改玩家名、
 * 坐标或未知文本。</p>
 */
public final class LeftMineLocalization {
    private static final Map<String, String> EXACT = new HashMap<>();
    private static final Map<String, String> KEYS = new HashMap<>();
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Map<String, String> PRESETS = new HashMap<>();
    private static final Map<String, String> PHRASES = new LinkedHashMap<>();

    static {
        // 地图编辑 / 节点 / 出生点
        EXACT.put("Node selection cleared.", "已清除节点选择。");
        EXACT.put("Node unlinked.", "已断开节点链接。");
        EXACT.put("Select a parent node first.", "请先选择父节点。");
        EXACT.put("Select a parent, then an infected or item spawn node.", "请先选择父节点，再选择感染者或物品生成节点。");
        EXACT.put("Spawn amount: one per player", "生成数量：每名玩家一个");
        EXACT.put("Use a weapon, throwable, healing item or carryable.", "请使用武器、投掷物、治疗物品或可携带物。");
        EXACT.put("Breaking this parent disconnects its child nodes. Break again within 5 seconds to confirm.",
                "破坏此父节点会断开其子节点。5 秒内再次破坏以确认。");
        EXACT.put("Select a saferoom node before placing its doors.", "放置安全门前请先选择安全屋节点。");
        EXACT.put("This room already has both doors, or the floor contains another node.", "该房间已有两扇门，或地板下存在其他节点。");
        EXACT.put("Tip created.", "提示已创建。");
        EXACT.put("Tip removed.", "提示已移除。");
        EXACT.put("Tip coordinates must be finite and text at most 2048 characters.", "提示坐标必须是有限值，文本最多 2048 个字符。");
        EXACT.put("Remove an existing map tip before adding more (maximum 256).", "请先移除已有地图提示，最多只能有 256 个。");

        // 特殊感染者控制提示
        EXACT.put("Ridden by a Jockey!", "被骑乘者骑住！");
        EXACT.put("Caught by a Charger!", "被冲锋者抓住！");
        EXACT.put("Pinned by a Hunter!", "被猎人扑倒！");
        EXACT.put("Caught by a Smoker!", "被烟鬼抓住！");

        // 会话 / 游戏状态
        EXACT.put("Game restarted at the current checkpoint.", "游戏已在当前检查点重新开始。");
        EXACT.put("No game is running.", "当前没有进行中的对局。");
        EXACT.put("No game is running. Use /left2mine start.", "当前没有进行中的对局。请使用 /left2mine start。");
        EXACT.put("Unknown mode. Use easy, normal, hard, expert, survival, or 1-6.",
                "未知模式。可用：easy、normal、hard、expert、survival 或 1-6。");
        EXACT.put("Survival session prepared.", "生存模式已准备。");
        EXACT.put("Game started.", "游戏已开始。");
        EXACT.put("Game stopped. Map reset.", "游戏已停止，地图已重置。");
        EXACT.put("A game is already running. Use /left2mine restart or /left2mine stop.",
                "已有对局正在进行。请使用 /left2mine restart 或 /left2mine stop。");
        EXACT.put("Choose a difficulty above Peaceful: easy, normal, hard or expert.", "请选择高于和平的难度：easy、normal、hard 或 expert。");
        EXACT.put("Safe for now. Checkpoint reached.", "暂时安全。已到达检查点。");
        EXACT.put("A horde warning is already pending.", "已有待处理的尸潮警告。");
        EXACT.put("Horde warning started.", "尸潮警告已开始。");
        EXACT.put("Panic event started or scheduled.", "恐慌事件已开始或已安排。");
        EXACT.put("Panic event stopped.", "恐慌事件已停止。");
        EXACT.put("Infected queued for a hidden spawn.", "感染者已排队等待隐藏生成。");
        EXACT.put("Boss queued for an authored boss spawn.", "Boss 已排队等待作者指定的生成点。");
        EXACT.put("Survival world record cleared.", "生存模式世界纪录已清除。");
        EXACT.put("Mission complete.", "任务完成。");
        EXACT.put("A campaign must be running to use /left2mine win.", "必须正在进行战役才能使用 /left2mine win。");
        EXACT.put("No boss encounter remains before the next safe room.", "下一个安全屋前没有剩余的 Boss 遭遇。");
        EXACT.put("No horde spawn candidates", "没有可用的尸潮生成候选点");
        EXACT.put("Horde spawn weights did not select a node", "尸潮生成权重未选中任何节点");
        EXACT.put("Prepare survival with /left2mine start survival first.", "请先使用 /left2mine start survival 准备生存模式。");
        EXACT.put("Wait for the team to recover before starting the next attempt.", "等待队伍恢复后再开始下一次尝试。");
        EXACT.put("Survival mode requires a running game.", "生存模式需要先开始一局游戏。");
        EXACT.put("Unknown director feature: ", "未知的导演功能：");
        EXACT.put(" director feature is disabled. Use /left2mine director enable ", " 导演功能已禁用。请使用 /left2mine director enable ");
        EXACT.put(" first.", " 后再试。");
        EXACT.put("/left2mine help, start [easy|normal|hard|expert|survival [expert]], restart, stop, status, playercount <1-8>, botteam <random|l4d1|l4d2>, hordemusic [theme|random], win, panicstart [waves] [delay], panicstop, clearsurvival, tipcreate <x> <y> <z> <ticks> <message>, tipremove <x> <y> <z>, director <spawn|enable|disable>",
                "/left2mine help（帮助）、start [easy|normal|hard|expert|survival [expert]]（开始）、restart（从检查点重启）、stop（停止）、status（状态）、playercount <1-8>（目标玩家数）、botteam <random|l4d1|l4d2>（Bot 队伍）、hordemusic [theme|random]（尸潮音乐）、win（胜利）、panicstart [waves] [delay]（开始恐慌）、panicstop（停止恐慌）、clearsurvival（清除生存纪录）、tipcreate <x> <y> <z> <ticks> <message>（创建提示）、tipremove <x> <y> <z>（移除提示）、director <spawn|enable|disable>（导演）");

        EXACT.put("Bot team must be random, L4D1, or L4D2.", "Bot 队伍必须是 random、L4D1 或 L4D2。");
        EXACT.put("Player count must be between 1 and 8.", "玩家数量必须在 1 到 8 之间。");
        EXACT.put("Unknown horde cue: ", "未知的尸潮提示：");
        EXACT.put("Unknown horde music theme: ", "未知的尸潮音乐主题：");
        EXACT.put("A horde warning is already pending.", "已有待处理的尸潮警告。");
        EXACT.put("No game is running. Use /left2mine start.", "当前没有进行中的对局。请使用 /left2mine start。");
        EXACT.put("This room already has both doors, or the floor contains another node.", "该房间已有两扇门，或地板下存在其他节点。");

        // 2.0.2 地图浏览器 / 下载 / 存档校验
        EXACT.put("Left2Mine map browser", "Left 2 Mine 地图浏览器");
        EXACT.put("A map has no releases.", "该地图没有可用版本。");
        EXACT.put("Cached file is too large.", "缓存文件过大。");
        EXACT.put("Invalid compatibility list.", "兼容性列表无效。");
        EXACT.put("Invalid download checksum.", "下载校验和无效。");
        EXACT.put("Invalid map download size.", "地图下载大小无效。");
        EXACT.put("Invalid or repeated map ID.", "地图 ID 无效或重复。");
        EXACT.put("Invalid release version.", "版本号无效。");
        EXACT.put("Invalid save folder.", "存档文件夹无效。");
        EXACT.put("Map addresses must use HTTPS.", "地图地址必须使用 HTTPS。");
        EXACT.put("Repeated map version.", "地图版本重复。");
        EXACT.put("The download must contain one world in the expected folder.", "下载内容必须在预期文件夹中包含一个世界。");
        EXACT.put("The download redirect is missing.", "下载重定向地址缺失。");
        EXACT.put("The download redirected too many times.", "下载重定向次数过多。");
        EXACT.put("The downloaded file is too large.", "下载文件过大。");
        EXACT.put("The expanded map is too large.", "解压后的地图过大。");
        EXACT.put("The map archive is damaged.", "地图压缩包已损坏。");
        EXACT.put("The map catalogue is invalid.", "地图目录无效。");
        EXACT.put("The map catalogue is too large.", "地图目录过大。");
        EXACT.put("The map catalogue or file is not available yet.", "地图目录或文件暂不可用。");
        EXACT.put("The map checksum does not match. Please retry.", "地图校验和不匹配，请重试。");
        EXACT.put("The map contains an unsafe file path.", "地图包含不安全的文件路径。");
        EXACT.put("The map contains duplicate file paths.", "地图包含重复的文件路径。");
        EXACT.put("The map contains too many files.", "地图包含过多文件。");
        EXACT.put("The map download is larger than expected.", "地图下载大小超过预期。");
        EXACT.put("The map download was interrupted. Please retry.", "地图下载中断，请重试。");
        EXACT.put("The map's level.dat is missing its world data.", "地图的 level.dat 缺少世界数据。");
        EXACT.put("The map's session.lock must not be a symbolic link.", "地图的 session.lock 不能是符号链接。");
        EXACT.put("The world icon checksum does not match.", "世界图标校验和不匹配。");
        EXACT.put("The world's icon.png must be a 64 by 64 pixel PNG.", "世界的 icon.png 必须是 64×64 像素的 PNG。");
        EXACT.put("This map catalogue needs a newer version of Left 2 Mine.", "此地图目录需要更新版本的 Left 2 Mine。");
        EXACT.put("Too many versions of a map.", "地图版本数量过多。");
        EXACT.put("Unable to recover older map saves", "无法恢复旧地图存档");
        EXACT.put("Unknown map mode.", "未知的地图模式。");
        EXACT.put("Unsafe map path.", "不安全的地图路径。");

        // 旧地图导入 / 世界检查
        EXACT.put("Legacy map preparation failed. Server not started; original save not upgraded.",
                "旧地图准备失败。服务器未启动，原始存档未被升级。");
        EXACT.put("Legacy map preparation failed; original save not upgraded", "旧地图准备失败；原始存档未被升级");
        EXACT.put("Unable to inspect Left 2 Mine save. Server not started; save not upgraded.",
                "无法检查 Left 2 Mine 存档。服务器未启动，存档未被升级。");
        EXACT.put("Unable to inspect legacy map", "无法检查旧地图");
        EXACT.put("Open this map normally to create its Left 2 Mine import backup before upgrading.",
                "请先正常打开此地图，创建 Left 2 Mine 导入备份后再升级。");
        EXACT.put("Cannot convert this legacy chunk. The original backup is retained.", "无法转换此旧区块。原始备份已保留。");
        EXACT.put("Read-only legacy map inspection (no conversion performed)", "只读旧地图检查（未执行转换）");
        EXACT.put("Ready for automatic conversion of mod data when chunks load.", "区块加载时将自动转换模组数据。");
        EXACT.put("Chunk contents, entities and gameplay compatibility have not been checked.", "区块内容、实体和玩法兼容性尚未检查。");
        EXACT.put("Legacy map backup ready.", "旧地图备份已准备。");

        // 常用翻译键（en_us 中已有键，但 Left 2 Mine 没有 zh_cn）
        KEYS.put("message.left2mine.neardeath", "需要急救包！下一次倒地会死亡！");
        KEYS.put("message.left2mine.helpfail", "击杀正在攻击的感染者，帮助 %s 起身！");
        KEYS.put("message.left2mine.incapacitated", "%s 已倒地");
        KEYS.put("message.left2mine.died", "%s 已死亡");
        KEYS.put("message.left2mine.special_killed", "%s 击杀了 %s");
        KEYS.put("message.left2mine.helpsuccess", "%s 扶起了 %s");
        KEYS.put("message.left2mine.witch_startled", "%s 惊动了女巫！");
        KEYS.put("gui.left2mine.overwhelmed", "幸存者已被击溃。");
        KEYS.put("gui.left2mine.gameover", "游戏结束！");
        KEYS.put("hud.left2mine.reloading", "换弹中...");
        KEYS.put("block.left2mine.saferoomdoor", "安全门");
        KEYS.put("block.left2mine.gamedoorsaferoom", "假安全门");
        KEYS.put("block.left2mine.gamedoor1", "橡木门");
        KEYS.put("block.left2mine.playerspawn", "玩家出生点");
        KEYS.put("block.left2mine.survivalspawn", "生存出生点");
        KEYS.put("block.left2mine.saferoomnode", "安全屋节点");
        KEYS.put("block.left2mine.saferoomdoornode", "安全门节点");
        KEYS.put("block.left2mine.itemspawn", "物品生成点");
        KEYS.put("item.left2mine.nodelinker", "节点链接器");

        NAMES.put("playerspawn", "玩家出生点");
        NAMES.put("survivalspawn", "生存出生点");
        NAMES.put("zombiespawn", "普通感染者生成点");
        NAMES.put("bossspawn", "Boss 生成点");
        NAMES.put("hordespawn", "尸潮生成点");
        NAMES.put("itemspawn", "物品生成点");
        NAMES.put("doorspawn", "门生成点");
        NAMES.put("saferoomnode", "安全屋节点");
        NAMES.put("saferoomdoor", "安全门");
        NAMES.put("saferoomdoornode", "安全门节点");
        NAMES.put("gamedoor1", "橡木门");
        NAMES.put("gamedoorsaferoom", "假安全门");
        NAMES.put("ammopile", "弹药堆");
        NAMES.put("chunkloader", "区块加载器");
        NAMES.put("Player Spawn", "玩家出生点");
        NAMES.put("Survival Spawn", "生存出生点");
        NAMES.put("Saferoom Door", "安全门");
        NAMES.put("Fake Saferoom Door", "假安全门");
        NAMES.put("Saferoom Node", "安全屋节点");
        NAMES.put("Saferoom Door Node", "安全门节点");
        NAMES.put("Oak Door", "橡木门");
        NAMES.put("Node Linker", "节点链接器");

        PRESETS.put("minor healing", "小型治疗");
        PRESETS.put("major healing", "大型治疗");
        PRESETS.put("tier 1 weapons", "一级武器");
        PRESETS.put("tier 2 weapons", "二级武器");
        PRESETS.put("throwables", "投掷物");
        PRESETS.put("special weapons", "特殊武器");
        PRESETS.put("secondary weapons", "副武器");
        PRESETS.put("carryables", "可携带物");
        PRESETS.put("melee weapons", "近战武器");

        // 这些片段会出现在拼接消息中
        PHRASES.put(". Bots fill vacancies; extra players are still allowed.", "。Bot 会填补空位，仍允许额外玩家加入。");
        PHRASES.put(". Changes apply to the next mission.", "。更改将在下一场任务生效。");
        PHRASES.put(" for the next mission.", "，用于下一场任务。");
        PHRASES.put("; restarts: ", "；重新开始次数：");
        PHRASES.put(" Current session: ", "。当前对局：");
        PHRASES.put(" Settings apply to the next campaign or survival start.", "。设置将在下一场战役或生存模式开始时生效。");
        PHRASES.put(" | HP ", " | 生命 ");
        PHRASES.put(" node is missing at ", " 节点缺失于 ");
        PHRASES.put("Saferoom entrance placed.", "安全屋入口已放置。");
        PHRASES.put("Saferoom exit placed.", "安全屋出口已放置。");
        PHRASES.put("Target player count: ", "目标玩家数量：");
        PHRASES.put("Target player count set to ", "目标玩家数量已设为 ");
        PHRASES.put("Prioritised bot team: ", "优先 Bot 队伍：");
        PHRASES.put("Prioritised bot team set to ", "优先 Bot 队伍已设为 ");
        PHRASES.put("Game running. Checkpoint: ", "游戏进行中。检查点：");
        PHRASES.put("Horde music: ", "尸潮音乐：");
        PHRASES.put("Unknown horde music theme. Use ", "未知的尸潮音乐主题。可用：");
        PHRASES.put("Director enabled.", "导演模式已启用。");
        PHRASES.put("Director disabled.", "导演模式已禁用。");
        PHRASES.put(" spawns enabled.", " 生成已启用。");
        PHRASES.put(" spawns disabled.", " 生成已禁用。");
        PHRASES.put("The map's ", "地图的 ");
        PHRASES.put("Unknown director feature: ", "未知的导演功能：");
        PHRASES.put(" director feature is disabled. Use /left2mine director enable ", " 导演功能已禁用。请使用 /left2mine director enable ");
        PHRASES.put(" first.", " 后再试。");
        PHRASES.put("Director ", "导演模式 ");
        PHRASES.put("Saferoom ", "安全屋 ");
        PHRASES.put("No target", "无目标");
        PHRASES.put("No visible live target", "没有可见的存活目标");
        PHRASES.put("Target obscured", "目标被遮挡");
        PHRASES.put("Engaging visible infected", "正在攻击可见感染者");
        PHRASES.put("Holding still for a teammate's medkit", "停下来等待队友急救");
        PHRASES.put("Using adrenaline", "正在使用肾上腺素");
        PHRASES.put("Melee recovery", "近战恢复");
        PHRASES.put("Swinging melee", "正在挥动近战武器");
        PHRASES.put("Outside melee reach", "超出近战范围");
        PHRASES.put("Leaving acid or fire", "正在离开酸液或火焰");
        PHRASES.put("Biled: defending with shove", "被胆汁喷到：用推击防御");
        PHRASES.put("Biled: outside close defence range", "被胆汁喷到：超出近身防御范围");
        PHRASES.put("Biled: struggling to aim", "被胆汁喷到：难以瞄准");
        PHRASES.put("Avoiding a shot near an unprovoked or fleeing Witch", "避免在未激怒或逃跑的女巫附近开枪");
        PHRASES.put("Avoiding Witch with melee", "避免用近战攻击女巫");
        PHRASES.put("Holding near the team; no safe Tank retreat", "留在队伍附近；没有安全的坦克撤退路线");
        PHRASES.put("Evading Tank near the team while firing", "在队伍附近边开火边躲避坦克");
        PHRASES.put("Finishing shove", "完成推击");
        PHRASES.put("Support interrupted by damage", "支援因受伤中断");
        PHRASES.put("Support yielded to danger", "支援因危险让位");
        PHRASES.put("Weapon cooldown or restraint", "武器冷却或被控制");
        PHRASES.put("Waiting for capture ownership", "等待捕获归属");
        PHRASES.put("Following ", "跟随 ");
        PHRASES.put("Healing ", "治疗 ");
        PHRASES.put("Reviving ", "复活 ");
        PHRASES.put("Collecting ", "收集 ");
        PHRASES.put("Freeing ", "解救 ");
        PHRASES.put("Blocked by ", "被阻挡：");
        PHRASES.put("Held by ", "被控制：");
        PHRASES.put("Tap-fire pacing", "点射节奏");
        PHRASES.put("Between bursts", "连发间隔");
        PHRASES.put("Trigger pacing", "扳机节奏");
        PHRASES.put("Reaction delay", "反应延迟");
        PHRASES.put("Profiling without gunfire", "无枪声性能分析");
        PHRASES.put(" encounter has already spawned since the last safe room.", " 遭遇在上一安全屋后已经生成过。");
        PHRASES.put(" is not a boss", " 不是 Boss");
        PHRASES.put("Unknown horde cue: ", "未知的尸潮提示：");
        PHRASES.put("Unknown horde music theme: ", "未知的尸潮音乐主题：");
        PHRASES.put("Invalid catalogue field: ", "无效的目录字段：");
        PHRASES.put("Invalid map folder: ", "无效的地图文件夹：");
        PHRASES.put("The map host returned HTTP ", "地图服务器返回 HTTP ");
        PHRASES.put("Close the game using ", "请关闭正在使用该存档的游戏：");
        PHRASES.put(", then try again.", "，然后重试。");
    }

    private LeftMineLocalization() {
    }

    public static boolean isChinese(String languageCode) {
        return languageCode != null && languageCode.toLowerCase(Locale.ROOT).startsWith("zh");
    }

    /**
     * 客户端 GUI/tooltip：只有中文环境才套用中文硬编码词典。
     */
    public static Component translate(Component component) {
        if (!FMLEnvironment.dist.isClient() || !ClientLanguage.isChinese()) {
            return component;
        }
        return translateChinese(component);
    }

    /**
     * 服务端发给某个玩家的消息：按该玩家客户端语言决定是否汉化。
     */
    public static Component translateForPlayer(Component component, ServerPlayer player) {
        if (player == null || !isChinese(player.getLanguage())) {
            return component;
        }
        return translateChinese(component);
    }

    public static String translateTextClient(String text) {
        if (!FMLEnvironment.dist.isClient() || !ClientLanguage.isChinese()) {
            return text;
        }
        return translateText(text);
    }

    private static Component translateChinese(Component component) {
        if (component == null) {
            return null;
        }

        ComponentContents contents = component.getContents();
        MutableComponent result;
        String literal = null;

        if (contents instanceof PlainTextContents.LiteralContents text) {
            literal = text.text();
        } else if (contents instanceof TranslatableContents translatable) {
            // 2.0.2 自带官方 zh_cn.json；翻译键交给原模组语言文件，避免覆盖官方译名。
            result = Component.translatable(translatable.getKey(), translatable.getArgs());
            result.setStyle(component.getStyle());
            return appendSiblings(result, component);
        }

        if (literal != null) {
            String translated = translateText(literal);
            result = MutableComponent.create(new PlainTextContents.LiteralContents(translated)).setStyle(component.getStyle());
            return appendSiblings(result, component);
        }

        result = component.copy();
        result.getSiblings().clear();
        return appendSiblings(result, component);
    }

    private static MutableComponent appendSiblings(MutableComponent result, Component source) {
        for (Component sibling : source.getSiblings()) {
            result.append(translateChinese(sibling));
        }
        return result;
    }

    private static String format(String template, Object[] args) {
        Object[] display = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            display[i] = args[i] instanceof Component component ? component.getString() : args[i];
        }
        try {
            return String.format(template, display);
        } catch (RuntimeException ignored) {
            return template;
        }
    }

    public static String translateText(String text) {
        String exact = EXACT.get(text);
        if (exact != null) {
            return exact;
        }

        if (text.startsWith("The map's ") && text.contains(" node is missing at ")) {
            int at = text.indexOf(" node is missing at ", "The map's ".length());
            String name = text.substring("The map's ".length(), at);
            String pos = text.substring(at + " node is missing at ".length());
            return "地图的 " + translateName(name) + " 节点缺失于 " + pos;
        }

        for (Map.Entry<String, String> entry : PHRASES.entrySet()) {
            if (text.contains(entry.getKey())) {
                text = text.replace(entry.getKey(), entry.getValue());
            }
        }

        if (text.startsWith("Selected ")) {
            int at = text.indexOf(" at ");
            if (at > 9) {
                String name = text.substring(9, at);
                String pos = text.substring(at + 4);
                return "已选择 " + translateName(name) + "，位置 " + pos;
            }
        }
        if (text.startsWith("Node linked to ")) {
            return "已链接到节点 " + text.substring("Node linked to ".length());
        }
        if (text.startsWith("Spawn amount: ")) {
            return "生成数量：" + translateValue(text.substring("Spawn amount: ".length()));
        }
        if (text.startsWith("Spawn preset: ")) {
            return "生成预设：" + translatePreset(text.substring("Spawn preset: ".length()));
        }
        if (text.startsWith("Custom spawn: ")) {
            return "自定义生成：" + translateName(text.substring("Custom spawn: ".length()));
        }
        if (text.startsWith("Left 2 Mine cannot restart: ")) {
            return "Left 2 Mine 无法重新开始：" + text.substring("Left 2 Mine cannot restart: ".length());
        }

        return text;
    }

    private static String translateValue(String value) {
        return value.equals("one per player") ? "每名玩家一个" : value;
    }

    private static String translatePreset(String preset) {
        return PRESETS.getOrDefault(preset, preset);
    }

    private static String translateName(String name) {
        String translated = NAMES.get(name);
        if (translated != null) {
            return translated;
        }
        for (Map.Entry<String, String> entry : NAMES.entrySet()) {
            if (name.startsWith(entry.getKey() + " (")) {
                return entry.getValue() + name.substring(entry.getKey().length());
            }
        }
        return name;
    }
}
