package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.thread.gacha.cloud.GachaCloudUploadTask;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;

@Singleton
public class GachaCloudService {
    @Inject
    public GachaCloudService() {
    }

    public GachaCloudUploadTask upload(String filename, File file){
        String username = Config.setting().getServerUsername();
        String password = Config.setting().getServerPassword();

        GachaCloudUploadTask task = new GachaCloudUploadTask(username,password,filename,file);
        return task;
    }
}
