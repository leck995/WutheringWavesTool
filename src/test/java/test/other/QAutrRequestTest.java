package test.other;

import cn.tealc.wutheringwavestool.model.ResponseBody;
import com.kuro.launcher.model.api.PlayerData;
import com.kuro.launcher.thread.api.QueryRoleTask;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class QAutrRequestTest extends Application {

    void getRequestJson() throws InterruptedException {
//        QueryRoleTask queryRole = new QueryRoleTask("100561390", "196a9117-867c-4032-a912-41eeb69d92aa");
//        // 2. 监听成功回调
//        queryRole.setOnSucceeded(event -> {
//            ResponseBody<PlayerData> value = queryRole.getValue();
//            if (value != null && value.getData() != null) {
//                System.out.println("角色名称: " + value.getData().getBaseData().getName());
//            } else {
//                System.err.println("收到空数据，请检查 Token 或 UID");
//            }
//        });
//        Thread.startVirtualThread(queryRole);

    }

    @Override
    public void start(Stage stage) throws Exception {

        stage.setScene(new Scene(new Pane(),200,220));
        stage.show();
        getRequestJson();
    }
}
