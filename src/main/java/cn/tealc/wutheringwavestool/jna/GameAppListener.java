package cn.tealc.wutheringwavestool.jna;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.NotificationKey;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.model.game.GameTime;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import de.saxsys.mvvmfx.MvvmFX;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

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
        if (title.equals("鸣潮  ")){
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
            }
        }else {
            if (start) {//当游戏已经启动，进入后台窗口时
                if (!user32.IsWindow(game)){//窗口已经被关闭
                    start=false;
                    long endGameTime = System.currentTimeMillis(); //游戏结束时间
                    long totalGameTime = endGameTime - startGameTime;//总共游玩时间
                    LocalDate localDate = LocalDate.now();
                    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    GameTime gameTime=new GameTime();
                    if (Config.currentRoleId != null){
                        gameTime.setRoleId(Config.currentRoleId);
                    }
                    gameTime.setGameDate(dateTimeFormatter.format(localDate));
                    gameTime.setStartTime(startGameTime);
                    gameTime.setEndTime(endGameTime);
                    gameTime.setDuration(totalGameTime);
                    GameTimeDao dao=new GameTimeDao();
                    dao.addTime(gameTime);
                    LOG.info("检测到鸣潮已经结束，保存时间{}",gameTime);
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