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
        LocalDate date1 = Instant.ofEpochMilli(startGameTime).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate date2 = Instant.ofEpochMilli(endGameTime).atZone(ZoneId.systemDefault()).toLocalDate();
        boolean sameDay = date1.isEqual(date2);
        long totalGameTime;
        if (sameDay) { //同天
            totalGameTime = endGameTime - startGameTime; // 同一天，总共游玩时间
        } else {//不同天，计算今天的开始时间
            LocalDate today = LocalDate.now();
            long startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            totalGameTime = endGameTime - startOfToday; // 不同一天，从今天开始到结束的时间差
        }
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


    public long getDuration() {
        if (start){
            long endGameTime = System.currentTimeMillis(); //游戏结束时间
            return endGameTime - startGameTime;
        }
        return 0;
    }
}