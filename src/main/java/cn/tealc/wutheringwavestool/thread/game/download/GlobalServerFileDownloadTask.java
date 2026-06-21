package cn.tealc.wutheringwavestool.thread.game.download;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.game.thread.GameResourceListGetTask;
import com.kuro.game.thread.LauncherResourceTask;
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
        LauncherResourceTask task = new LauncherResourceTask(LauncherResourceTask.Type.BILIBILI);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<LauncherResource> body = task.getValue();
            if (body.getCode() == 200){
                LauncherResource resource = body.getData();
                UpdateData updateData = resource.getUpdateData();
                String host = updateData.getCdnList().getFirst().getUrl();
                String indexUrl = host + updateData.getConfig().getIndexFile();
                GameResourceListGetTask resourceListGetTask = new GameResourceListGetTask(indexUrl);
                resourceListGetTask.setOnSucceeded(workerStateEvent1 -> {
                    ResponseBody<GameResourceList> body1 = resourceListGetTask.getValue();
                    if (body1.getCode() == 200){
                        GameResourceList resourceList = body1.getData();
                        String fileHost = host + updateData.getResourcesBasePath()+"/";
                        filterServerFile(resourceList.getResource(),fileHost);
                    }
                });
                resourceListGetTask.setOnFailed(workerStateEvent1 -> LOG.error("获取游戏资源列表失败", workerStateEvent1.getSource().getException()));
                Thread.startVirtualThread(resourceListGetTask);
            }
        });
        task.setOnFailed(workerStateEvent -> LOG.error("获取启动器资源失败", workerStateEvent.getSource().getException()));
        Thread.startVirtualThread(task);
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
