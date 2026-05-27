package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.SourceType;
import com.fasterxml.jackson.core.type.TypeReference;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kuro.launcher.model.LocalCacheUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

public class SelectedPlayerViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(SelectedPlayerViewModel.class);

    @Inject
    private ObjectMapper objectMapper;

    private ObservableList<LocalCacheUser> localCacheUserList = FXCollections.observableArrayList();

    public SelectedPlayerViewModel() {
        readLocalCacheUser();
    }




    public void readLocalCacheUser() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            System.err.println("错误: 无法找到 APPDATA 环境变量。");
        }
        String path = null;
        if (Config.setting.getGameRootDirSource() == SourceType.GLOBAL) {
            path = Paths.get(appData, "KR_G153", "A1730", "KRSDKUserLauncherCache.json").toString();
        } else {
            path = Paths.get(appData, "KR_G152", "A1381", "KRSDKUserLauncherCache.json").toString();
        }
        File file = new File(path);
        if (!file.exists()) {
            System.err.println("错误: 文件未找到: " + path);
            return;
        }
        try {
            List<LocalCacheUser> localCacheUsers = objectMapper.readValue(file, new TypeReference<List<LocalCacheUser>>() {
            });
            LOG.debug("从 KRSDKUserLauncherCache 读取到 {} 个用户数据", localCacheUsers.size());
            localCacheUserList.addAll(localCacheUsers);
        } catch (IOException e) {
            LOG.debug("从 KRSDKUserLauncherCache 读取数据失败", e);
        }
    }

    public void update(int index){
        LocalCacheUser localCacheUser = localCacheUserList.get(index);



    }


    public ObservableList<LocalCacheUser> getLocalCacheUserList() {
        return localCacheUserList;
    }
}
