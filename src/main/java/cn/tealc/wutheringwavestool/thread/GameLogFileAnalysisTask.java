package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.dao.GameRecordDao;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.game.GameRecord;
import cn.tealc.wutheringwavestool.model.game.GameRecordForLog;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.kuro.kujiequ.model.sign.UserInfo;
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

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-11-13 15:08
 */
public class GameLogFileAnalysisTask extends Task<List<GameTime>>{
    private static final Logger LOG= LoggerFactory.getLogger(GameLogFileAnalysisTask.class);

    private enum Type{
        ROLE_CHANGE(false,"角色下场，立即隐藏"),
        ROLE_DEATH(false,"前台角色死亡进行切人"),
        BATTLE(false,"切换玩家状态: 进入战斗造成伤害"),
        PHANTOM_GET(false,"[技能名称: 初次幻象收服]"),
        PHANTOM_CALL_SKILL(false,"召唤系幻象的出生特效"),
        PHANTOM_TRANSFORM_SKILL(false,"结束技能名称: 变身幻象"),
        PARALYSIS(false,"进入倒地状态"),
        TRANSFER(false,"传送:完成"),
        PARRY_FRONT(true,"结束技能名称: (.+)?极限闪避前闪"),
        PARRY_BACK(true,"结束技能名称: (.+)?极限闪避后闪"),
        PARRY_ATTACK(true,"结束技能名称: (.+)?极限闪避反击"),
        STRENGTH(false,"当前体力数据 [data: "),
        MONTH_CARD(false,"【月卡每日奖励】信息推送"),
        ACCOUNT_LOGIN(false,"SetUserId [playerId:");

        private final boolean regex;
        private final String key;
        Type(boolean regex,String key) {
            this.key = key;
            this.regex = regex;
        }

    }


