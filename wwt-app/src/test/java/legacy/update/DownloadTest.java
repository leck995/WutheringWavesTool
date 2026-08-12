package legacy.update;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.game.thread.GameFileDownloadTask;
import com.kuro.game.thread.GameResourceListGetTask;
import com.kuro.game.thread.LauncherResourceTask;
import com.kuro.game.thread.server.ServerUniqueFileDownload;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class DownloadTest extends Application {
    @Override
    public void start(Stage stage) throws Exception {
 /*       LauncherResourceTask task = new LauncherResourceTask(LauncherResourceTask.Type.CN);
        task.setOnSucceeded(event -> {
            ResponseBody<LauncherResource> value = task.getValue();
            if (value.getCode() == 200){
                System.out.println(value.getData().getUpdateData().getResourceJsonUrl());
                downloadGameResource(value.getData().getUpdateData());
            }
        });
        Thread.startVirtualThread(task);*/
        ServerUniqueFileDownload task = new ServerUniqueFileDownload();
        Thread.startVirtualThread(task);

    }


    void download(List<FileInfo> fileInfoList){
        List<String> strings = List.of("Client/Binaries/Win64/ThirdParty/KrPcSdk_Mainland",
                "Client/Binaries/Win64/Client-Win64-Shipping.exe",
                "Client/Binaries/Win64/Client-Win64-ShippingBase.dll");


        List<FileInfo> requestDownFileInfoList = new ArrayList<>();
        strings.forEach(string -> {
            List<FileInfo> list = fileInfoList.stream().filter(fileInfo -> fileInfo.getDest().startsWith(string)).toList();
            requestDownFileInfoList.addAll(list);
        });

        requestDownFileInfoList.forEach(fileInfo -> {
            System.out.println(fileInfo.getDest());
        });



        //updateData.resourcesDiff

    }

    void downloadGameResource(UpdateData updateData){
        String resourceJsonUrl = updateData.getResourceJsonUrl();


        GameResourceListGetTask  task = new GameResourceListGetTask(resourceJsonUrl);
        task.setOnSucceeded(event -> {
            ResponseBody<GameResourceList> value = task.getValue();
            if (value.getCode() == 200){
                System.out.println(value.getData().getResource().size());
                download(value.getData().getResource());
            }
        });
        Thread.startVirtualThread(task);
    }
}
