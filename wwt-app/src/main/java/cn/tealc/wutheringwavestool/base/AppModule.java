package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.dao.*;
import cn.tealc.wutheringwavestool.service.*;
import cn.tealc.wutheringwavestool.ui.system.tray.TrayIconManager;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class AppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(UserInfoDao.class).in(Singleton.class);
        bind(GameTimeDao.class).in(Singleton.class);
        bind(GameRecordDao.class).in(Singleton.class);
        bind(GameRoleDataDao.class).in(Singleton.class);
        bind(GameTowerDataDao.class).in(Singleton.class);
        bind(GameSlashDataDao.class).in(Singleton.class);
        bind(SignHistoryDao.class).in(Singleton.class);
        bind(ConfigDao.class).in(Singleton.class);
        bind(GameSettingDao.class).in(Singleton.class);
        bind(LocalCachePlayerDataDao.class).in(Singleton.class);
        bind(LocalCachePlayerDataService.class).in(Singleton.class);
        bind(ConfigService.class).in(Singleton.class);
        bind(TaskManageService.class).in(Singleton.class);
        bind(AutoSignService.class).in(Singleton.class);
        bind(GameWindowMonitorService.class).in(Singleton.class);
        bind(TrayIconManager.class).in(Singleton.class);
        bind(TokenRefreshService.class).in(Singleton.class);
        bind(WebKujiequManager.class).in(Singleton.class);
        bind(GachaStatService.class).in(Singleton.class);
        bind(GameTimeService.class).in(Singleton.class);
        bind(GameRecordService.class).in(Singleton.class);
        bind(UserInfoService.class).in(Singleton.class);
        bind(LauncherUserService.class).in(Singleton.class);
        bind(GameSettingService.class).in(Singleton.class);
        bind(GameRoleDataService.class).in(Singleton.class);
    }
}
