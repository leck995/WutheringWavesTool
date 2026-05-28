package cn.tealc.wutheringwavestool.base;

import cn.tealc.wutheringwavestool.dao.ConfigDao;
import cn.tealc.wutheringwavestool.dao.GameRecordDao;
import cn.tealc.wutheringwavestool.dao.GameRoleDataDao;
import cn.tealc.wutheringwavestool.dao.GameSettingDao;
import cn.tealc.wutheringwavestool.dao.GameSlashDataDao;
import cn.tealc.wutheringwavestool.dao.GameTimeDao;
import cn.tealc.wutheringwavestool.dao.GameTowerDataDao;
import cn.tealc.wutheringwavestool.dao.OAuthCredentialDao;
import cn.tealc.wutheringwavestool.dao.SignHistoryDao;
import cn.tealc.wutheringwavestool.dao.UserInfoDao;
import cn.tealc.wutheringwavestool.service.ConfigService;
import cn.tealc.wutheringwavestool.service.OAuthCredentialService;

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
        bind(OAuthCredentialDao.class).in(Singleton.class);
        bind(OAuthCredentialService.class).in(Singleton.class);
        bind(ConfigService.class).in(Singleton.class);
        bind(DownloadProgressService.class).in(Singleton.class);
    }
}
