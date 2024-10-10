package cn.tealc.wutheringwavestool.jna;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.MainApplication;
import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import cn.tealc.wutheringwavestool.model.sign.UserInfo;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.MvvmFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * @program: WutheringWavesTool
 * @description: 有个坑，务必将其设置为全局变量，不知道为什么，不设置为全局变量，钩子无效
 * @author: Leck
 * @create: 2024-07-10 23:29
 */
public class GameAppListener implements WinUser.WinEventProc{
    private final Logger LOG= LoggerFactory.getLogger(GameAppListener.class);
    private static GameAppListener gameAppListener;
    private WinDef.HWND game;
    private boolean start=false;//标记游戏打开了
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
        //LOG.debug("当前前台窗口是:{}",title);
        if (title.equals("鸣潮  ") || title.equals("鸣潮  ") ){
            if (!start){
                game=hwnd;
                start = true;
                startGameTime = System.currentTimeMillis();
                LOG.info("检测到鸣潮已经启动");
            }
        }else if (title.equals(Config.appTitle)){ //进入到助手界面，刷新游戏时间
            if (start){
                long endGameTime = System.currentTimeMillis(); //游戏结束时间
                long totalGameTime = endGameTime - startGameTime;//总共游玩时间
                MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE,totalGameTime);
                save();
            }
        }else {
            if (start){ //只要游戏启动过，start必为true,所以不用处理其他情况
                long endGameTime = System.currentTimeMillis(); //游戏结束时间
                long totalGameTime = endGameTime - startGameTime;//总共游玩时间
                MvvmFX.getNotificationCenter().publish(NotificationKey.HOME_GAME_TIME_UPDATE,totalGameTime);
                save();
            }
        }
    }

    /**
     * @description: 统计游玩时间
     * @param:
     * @return  void
     * @date:   2024/7/21
     */
    private void save(){
        if (!user32.IsWindow(game)){//窗口已经被关闭
            start=false;
            long endGameTime = System.currentTimeMillis(); //游戏结束时间
            long totalGameTime = endGameTime - startGameTime;//总共游玩时间

            //获取游戏开始时间日期
            Instant instant = Instant.ofEpochMilli(startGameTime);
            ZoneId zone = ZoneId.systemDefault();
            ZonedDateTime zdt = instant.atZone(zone);
            LocalDateTime startDateTime = zdt.toLocalDateTime();
            LocalDate startDate = zdt.toLocalDate();

            //获取结束日期
            LocalDate localDate = LocalDate.now();

            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            GameTimeDao dao=new GameTimeDao();


            UserInfoDao userInfoDao = new UserInfoDao();
            UserInfo currentUser = userInfoDao.getMain();

            if (startDate.isBefore(localDate)){ //跨天
                LocalDateTime endOfDay = startDate.plusDays(1).atStartOfDay();
                long millisecondsUntilEndOfDay = ChronoUnit.MILLIS.between(startDateTime, endOfDay);
                GameTime gameTime1=new GameTime();
                if (currentUser != null){
                    gameTime1.setRoleId(currentUser.getRoleId());
                }
                gameTime1.setGameDate(dateTimeFormatter.format(startDate));
                gameTime1.setStartTime(startGameTime);
                gameTime1.setEndTime(startGameTime + millisecondsUntilEndOfDay);
                gameTime1.setDuration(millisecondsUntilEndOfDay);
                dao.addTime(gameTime1);
                LOG.info("检测到鸣潮已经结束且跨天，保存昨天时间{}",gameTime1);


                GameTime gameTime=new GameTime();
                if (currentUser != null){
                    gameTime.setRoleId(currentUser.getRoleId());
                }
                gameTime.setGameDate(dateTimeFormatter.format(localDate));
                long todayMillis = totalGameTime - millisecondsUntilEndOfDay;
                gameTime.setStartTime(endGameTime-todayMillis);
                gameTime.setEndTime(endGameTime);
                gameTime.setDuration(todayMillis);

                dao.addTime(gameTime);
                LOG.info("检测到鸣潮已经结束且跨天，保存今天时间{}",gameTime);

            }else {
                GameTime gameTime=new GameTime();

                if (currentUser != null){
                    gameTime.setRoleId(currentUser.getRoleId());
                }
                gameTime.setGameDate(dateTimeFormatter.format(localDate));
                gameTime.setStartTime(startGameTime);
                gameTime.setEndTime(endGameTime);
                gameTime.setDuration(totalGameTime);

                dao.addTime(gameTime);
                LOG.info("检测到鸣潮已经结束，保存时间{}",gameTime);
            }


            //判断是否开启自动退出，退出即结束程序
            if (Config.setting.isExitWhenGameOver()){
                if (totalGameTime > 60000){ //当游戏时长大于1分钟才自动关闭，以防鸣潮SB更新重启
                    MainApplication.exit();
                }else {
                    LOG.info("检测到鸣潮已经结束，游戏时长小于一分钟，不自动停止");
                }
            }

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