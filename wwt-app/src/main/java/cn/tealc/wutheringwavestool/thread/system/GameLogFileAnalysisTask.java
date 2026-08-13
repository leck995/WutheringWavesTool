package cn.tealc.wutheringwavestool.thread.system;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.service.GameRecordService;
import cn.tealc.wutheringwavestool.service.GameTimeService;
import cn.tealc.wutheringwavestool.model.game.GameRecordForLog;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 鸣潮游戏日志分析任务
 * 解密并分析 Client*.log 系列文件，提取每日游戏行为记录与各账号游玩时长
 *
 * @author Leck
 * @create 2024-11-13
 */
public class GameLogFileAnalysisTask extends Task<List<GameTime>> {
    private static final Logger LOG = LoggerFactory.getLogger(GameLogFileAnalysisTask.class);

    // ==================== 常量 ====================
    private static final String CLIENT_LOG_PREFIX = "Client";
    private static final SimpleDateFormat LOG_TIME_FORMAT = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss:SSS");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ==================== 预编译正则（避免每行重复创建） ====================
    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]");
    private static final Pattern PARRY_FRONT_PATTERN =
            Pattern.compile("结束技能名称: (.+)?极限闪避前闪");
    private static final Pattern PARRY_BACK_PATTERN =
            Pattern.compile("结束技能名称: (.+)?极限闪避后闪");
    private static final Pattern PARRY_ATTACK_PATTERN =
            Pattern.compile("结束技能名称: (.+)?极限闪避反击");
    private static final Pattern STRENGTH_PATTERN = Pattern.compile("UPs:(\\d+)");
    private static final Pattern MONTH_CARD_PATTERN = Pattern.compile("remainDays: (\\d+)");
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("playerId:\\s*(\\d+)");

    // ==================== 日志事件枚举 ====================
    /**
     * 游戏日志中的关键事件类型，keyword 为特征子串（contains 匹配）
     */
    private enum LogEvent {
        ROLE_CHANGE("角色下场，立即隐藏"),
        ROLE_DEATH("前台角色死亡进行切人"),
        BATTLE("切换玩家状态: 进入战斗造成伤害"),
        PHANTOM_GET("[技能名称: 初次幻象收服]"),
        PHANTOM_CALL_SKILL("召唤系幻象的出生特效"),
        PHANTOM_TRANSFORM_SKILL("结束技能名称: 变身幻象"),
        PARALYSIS("进入倒地状态"),
        TRANSFER("传送:完成"),
        STRENGTH("当前体力数据 [data: "),
        MONTH_CARD("【月卡每日奖励】信息推送"),
        ACCOUNT_LOGIN("SetUserId [playerId:");

        final String keyword;

        LogEvent(String keyword) {
            this.keyword = keyword;
        }
    }

    // ==================== 解密 ====================
    /**
     * XOR 解密，与 CardPoolRequestTask 中算法一致
     * 每个字节 (b & 0x0F) % 2 == 1 → XOR 0xA5，否则 → XOR 0xEF
     */
    private static void decrypt(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            bytes[i] = (byte) (((b & 0x0F) % 2) == 1 ? b ^ 0xA5 : b ^ 0xEF);
        }
    }

    // ==================== 主流程 ====================

    @Override
    protected List<GameTime> call() throws Exception {
        File logDir = GameResourcesManager.getGameLogDir();
        if (logDir == null) {
            LOG.warn("游戏日志目录未设置");
            return Collections.emptyList();
        }

        File[] logFiles = getSortedLogFiles(logDir);
        if (logFiles == null || logFiles.length == 0) {
            LOG.warn("未找到 Client*.log 日志文件");
            return Collections.emptyList();
        }

        List<GameRecordForLog> records = new ArrayList<>();
        List<GameTime> gameTimes = new ArrayList<>();

        analyzeLogFiles(logFiles, records, gameTimes);

        saveRecords(records);
        saveGameTimes(gameTimes);
        return gameTimes;
    }

    // ==================== 文件排序 ====================

    /**
     * 获取按修改时间升序排列的 Client*.log 文件列表
     */
    private File[] getSortedLogFiles(File logDir) {
        File[] files = logDir.listFiles(f -> f.getName().startsWith(CLIENT_LOG_PREFIX));
        if (files != null) {
            Arrays.sort(files, Comparator.comparingLong(File::lastModified));
        }
        return files;
    }

    // ==================== 日志分析核心 ====================

    /**
     * 遍历所有日志文件，解密并逐行分析
     */
    private void analyzeLogFiles(File[] logFiles,
                                 List<GameRecordForLog> records,
                                 List<GameTime> gameTimes) throws Exception {

        GameRecordForLog currentRecord = new GameRecordForLog();
        records.add(currentRecord);

        GameTime currentGameTime = new GameTime();
        Long sessionStartTime = null; // 当前跨天窗口起点，用于检测日期切换
        Optional<Long> lastTimestamp = Optional.empty();
        String currentRoleId = null;

        for (File file : logFiles) {
            int currentStrength = 0;

            try {
                // 一次性读取加密文件，解密后转换为字符串，避免流式解密在 UTF-8 字符边界的潜在问题
                byte[] encrypted = readAllBytes(file);
                decrypt(encrypted);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(
                                new ByteArrayInputStream(encrypted), StandardCharsets.UTF_8));

                String line;
                while ((line = reader.readLine()) != null) {

                    // 提取行首时间戳
                    Optional<Long> lineTime = extractTimestamp(line);
                    if (lineTime.isPresent()) {
                        lastTimestamp = lineTime;

                        if (sessionStartTime == null) {
                            sessionStartTime = lineTime.get();
                        }

                        // 跨天：关闭旧记录，开启新记录
                        if (isDifferentDay(sessionStartTime, lineTime.get())) {
                            currentRecord.setCloseTime(sessionStartTime);
                            sessionStartTime = lineTime.get();

                            currentRecord = new GameRecordForLog();
                            currentRecord.setRoleId(currentRoleId);
                            currentRecord.setCloseTime(lineTime.get());
                            records.add(currentRecord);
                        } else {
                            currentRecord.setCloseTime(lineTime.get());
                        }
                    }

                    // 处理日志行中的事件（ACCOUNT_LOGIN 因副作用多，在循环内直接处理）
                    boolean isAccountLogin = line.contains(LogEvent.ACCOUNT_LOGIN.keyword);

                    if (isAccountLogin) {
                        String roleId = extractString(line, ACCOUNT_PATTERN);
                        if (roleId != null && lastTimestamp.isPresent()) {
                            currentRoleId = roleId;

                            if (gameTimes.isEmpty()) {
                                // 第一个账号
                                currentGameTime.setStartTime(lastTimestamp.get());
                                currentGameTime.setRoleId(roleId);
                                gameTimes.add(currentGameTime);
                            } else {
                                // 账号切换：结束旧 GameTime，创建新 GameTime 和新 GameRecord
                                currentGameTime.setEndTime(lastTimestamp.get());
                                currentGameTime = new GameTime();
                                currentGameTime.setStartTime(lastTimestamp.get());
                                currentGameTime.setRoleId(roleId);
                                gameTimes.add(currentGameTime);

                                currentRecord.setCloseTime(lastTimestamp.get());
                                currentRecord = new GameRecordForLog();
                                currentRecord.setRoleId(roleId);
                                records.add(currentRecord);
                            }
                            currentRecord.setRoleId(roleId);
                        }
                    } else {
                        // 普通事件：计数统计
                        currentStrength = processLine(currentRecord, line, currentStrength);
                    }
                }
            } catch (IOException e) {
                LOG.error("读取日志文件失败: {}", file.getName(), e);
            }
        }

        // 设置最后一个玩家的游戏结束时间
        currentGameTime.setEndTime(lastTimestamp.orElse(System.currentTimeMillis()));
    }

    /**
     * 处理单行日志中的普通事件，更新计数值
     *
     * @return 更新后的当前体力值（仅 STRENGTH 事件会改变）
     */
    private int processLine(GameRecordForLog record, String line, int currentStrength) {

        // 关键字事件（体力消耗需记录变化量，因此单独处理）
        for (LogEvent event : LogEvent.values()) {
            if (!line.contains(event.keyword)) continue;

            switch (event) {
                case ROLE_CHANGE -> record.setRoleChange(record.getRoleChange() + 1);
                case ROLE_DEATH -> record.setRoleDeath(record.getRoleDeath() + 1);
                case BATTLE -> record.setBattle(record.getBattle() + 1);
                case PHANTOM_GET -> record.setPhantomGet(record.getPhantomGet() + 1);
                case PHANTOM_CALL_SKILL -> record.setPhantomCallSkill(record.getPhantomCallSkill() + 1);
                case PHANTOM_TRANSFORM_SKILL -> record.setPhantomTransformSkill(record.getPhantomTransformSkill() + 1);
                case PARALYSIS -> record.setParalysis(record.getParalysis() + 1);
                case TRANSFER -> record.setTransfer(record.getTransfer() + 1);
                case STRENGTH -> {
                    int strength = extractInt(line, STRENGTH_PATTERN);
                    if (strength > 0 && strength < currentStrength) {
                        record.setUsedStrength(record.getUsedStrength() + (currentStrength - strength));
                    }
                    return strength;
                }
                case MONTH_CARD -> {
                    record.setMonthCard(true);
                    record.setMonthCardRemainDays(extractInt(line, MONTH_CARD_PATTERN));
                }
            }
        }

        // 闪避正则需要匹配器检测（前闪/后闪互斥，反击可同时存在）
        if (PARRY_FRONT_PATTERN.matcher(line).find()) {
            record.setParryFront(record.getParryFront() + 1);
        } else if (PARRY_BACK_PATTERN.matcher(line).find()) {
            record.setParryBack(record.getParryBack() + 1);
        }
        if (PARRY_ATTACK_PATTERN.matcher(line).find()) {
            record.setParryAttack(record.getParryAttack() + 1);
        }

        return currentStrength;
    }

    // ==================== 数据保存 ====================

    /**
     * 保存每日游戏行为记录到数据库，跳过无有效数据的记录
     */
    private void saveRecords(List<GameRecordForLog> records) {
        GameRecordService gameRecordService = AppInjector.getInstance(GameRecordService.class);
        for (GameRecordForLog record : records) {
            if (hasValidData(record)) {
                record.setCreateDate(formatDate(record.getCloseTime()));
                gameRecordService.addOrUpdateRecord(record);
                LOG.info("保存游戏记录: {}", record);
            }
        }
    }

    /**
     * 判断游戏记录是否有有效数据
     */
    private boolean hasValidData(GameRecordForLog record) {
        int total = record.getRoleChange() + record.getRoleDeath() + record.getBattle()
                + record.getPhantomGet() + record.getPhantomCallSkill() + record.getPhantomTransformSkill()
                + record.getParalysis() + record.getTransfer() + record.getParryFront()
                + record.getParryBack() + record.getParryAttack() + record.getUsedStrength()
                + record.getMonthCardRemainDays();
        return total > 0 || record.isMonthCard();
    }

    /**
     * 保存各账号游玩时长到数据库，自动处理跨天拆分
     */
    private void saveGameTimes(List<GameTime> gameTimes) {
        GameTimeService gameTimeService = AppInjector.getInstance(GameTimeService.class);
        for (GameTime gameTime : gameTimes) {
            Long start = gameTime.getStartTime();
            Long end = gameTime.getEndTime();
            if (start == null || end == null) continue;

            long duration = end - start;
            LocalDate startDate = toLocalDate(start);
            LocalDate endDate = toLocalDate(end);

            if (startDate.isBefore(endDate)) {
                saveCrossDayGameTime(gameTime, start, end, duration, gameTimeService, startDate);
            } else {
                saveSingleDayGameTime(gameTime, start, duration, gameTimeService, endDate);
            }
        }
    }

    /**
     * 跨天游玩时长拆分为昨天和今天两条记录
     */
    private void saveCrossDayGameTime(GameTime gameTime, long start, long end, long duration,
                                      GameTimeService gameTimeService, LocalDate startDate) {
        LocalDateTime startDateTime = toLocalDateTime(start);
        LocalDateTime endOfDay = startDate.plusDays(1).atStartOfDay();
        long yesterdayMillis = ChronoUnit.MILLIS.between(startDateTime, endOfDay);

        // 昨天
        GameTime yesterday = buildGameTime(gameTime.getRoleId(), startDate,
                start, yesterdayMillis);
        gameTimeService.addTime(yesterday);
        LOG.info("保存跨天游玩时长（昨天）: {}", yesterday);

        // 今天
        long todayMillis = duration - yesterdayMillis;
        GameTime today = buildGameTime(gameTime.getRoleId(), startDate.plusDays(1),
                end - todayMillis, todayMillis);
        gameTimeService.addTime(today);
        LOG.info("保存跨天游玩时长（今天）: {}", today);
    }

    /**
     * 单天游玩时长
     */
    private void saveSingleDayGameTime(GameTime gameTime, long start, long duration,
                                       GameTimeService gameTimeService, LocalDate date) {
        GameTime record = buildGameTime(gameTime.getRoleId(), date, start, duration);
        gameTimeService.addTime(record);
        LOG.info("保存单天游玩时长: {}", record);
    }

    private GameTime buildGameTime(String roleId, LocalDate date,
                                   long startTime, long duration) {
        GameTime gt = new GameTime();
        gt.setRoleId(roleId);
        gt.setGameDate(date.format(DATE_FORMATTER));
        gt.setStartTime(startTime);
        gt.setEndTime(startTime + duration);
        gt.setDuration(duration);
        return gt;
    }

    // ==================== 工具方法 ====================

    /**
     * 从日志行提取时间戳 [yyyy.MM.dd-HH.mm.ss:SSS]
     */
    private Optional<Long> extractTimestamp(String line) {
        Matcher m = TIMESTAMP_PATTERN.matcher(line);
        if (m.find()) {
            try {
                return Optional.of(LOG_TIME_FORMAT.parse(m.group(1)).getTime());
            } catch (ParseException e) {
                LOG.error("解析日志时间失败: {}", m.group(1), e);
            }
        }
        return Optional.empty();
    }

    /**
     * 从日志行提取正则捕获的第一个整数值（累加所有匹配）
     */
    private int extractInt(String line, Pattern pattern) {
        Matcher m = pattern.matcher(line);
        int total = 0;
        while (m.find()) {
            total += Integer.parseInt(m.group(1));
        }
        return total;
    }

    /**
     * 从日志行提取正则捕获的第一个字符串
     */
    private String extractString(String line, Pattern pattern) {
        Matcher m = pattern.matcher(line);
        return m.find() ? m.group(1) : null;
    }

    /**
     * 将文件完整读入字节数组，用于一次性解密
     */
    private byte[] readAllBytes(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             FileChannel channel = raf.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size());
            channel.read(buffer);
            return buffer.array();
        }
    }

    private boolean isDifferentDay(long t1, long t2) {
        return !toLocalDate(t1).isEqual(toLocalDate(t2));
    }

    private LocalDate toLocalDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private String formatDate(long timestamp) {
        return toLocalDate(timestamp).format(DATE_FORMATTER);
    }
}
