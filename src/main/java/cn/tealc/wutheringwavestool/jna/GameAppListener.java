package cn.tealc.wutheringwavestool.jna;

import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.game.GameRecordForLog;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.thread.GameLogFileAnalysisTask;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.MvvmFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * @program: WutheringWavesTool
 * @description: 有个坑，务必将其设置为全局变量，不知道为什么，不设置为全局变量，钩子无效
 * @author: Leck
 * @create: 2024-07-10 23:29
 */
public class GameAppListener implements WinUser.WinEventProc{
    private static final Logger LOG= LoggerFactory.getLogger(GameAppListener.class);
    private static GameAppListener gameAppListener;
    private WinDef.HWND game;
    private boolean start = false;//标记游戏打开了
    private final User32 user32 = User32.INSTANCE;
    private long startGameTime;


    private GameAppListener(){
    }

    public static GameAppListener getInstance(){
        if (gameAppListener==null){
            gameAppListener=new GameAppListener();
        }

        return gameAppListener;
    }

    @Override
    public void callback(WinNT.HANDLE handle, WinDef.DWORD dword, WinDef.HWND hwnd, WinDef.LONG aLong, WinDef.LONG aLong1, WinDef.DWORD dword1, WinDef.DWORD dword2) {
        char[] buffer = new char[256];
        user32.GetWindowText(hwnd, buffer, buffer.length);
        String title = Native.toString(buffer);
        LOG.debug("当前前台窗口是:{}",title);
        if (title.equals("鸣潮  ") || title.equals("Wuthering Waves  ") || title.equals("鳴潮  ")) {
            // 检测到游戏启动
            if (!start) {
                game = hwnd;
                start = true;
                startGameTime = System.currentTimeMillis();
                LOG.info("检测到鸣潮已经启动");
            }
        }else {
            // 处理其他情况
            if (start) { // 只要游戏启动过，start必为true
                updateMainViewTime();
                boolean isAlive = gameIsAlive();
                if (!isAlive) { //游戏结束
                    onEnd();
                }
            }
        }
    }

    private void updateMainViewTime(){
        long endGameTime = System.currentTimeMillis(); // 游戏结束时间
        long totalGameTime = endGameTime - startGameTime; // 总共游玩时间
        if (totalGameTime > 60 * 1000) { // 过滤小于1分钟的时间
            MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE, totalGameTime);
        }
    }


    private void onEnd() {
        LOG.info("检测到鸣潮已经结束");
        start = false;
        GameLogFileAnalysisTask task = new GameLogFileAnalysisTask();
        task.setOnSucceeded(workerStateEvent -> {
            long startTime = startGameTime;
            List<GameRecordForLog> list = task.getValue();
            saveTime(list);
            exit(startTime);
        });
        Thread.startVirtualThread(task);
    }


    /**
     * @description: 最后执行，判断是否需要自动退出助手
     * @param:
     * @return  void
     * @date:   2024/11/22
     */
    private void exit(long startTime){
        //判断是否开启自动退出，退出即结束程序
        if (Config.setting.isExitWhenGameOver()){
            long endGameTime = System.currentTimeMillis();
            long totalGameTime = endGameTime - startTime;//总共游玩时间
            if (totalGameTime > 60000){ //当游戏时长大于1分钟才自动关闭，以防鸣潮SB更新重启
                MainApplication.exit();
            }else {
                LOG.info("检测到鸣潮已经结束，游戏时长小于一分钟，不自动退出助手");
            }
        }
    }

    /**
     * @description: 判断当前游戏窗口是否存在
     * @param:
     * @return  boolean
     * @date:   2024/11/16
     */
    private boolean gameIsAlive(){
        return user32.IsWindow(game);
    }


    private void saveTime(List<GameRecordForLog> list){
        for (GameRecordForLog record : list) {
            save(record);
        }
    }
    public String getDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(date);
    }
    private void save(GameRecordForLog record){
        long endGameTime = record.getCloseTime(); //游戏结束时间
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

    public long getDuration() {
        if (start){
            long endGameTime = System.currentTimeMillis(); //游戏结束时间
            return endGameTime - startGameTime;
        }
        return 0;
    }
}