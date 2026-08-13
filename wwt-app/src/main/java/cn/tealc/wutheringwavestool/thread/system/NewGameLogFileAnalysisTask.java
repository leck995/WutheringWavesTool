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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NewGameLogFileAnalysisTask extends Task<List<GameTime>> {
    private static final Logger LOG = LoggerFactory.getLogger(NewGameLogFileAnalysisTask.class);
    private static final String CLIENT_LOG_PREFIX = "Client";
    private static final String DATE_FORMAT = "yyyy.MM.dd-HH.mm.ss:SSS";
    private static final String GAME_DATE_FORMAT = "yyyy-MM-dd";

    private enum LogEventType {
        ROLE_CHANGE(false, "角色下场，立即隐藏"),
        ROLE_DEATH(false, "前台角色死亡进行切人"),
        BATTLE(false, "切换玩家状态: 进入战斗造成伤害"),
        PHANTOM_GET(false, "[技能名称: 初次幻象收服]"),
        PHANTOM_CALL_SKILL(false, "召唤系幻象的出生特效"),
        PHANTOM_TRANSFORM_SKILL(false, "结束技能名称: 变身幻象"),
        PARALYSIS(false, "进入倒地状态"),
        TRANSFER(false, "传送:完成"),
        PARRY_FRONT(true, "结束技能名称: (.+)?极限闪避前闪"),
        PARRY_BACK(true, "结束技能名称: (.+)?极限闪避后闪"),
        PARRY_ATTACK(true, "结束技能名称: (.+)?极限闪避反击"),
        STRENGTH(false, "当前体力数据 [data: "),
        MONTH_CARD(false, "【月卡每日奖励】信息推送"),
        ACCOUNT_LOGIN(false, "SetUserId [playerId:");

        private final boolean isRegex;
        private final String logKey;

        LogEventType(boolean isRegex, String logKey) {
            this.isRegex = isRegex;
            this.logKey = logKey;
        }
    }

    @Override
    protected List<GameTime> call() throws Exception {
        List<GameRecordForLog> records = new ArrayList<>();
        List<GameTime> gameTimes = new ArrayList<>();
        GameRecordForLog currentRecord = new GameRecordForLog();
        GameTime currentGameTime = new GameTime();

        File logDirectory = GameResourcesManager.getGameLogDir();
        if (logDirectory != null) {
            File[] logFiles = getSortedLogFiles(logDirectory);
            if (logFiles != null) {
                processLogFiles(logFiles, records, gameTimes, currentRecord, currentGameTime);
            }
        }

        saveRecords(records);
        saveGameTimes(gameTimes);
        return gameTimes;
    }

    private File[] getSortedLogFiles(File logDirectory) {
        File[] logFiles = logDirectory.listFiles(file -> file.getName().startsWith(CLIENT_LOG_PREFIX));
        if (logFiles != null) {
            Arrays.sort(logFiles, Comparator.comparingLong(File::lastModified));
        }
        return logFiles;
    }

    private void processLogFiles(File[] logFiles, List<GameRecordForLog> records, List<GameTime> gameTimes,
                                 GameRecordForLog currentRecord, GameTime currentGameTime) {
        Long startTime = null;
        Optional<Long> currentTime = Optional.empty();

        for (File logFile : logFiles) {
            try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
                String logLine;
                int currentStrength = 0;

                while ((logLine = reader.readLine()) != null) {
                    currentTime = extractTimestamp(logLine);
                    if (startTime == null && currentTime.isPresent()) {
                        startTime = currentTime.get();
                    }

                    if (startTime != null && currentTime.isPresent()) {
                        handleDayChange(records, currentRecord, startTime, currentTime.get());
                    }

                    currentStrength = processLogLine(currentRecord, logLine, currentStrength, currentTime, gameTimes);
                }
            } catch (IOException e) {
                LOG.error("无法读取日志文件: {}", e.getMessage());
            }
        }

        if (currentTime.isPresent()) {
            currentGameTime.setEndTime(currentTime.get());
        }
    }

    /**
     * @description: 处理跨天
     * @param:	records
     * @param:	currentRecord
     * @param:	startTime
     * @param:	currentTime
     * @return  void
     * @date:   2025/2/21
     */
    private void handleDayChange(List<GameRecordForLog> records, GameRecordForLog currentRecord,
                                 Long startTime, Long currentTime) {
        if (!isSameDay(startTime, currentTime)) { //跨天时
            currentRecord.setCloseTime(startTime);
            startTime = currentTime;
            currentRecord = new GameRecordForLog();
            currentRecord.setCloseTime(currentTime);
            records.add(currentRecord);
        } else { //仍是同一天
            currentRecord.setCloseTime(currentTime);
        }
    }

    private int processLogLine(GameRecordForLog currentRecord, String logLine, int currentStrength,
                               Optional<Long> currentTime, List<GameTime> gameTimes) {
        for (LogEventType eventType : LogEventType.values()) {
            if (eventType.isRegex) {
                updateRecordWithRegex(currentRecord, logLine, eventType);
            } else if (logLine.contains(eventType.logKey)) {
                updateRecord(currentRecord, eventType, logLine, currentStrength, currentTime, gameTimes);
            }
        }
        return currentStrength;
    }

    private void updateRecord(GameRecordForLog currentRecord, LogEventType eventType, String logLine,
                              int currentStrength, Optional<Long> currentTime, List<GameTime> gameTimes) {
        switch (eventType) {
            case ROLE_CHANGE -> currentRecord.setRoleChange(currentRecord.getRoleChange() + 1);
            case ROLE_DEATH -> currentRecord.setRoleDeath(currentRecord.getRoleDeath() + 1);
            case BATTLE -> currentRecord.setBattle(currentRecord.getBattle() + 1);
            case PHANTOM_GET -> currentRecord.setPhantomGet(currentRecord.getPhantomGet() + 1);
            case PHANTOM_CALL_SKILL -> currentRecord.setPhantomCallSkill(currentRecord.getPhantomCallSkill() + 1);
            case PHANTOM_TRANSFORM_SKILL -> currentRecord.setPhantomTransformSkill(currentRecord.getPhantomTransformSkill() + 1);
            case PARALYSIS -> currentRecord.setParalysis(currentRecord.getParalysis() + 1);
            case TRANSFER -> currentRecord.setTransfer(currentRecord.getTransfer() + 1);
            case STRENGTH -> {
                int strength = extractStrength(logLine);
                if (strength < currentStrength) {
                    currentRecord.setUsedStrength(currentRecord.getUsedStrength() + (currentStrength - strength));
                }
                currentStrength = strength;
            }
            case MONTH_CARD -> {
                currentRecord.setMonthCard(true);
                currentRecord.setMonthCardRemainDays(extractRemainDays(logLine));
            }
            case ACCOUNT_LOGIN -> handleAccountLogin(currentRecord, logLine, currentTime, gameTimes);
        }
    }

    private void updateRecordWithRegex(GameRecordForLog currentRecord, String logLine, LogEventType eventType) {
        Matcher matcher = Pattern.compile(eventType.logKey).matcher(logLine);
        if (matcher.find()) {
            switch (eventType) {
                case PARRY_FRONT -> currentRecord.setParryFront(currentRecord.getParryFront() + 1);
                case PARRY_BACK -> currentRecord.setParryBack(currentRecord.getParryBack() + 1);
                case PARRY_ATTACK -> currentRecord.setParryAttack(currentRecord.getParryAttack() + 1);
            }
        }
    }

    private void handleAccountLogin(GameRecordForLog currentRecord, String logLine,
                                    Optional<Long> currentTime, List<GameTime> gameTimes) {
        String roleId = extractAccountUID(logLine);
        if (roleId != null && currentTime.isPresent()) {
            if (gameTimes.isEmpty()) {
                currentRecord.setRoleId(roleId);
                GameTime gameTime = new GameTime();
                gameTime.setStartTime(currentTime.get());
                gameTime.setRoleId(roleId);
                gameTimes.add(gameTime);
            } else {
                // Handle player switching logic
            }
        }
    }

    /**
     * @description: 保存所有账号游戏记录
     * @param:	records
     * @return  void
     * @date:   2025/2/21
     */
    private void saveRecords(List<GameRecordForLog> records) {
        GameRecordService gameRecordService = AppInjector.getInstance(GameRecordService.class);
        for (GameRecordForLog record : records) {
            if (hasValidRecordData(record)) {
                record.setCreateDate(formatDate(record.getCloseTime(), GAME_DATE_FORMAT));
                gameRecordService.addOrUpdateRecord(record);
                LOG.info("保存游戏记录: {}", record);
            }
        }
    }

    /**
     * @description: 判断游戏数据是否有效，及数据不为0
     * @param:	record
     * @return  boolean
     * @date:   2025/2/21
     */
    private boolean hasValidRecordData(GameRecordForLog record) {
        int totalEvents = record.getRoleChange() + record.getRoleDeath() + record.getBattle()
                + record.getPhantomGet() + record.getPhantomCallSkill() + record.getPhantomTransformSkill()
                + record.getParalysis() + record.getTransfer() + record.getParryFront()
                + record.getParryBack() + record.getParryAttack() + record.getUsedStrength()
                + record.getMonthCardRemainDays();
        return totalEvents > 0 || record.isMonthCard();
    }

    /**
     * @description: 保存全部账号游玩时长
     * @param:	gameTimes
     * @return  void
     * @date:   2025/2/21
     */
    private void saveGameTimes(List<GameTime> gameTimes) {
        GameTimeService gameTimeService = AppInjector.getInstance(GameTimeService.class);
        for (GameTime gameTime : gameTimes) {
            if (gameTime.getStartTime() != null && gameTime.getEndTime() != null) {
                saveGameTime(gameTime, gameTimeService);
            }
        }
    }

    /**
     * @description: 保存单个账号游玩时长
     * @param:	gameTime
     * @param:	dao
     * @return  void
     * @date:   2025/2/21
     */
    private void saveGameTime(GameTime gameTime, GameTimeService gameTimeService) {
        long startTime = gameTime.getStartTime();
        long endTime = gameTime.getEndTime();
        long duration = endTime - startTime;

        LocalDate startDate = toLocalDate(startTime);
        LocalDate endDate = toLocalDate(endTime);

        if (startDate.isBefore(endDate)) {
saveCrossDayGameTime(gameTime, startTime, endTime, duration, gameTimeService, startDate);
            } else {
                saveSingleDayGameTime(gameTime, startTime, endTime, duration, gameTimeService, endDate);
        }
    }

    /**
     * @description: 保存跨天游玩时长
     * @param:	gameTime
     * @param:	startTime
     * @param:	endTime
     * @param:	duration
     * @param:	dao
     * @param:	startDate
     * @return  void
     * @date:   2025/2/21
     */
    private void saveCrossDayGameTime(GameTime gameTime, long startTime, long endTime, long duration,
                                      GameTimeService gameTimeService, LocalDate startDate) {
        LocalDateTime startDateTime = toLocalDateTime(startTime);
        LocalDateTime endOfDay = startDate.plusDays(1).atStartOfDay();
        long millisUntilEndOfDay = ChronoUnit.MILLIS.between(startDateTime, endOfDay);

        GameTime yesterdayGameTime = createGameTime(gameTime, startTime, millisUntilEndOfDay, startDate);
        gameTimeService.addTime(yesterdayGameTime);
        LOG.info("保存跨天游玩时长 (yesterday): {}", yesterdayGameTime);

        long todayMillis = duration - millisUntilEndOfDay;
        GameTime todayGameTime = createGameTime(gameTime, endTime - todayMillis, todayMillis, startDate.plusDays(1));
        gameTimeService.addTime(todayGameTime);
        LOG.info("保存跨天游玩时长 (today): {}", todayGameTime);
    }

    /**
     * @description: 保存单天游玩时长
     * @param:	gameTime
     * @param:	startTime
     * @param:	endTime
     * @param:	duration
     * @param:	dao
     * @param:	date
     * @return  void
     * @date:   2025/2/21
     */
    private void saveSingleDayGameTime(GameTime gameTime, long startTime, long endTime, long duration,
                                       GameTimeService gameTimeService, LocalDate date) {
        GameTime singleDayGameTime = createGameTime(gameTime, startTime, duration, date);
        gameTimeService.addTime(singleDayGameTime);
        LOG.info("保存单天游玩时长: {}", singleDayGameTime);
    }

    private GameTime createGameTime(GameTime gameTime, long startTime, long duration, LocalDate date) {
        GameTime newGameTime = new GameTime();
        newGameTime.setRoleId(gameTime.getRoleId());
        newGameTime.setGameDate(formatDate(date, GAME_DATE_FORMAT));
        newGameTime.setStartTime(startTime);
        newGameTime.setEndTime(startTime + duration);
        newGameTime.setDuration(duration);
        return newGameTime;
    }

    private String formatDate(long timestamp, String pattern) {
        return Instant.ofEpochMilli(timestamp)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern));
    }

    private String formatDate(LocalDate date, String pattern) {
        return date.format(DateTimeFormatter.ofPattern(pattern));
    }

    private LocalDate toLocalDate(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private LocalDateTime toLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * @description: 判断是否时同一天
     * @param:	timestamp1
     * @param:	timestamp2
     * @return  boolean
     * @date:   2025/2/21
     */
    private boolean isSameDay(long timestamp1, long timestamp2) {
        return toLocalDate(timestamp1).isEqual(toLocalDate(timestamp2));
    }

    /**
     * @description: 获取体力
     * @param:	logLine
     * @return  int
     * @date:   2025/2/21
     */
    private int extractStrength(String logLine) {
        return extractIntFromLog(logLine, "UPs:(\\d+)");
    }

    /**
     * @description: 获取月卡剩余时间
     * @param:	logLine
     * @return  int
     * @date:   2025/2/21
     */
    private int extractRemainDays(String logLine) {
        return extractIntFromLog(logLine, "remainDays: (\\d+)");
    }


    private int extractIntFromLog(String logLine, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(logLine);
        int total = 0;
        while (matcher.find()) {
            total += Integer.parseInt(matcher.group(1));
        }
        return total;
    }


    /**
     * @description: 匹配玩家id
     * @param:	logLine
     * @return  java.lang.String
     * @date:   2025/2/21
     */
    private String extractAccountUID(String logLine) {
        return extractStringFromLog(logLine, "playerId:\\s*(\\d+)");
    }


    /**
     * @description: 匹配提取指定格式的正则表达式
     * @param:	logLine
     * @param:	regex
     * @return  java.lang.String
     * @date:   2025/2/21
     */
    private String extractStringFromLog(String logLine, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(logLine);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * @description: 从日志中匹配并提取时间，转为时间戳
     * @param:	logLine
     * @return  java.util.Optional<java.lang.Long>
     * @date:   2025/2/21
     */
    private Optional<Long> extractTimestamp(String logLine) {
        Matcher matcher = Pattern.compile("\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]").matcher(logLine);
        if (matcher.find()) {
            return parseTimestamp(matcher.group(1));
        }
        return Optional.empty();
    }

    /**
     * @description: 将文本日期yyyy.MM.dd-HH.mm.ss:SSS转换为时间戳
     * @param:	time
     * @return  java.util.Optional<java.lang.Long>
     * @date:   2025/2/21
     */
    private Optional<Long> parseTimestamp(String time) {
        try {
            return Optional.of(new SimpleDateFormat(DATE_FORMAT).parse(time).getTime());
        } catch (ParseException e) {
            LOG.error("Failed to parse timestamp: {}", e.getMessage());
            return Optional.empty();
        }
    }
}