package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.Config;
import cn.tealc.wutheringwavestool.model.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public class CheckVersionTask extends Task<Integer> {
    private static final String NET_URL="https://api.github.com/repos/leck995/WutheringWavesTool/releases/latest";
    private static final String TIP="发现新版本：%s,可在设置中获取更新详细信息";
    private static final String TIP_ERROR="检查版本更新失败，请检查网络状况";
    @Override
    protected Integer call() throws Exception {
        Thread.sleep(3000);
        ObjectMapper objectMapper=new ObjectMapper();
        URL url= null;
        try {
            url = new URL(NET_URL);

            Version versionInfo = objectMapper.readValue(url, Version.class);
            //Map<String, String> versionMap = objectMapper.readValue(url, Map.class);

            String version=versionInfo.getTag_name();
            double net = Double.parseDouble(version.replace(".",""));
            double now = Double.parseDouble(Config.version.replace(".",""));
            if (now < net){
                System.out.println("有新版本");
                return 1;//有新版本
            }else {
                return 0;//无新版本
            }
        } catch (IOException e) {
            System.out.println("无法检测更新");
            return -1; //无法检测更新
        }
    }
}