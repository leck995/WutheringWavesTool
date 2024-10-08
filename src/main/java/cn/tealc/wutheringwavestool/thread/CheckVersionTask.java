package cn.tealc.wutheringwavestool.thread;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;

public class CheckVersionTask extends Task<Integer> {
    private static final Logger LOG= LoggerFactory.getLogger(CheckVersionTask.class);
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
            if (!versionInfo.isPrerelease()){
                String version=versionInfo.getTag_name();
                double net = Double.parseDouble(version.replace(".",""));
                double now = Double.parseDouble(Config.version.replace(".",""));
                if (now < net){
                    LOG.info("检检测到有新版本需要更新");
                    return 1;//有新版本
                }else {
                    return 0;//无新版本
                }
            }
            return 0;
        } catch (IOException e) {
            LOG.error("检测更新出现异常",e);
            return -1; //无法检测更新
        }
    }
}