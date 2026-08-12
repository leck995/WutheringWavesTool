package cn.tealc.wutheringwavestool.ui.system.home;

import cn.tealc.wutheringwavestool.base.NotificationKey;
import cn.tealc.wutheringwavestool.base.NotificationManager;
import cn.tealc.wutheringwavestool.model.LocalCachePlayerData;
import cn.tealc.wutheringwavestool.service.ConfigService;
import cn.tealc.wutheringwavestool.service.LauncherUserService;
import cn.tealc.wutheringwavestool.service.LocalCachePlayerDataService;
import cn.tealc.wutheringwavestool.ui.base.BaseViewModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.kuro.launcher.model.LocalCacheUser;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class SelectedPlayerByLocalViewModel extends BaseViewModel {
    private static final Logger LOG = LoggerFactory.getLogger(SelectedPlayerByLocalViewModel.class);

    @Inject
    private ObjectMapper objectMapper;
    @Inject
    private LocalCachePlayerDataService playerDataService;
    @Inject
    private ConfigService configService;
    @Inject
    private LauncherUserService launcherUserService;

    private final ObservableList<LocalCacheUser> localCacheUserList = FXCollections.observableArrayList();

    public void initialize() {
        readLocalCacheUser();
    }


    public void readLocalCacheUser() {
        Optional<List<LocalCacheUser>> localCacheUsers1 = launcherUserService.readLocalCacheUser();
        localCacheUsers1.ifPresent(localCacheUserList::addAll);

        for (LocalCacheUser localCacheUser : localCacheUserList) {
            Optional<LocalCachePlayerData> cuid = playerDataService.getByCuid(localCacheUser.getCuid());
            cuid.ifPresent(u -> localCacheUser.setThirdNickName(u.getRoleName()));
        }

//        Optional<String> u = configService.get(RoleBoardByLocalViewModel.LOCAL_CACHE_SELECTED_ROLE);
//        u.ifPresent(user ->{
//            for (int i = 0; i < localCacheUserList.size(); i++) {
//                LocalCacheUser localCacheUser = localCacheUserList.get(i);
//            }
//        }); //暂时搁置

    }

    public void update(int index){
        LocalCacheUser localCacheUser = localCacheUserList.get(index);
        NotificationManager.publish(NotificationKey.HOME_ROLE_LOCAL_CHANGE,localCacheUser);
    }


    public ObservableList<LocalCacheUser> getLocalCacheUserList() {
        return localCacheUserList;
    }
}
