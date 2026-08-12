package legacy;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.game.model.game.FileInfo;
import com.kuro.game.model.game.GameResourceList;
import com.kuro.game.model.launcher.LauncherResource;
import com.kuro.game.model.launcher.item.UpdateData;
import com.kuro.game.thread.GameResourceListGetTask;
import com.kuro.game.thread.LauncherResourceTask;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @description:
 * @author: Leck
 * @create: 2025-02-15 22:35
 */
public class StageDemo extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        StackPane root = new StackPane();
        root.setScaleX(1.2);
        root.setScaleY(1.2);
        root.setStyle("-fx-background-color: #ecc0c0");
        root.setPrefWidth(1280);
        root.setPrefHeight(720);
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Stage Demo");
        stage.show();

        down();


    }


    void down(){
        LauncherResourceTask task = new LauncherResourceTask(LauncherResourceTask.Type.GLOBAL);
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

                        //resourceList.getResource().forEach(fileInfo -> System.out.println(fileInfo.getDest()));

                        //checkExistedFile(resourceList.getResource());
                        System.out.println("DDDDDD");
                        filterServerFile(resourceList.getResource());
                        System.out.println("fffffff");
                    }

                });
                Thread.startVirtualThread(resourceListGetTask);

            }
        });
        Thread.startVirtualThread(task);
    }



    void filterServerFile(List<FileInfo> list){

        List<FileInfo> serverFileList1 = list.stream().
                filter(fileInfo -> fileInfo.getDest().startsWith("Client/Binaries/Win64/Client-Win64-Shipping.exe") || fileInfo.getDest().startsWith("Client/Binaries/Win64/Client-Win64-ShippingBase.dll")).
                toList();
        serverFileList1.forEach(fileInfo -> System.out.println(fileInfo.getDest()));

        List<FileInfo> serverFileList2 = list.stream().filter(fileInfo -> fileInfo.getDest().startsWith("Client/Binaries/Win64/ThirdParty/KrPcSdk_")).toList();
        serverFileList2.forEach(fileInfo -> System.out.println(fileInfo.getDest()));
    }

    void checkExistedFile(List<FileInfo> list){
        Path gameDir = Path.of("G:\\Game\\Wuthering Waves\\Wuthering Waves Game");
        List<FileInfo> needDownLoadList = new ArrayList<>();
        System.out.println(list.size());
        list.forEach(fileInfo -> {
            File file = gameDir.resolve(fileInfo.getDest()).toFile();



            if (fileInfo.getSize() != file.length()){
                needDownLoadList.add(fileInfo);
                System.out.printf("%s的本地：%d，网络%s%n",file.getName(),file.length(),fileInfo.getSize());
            }


           /* Path path = gameDir.resolve(fileInfo.getDest());
            String md5 = md5(path);
            if (!md5.equals(fileInfo.getMd5())){
                needDownLoadList.add(fileInfo);
            }*/
        });

        System.out.println(needDownLoadList.size());
        long sum = needDownLoadList.stream().mapToLong(FileInfo::getSize).sum();
        System.out.println(sum);


    }


    private String md5(Path path){
        try (FileInputStream fileInputStream = new FileInputStream(path.toFile())) {
            String md5Hex = DigestUtils.md5Hex(fileInputStream);
            fileInputStream.close();
            return md5Hex;
        } catch (Exception e) {

        }
        return "";
    }

}