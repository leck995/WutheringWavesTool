package test.download;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.CdnData;
import com.kuro.game.thread.LauncherResourceTask;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Comparator;
import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-06-20 19:53
 */
public class DownloadJsonCrerat extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        stage.setTitle("Download JSON Crerat");
        stage.setWidth(400);
        stage.setHeight(400);
        stage.show();
        request();
    }


    private void request(){
        LauncherResourceTask task = new LauncherResourceTask(LauncherResourceTask.Type.BILIBILI);
        task.setOnSucceeded(workerStateEvent -> {
            ResponseBody<LauncherResource> value = task.getValue();
            List<CdnData> cdnList = value.getData().getUpdateData().getCdnList();
            cdnList.sort(Comparator.comparingInt(CdnData::getPing));
            CdnData cdnData = cdnList.getFirst();

            String host = cdnData.getUrl();





        });
    }

}