    @Override
    protected List<GameTime> call() throws Exception {
        Pattern parryFrontPattern = Pattern.compile(Type.PARRY_FRONT.key);
        Pattern parryBackPattern = Pattern.compile(Type.PARRY_BACK.key);
        Pattern parryAttackPattern = Pattern.compile(Type.PARRY_ATTACK.key);

        List<GameRecordForLog> recordList=new ArrayList<>();
        List<GameTime> timeList=new ArrayList<>();
        GameRecordForLog record=new GameRecordForLog();
        GameTime gameTime=new GameTime();
        String roleId=null;
        recordList.add(record);
        File dir = GameResourcesManager.getGameLogDir();
        if (dir != null){
            File[] files = dir.listFiles(file-> file.getName().startsWith("Client"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparingLong(File::lastModified)); //保证按照时间顺序
                Long startTime = null;
                Optional<Long> currentTime = Optional.empty();
                for (File file : files){
                    int currentStrength = 0; //当前体力

                    try (BufferedReader br = new BufferedReader(new FileReader(file.getAbsolutePath()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            //通过时间判断是否跨天
                            currentTime = getTimestamp(line);
                            if (startTime == null) { //第一次,赋值startTime
                                if (currentTime.isPresent())
                                    startTime = currentTime.get();
                            }else{
                                if (currentTime.isPresent()){
                                    boolean sameDay = isSameDay(startTime, currentTime.get());
                                    if (!sameDay){ //不是同一天，跨天时
                                        record.setCloseTime(startTime); //设置旧record结束时间
                                        startTime = currentTime.get(); //更新开始时间

                                        //创建新record
                                        record = new GameRecordForLog();
                                        record.setRoleId(roleId); //设置玩家角色，其他会在切换玩家是设定
                                        record.setCloseTime(currentTime.get());
                                        recordList.add(record);
                                    }else {
                                        record.setCloseTime(currentTime.get());
                                    }
                                }
                            }

                            for (Type value : Type.values()) {
                                if (!value.regex){
                                    if (line.contains(value.key)) {
                                        switch (value) {
                                            case ROLE_CHANGE -> record.setRoleChange(record.getRoleChange() + 1);
                                            case ROLE_DEATH -> record.setRoleDeath(record.getRoleDeath() + 1);
                                            case BATTLE -> record.setBattle(record.getBattle() + 1);
                                            case PHANTOM_GET -> record.setPhantomGet(record.getPhantomGet() + 1);
                                            case PHANTOM_CALL_SKILL -> record.setPhantomCallSkill(record.getPhantomCallSkill() + 1);
                                            case PHANTOM_TRANSFORM_SKILL -> record.setPhantomTransformSkill(record.getPhantomTransformSkill() + 1);
                                            case PARALYSIS -> record.setParalysis(record.getParalysis() + 1);
                                            case TRANSFER -> record.setTransfer(record.getTransfer() + 1);
                                            case STRENGTH -> {
                                                int strength = getStrength(line);
                                                if (strength < currentStrength){
                                                   int usedStrength = currentStrength - strength;
                                                   record.setUsedStrength(record.getUsedStrength() + usedStrength);
                                                }
                                                currentStrength = strength;
                                            }
                                            case MONTH_CARD -> {
                                                int remainDays = getRemainDays(line);
                                                record.setMonthCard(true);
                                                record.setMonthCardRemainDays(remainDays);
                                            }
                                            case ACCOUNT_LOGIN -> {
                                                roleId = getAccountUID(line);
                                                assert currentTime.isPresent();
                                                if (roleId != null) { //游戏账号切换了
                                                    //Long timestamp = getTimestamp(line);
                                                    if (timeList.isEmpty()){ //当是第一个玩家时，不切换
                                                        gameTime.setStartTime(currentTime.get());
                                                        gameTime.setRoleId(roleId);
                                                        record.setRoleId(roleId);
                                                        timeList.add(gameTime);
                                                    }else {//当是第二个玩家及以上时，切换
                                                        gameTime.setEndTime(currentTime.get()); //当前玩家下线时间
                                                        gameTime = new GameTime();//新玩家
                                                        gameTime.setStartTime(currentTime.get());
                                                        gameTime.setRoleId(roleId);
                                                        timeList.add(gameTime);

                                                        //玩家切换也要切换record,代码好乱啊，找个时间优化一下
                                                        record.setCloseTime(currentTime.get()); //设置旧record结束时间
                                                        //创建新record
                                                        record = new GameRecordForLog();

                                                        record.setRoleId(roleId);
                                                        recordList.add(record);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (parryFrontPattern.matcher(line).find()) {
                                record.setParryFront(record.getParryFront() + 1);
                            }else if (parryBackPattern.matcher(line).find()) {
                                record.setParryBack(record.getParryBack() + 1);
                            }
                            //闪避反击与闪避可同时存在
                            if (parryAttackPattern.matcher(line).find()) {
                                record.setParryAttack(record.getParryAttack() + 1);
                            }
                        }
                    } catch (IOException e) {
                        LOG.error(e.getMessage());
                    }
                }

                gameTime.setEndTime(currentTime.orElse(System.currentTimeMillis()));
            }
        }

        saveRecordData(recordList);
        for (GameTime time : timeList) {
            saveTimeData(time);
        }

        return timeList;
    }



    /**
     * @description: 保存数据
     * @param:	records
     * @return  void
     * @date:   2024/11/22
     */
    private void saveRecordData(List<GameRecordForLog> records){
        GameRecordDao dao =new GameRecordDao();
        //LOG.debug("本次游戏共统计 {} 名角色",records.size());
        for (GameRecordForLog record : records) {
            //LOG.debug("开始保存 {} 的游玩数据",record.getRoleId());
            int num=record.getRoleChange() + record.getRoleDeath() + record.getBattle()
                    +record.getPhantomGet()+record.getPhantomCallSkill()+record.getPhantomTransformSkill()
                    +record.getParalysis()+record.getTransfer()+record.getParryFront()
                    +record.getParryBack()+record.getParryAttack()+record.getUsedStrength()+record.getMonthCardRemainDays();
            if (num > 0 || record.isMonthCard()){
                //有数据，保存
                LocalDate localDate = Instant.ofEpochMilli(record.getCloseTime())
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                record.setCreateDate(localDate.format(formatter));
                dao.addOrUpdateRecord(record);
                LOG.info(record.toString());
            }else {
                //LOG.info("日志无可用数据，无需更新");
            }
        }

    }





    private void saveTimeData(GameTime record){
        long endGameTime = record.getEndTime(); //游戏结束时间
        long startGameTimeForPlayer = record.getStartTime();
        LOG.info("玩家{}，开始时间{}，结束时间{}",record.getRoleId(),getDate(startGameTimeForPlayer),getDate(endGameTime));
        long totalGameTime = endGameTime - startGameTimeForPlayer;//总共游玩时间
        //获取游戏开始时间日期
        ZoneId zone = ZoneId.systemDefault();

        Instant startInstant = Instant.ofEpochMilli(startGameTimeForPlayer);
        ZonedDateTime startZdt = startInstant.atZone(zone);
        LocalDateTime startDateTime = startZdt.toLocalDateTime(); //开始时间
        LocalDate startDate = startZdt.toLocalDate();//开始日期

        Instant endInstant = Instant.ofEpochMilli(endGameTime);
        ZonedDateTime endZdt = endInstant.atZone(zone);
        LocalDate endDate = endZdt.toLocalDate(); //介绍日期

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        GameTimeDao dao=new GameTimeDao();

        if (startDate.isBefore(endDate)){ //跨天
            LocalDateTime endOfDay = startDate.plusDays(1).atStartOfDay();
            long millisecondsUntilEndOfDay = ChronoUnit.MILLIS.between(startDateTime, endOfDay);

            //保存昨天游玩时间
            GameTime gameTime1=new GameTime();
            if (record.getRoleId() != null){ //基本上不可能存在空值
                gameTime1.setRoleId(record.getRoleId());
            }
            gameTime1.setGameDate(dateTimeFormatter.format(startDate));
            gameTime1.setStartTime(startGameTimeForPlayer);
            gameTime1.setEndTime(startGameTimeForPlayer + millisecondsUntilEndOfDay);
            gameTime1.setDuration(millisecondsUntilEndOfDay);
            dao.addTime(gameTime1);
            LOG.info("检测到鸣潮已经结束且跨天，保存昨天时间{}",gameTime1);

            //保存今天游玩时间
            GameTime gameTime=new GameTime();
            if (record.getRoleId() != null){ //基本上不可能存在空值
                gameTime.setRoleId(record.getRoleId());
            }
            gameTime.setGameDate(dateTimeFormatter.format(endDate));
            long todayMillis = totalGameTime - millisecondsUntilEndOfDay;
            gameTime.setStartTime(endGameTime-todayMillis);
            gameTime.setEndTime(endGameTime);
            gameTime.setDuration(todayMillis);
            dao.addTime(gameTime);
            LOG.info("检测到鸣潮已经结束且跨天，保存今天时间{}",gameTime);

        }else {
            GameTime gameTime=new GameTime();
            if (record.getRoleId() != null){ //基本上不可能存在空值
                gameTime.setRoleId(record.getRoleId());
            }
            gameTime.setGameDate(dateTimeFormatter.format(endDate));
            gameTime.setStartTime(startGameTimeForPlayer);
            gameTime.setEndTime(endGameTime);
            gameTime.setDuration(totalGameTime);
            dao.addTime(gameTime);
            LOG.info("检测到鸣潮已经结束，保存时间{}",gameTime);
        }
    }


    public String getDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }

    public boolean isSameDay(long timestamp1, long timestamp2) {

        LocalDate date1 = Instant.ofEpochMilli(timestamp1).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate date2 = Instant.ofEpochMilli(timestamp2).atZone(ZoneId.systemDefault()).toLocalDate();
        return date1.isEqual(date2);
    }


    /**
     * @description: 获取体力，两种体力
     * @param:	row
     * @return  int
     * @date:   2024/11/18
     */
    private int getStrength(String row){
       // System.out.println(row);
        String regex = "UPs:(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(row);
        if (matcher.find()){
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
   /*     int i = 0;
        while (matcher.find()) {
            i += Integer.parseInt(matcher.group(1));
        }
        //System.out.println(i);
        return i;*/
    }


    /**
     * @description: 获取月卡剩余天数
     * @param:	row
     * @return  int
     * @date:   2024/11/18
     */
    private int getRemainDays(String row){
        String regex = "remainDays: (\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(row);
        int i = 0;
        while (matcher.find()) {
            i += Integer.parseInt(matcher.group(1));
        }
        return i;
    }


    /**
     * @description: 匹配玩家UID
     * @param:	row
     * @return  java.lang.String
     * @date:   2024/11/22
     */
    private String getAccountUID(String row){
        String regex = "playerId:\\s*(\\d+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(row);

        if (matcher.find()) {
            String playerId = matcher.group(1);
            return playerId;
        }
        return null;
    }


    /**
     * @description: 匹配日志中2024.11.21-23.59.52:748的时间
     * @param:	row
     * @return  java.lang.Long
     * @date:   2024/11/22
     */
    private Optional<Long> getTimestamp(String row){
        String regex = "\\[(\\d{4}\\.\\d{2}\\.\\d{2}-\\d{2}\\.\\d{2}\\.\\d{2}:\\d{3})\\]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(row);
        Long timestamp = null;
        if (matcher.find()) {
            String time = matcher.group(1);
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd-HH.mm.ss:SSS");
            try {
                Date date = format.parse(time);
                // 获取毫秒
                timestamp = date.getTime();
            } catch (ParseException e) {
                LOG.error("游戏日志中时间转换出现问题" + e.getMessage());
            }
        }
        return Optional.ofNullable(timestamp);
    }
}