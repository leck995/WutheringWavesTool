package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.kuro.game.GameManager;
import com.kuro.game.model.Type;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.model.ResponseBody;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * 多线程下载任务，传入文件列表、URL前缀和保存目录进行并发下载，进度上报至 DownloadProgressService
 */
public class GlobalServerFileDownloadTask extends Task<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalServerFileDownloadTask.class);
    public GlobalServerFileDownloadTask() {

    }

    @Override
    protected Void call() throws Exception {
        try {
            GameManager gameManager = AppInjector.getInstance(GameManager.class);
            ResponseBody<LauncherResource> body = gameManager.getLauncherResource(Type.BILIBILI);
            if (body.getCode() == 200 && body.getData() != null){
                LauncherResource resource = body.getData();
                UpdateData updateData = resource.getUpdateData();
                String host = updateData.getCdnList().getFirst().getUrl();
                String indexUrl = host + updateData.getConfig().getIndexFile();
                ResponseBody<GameResourceList> body1 = gameManager.getGameResourceList(indexUrl);
                if (body1.getCode() == 200 && body1.getData() != null){
                    GameResourceList resourceList = body1.getData();
                    String fileHost = host + updateData.getResourcesBasePath()+"/";
                    filterServerFile(resourceList.getResource(), fileHost);
                }
            }
        } catch (Exception e) {
            LOG.error("获取启动器/游戏资源失败", e);
        }
        return null;
    }


    void filterServerFile(List<FileInfo> list,String host){
        List<FileInfo> fileInfos = list.stream()
                .filter(f -> f.getDest().startsWith("Client/Binaries/Win64/Client-Win64-Shipping.exe") ||
                        f.getDest().startsWith("Client/Binaries/Win64/Client-Win64-ShippingBase.dll") ||
                        f.getDest().startsWith("Client/Binaries/Win64/ThirdParty/KrPcSdk_"))
                .toList();
        File gameDir = GameResourcesManager.getGameDir();
        if (gameDir != null){
            File savePath = new File(gameDir,"WwtBackup/bilibili");
            System.out.println(host);
            ResourceDownloadTask downloadTask = new ResourceDownloadTask(fileInfos,host,savePath);
            Thread.startVirtualThread(downloadTask);
        }

    }
}
