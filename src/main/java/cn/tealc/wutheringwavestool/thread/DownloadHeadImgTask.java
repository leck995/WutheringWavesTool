package cn.tealc.wutheringwavestool.thread;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Deprecated
public class DownloadHeadImgTask extends Task<Boolean> {
    private static final Logger LOG= LoggerFactory.getLogger(DownloadHeadImgTask.class);
    private static final String HEAD_JSON_URL="https://raw.githubusercontent.com/leck995/WutheringWavesTool/new-ui/assets/data/header.json";
    public static boolean hasUpdate=false; //标记程序生命周期内是否已经更新过，更新过不再更新

    @Override
    protected Boolean call() throws Exception {
        if (!hasUpdate){
            hasUpdate=true;
            URI uri = URI.create(HEAD_JSON_URL);
            ObjectMapper mapper = new ObjectMapper();
            HashMap<String, String> map = mapper.readValue(uri.toURL(), new TypeReference<HashMap<String, String>>() {
            });
            File dir=new File("assets/header/");
            if (!dir.exists()){
                dir.mkdirs();
            }
            for (Map.Entry<String, String> entry : map.entrySet()) {
                File file = new File("assets/header/"+entry.getKey()+".png");

                if(!file.exists()){
                    LOG.info("开始下载头像:{}",file.getName());
                    URL url = URI.create(entry.getValue()).toURL();
                    BufferedImage read = ImageIO.read(url);
                    ImageIO.write(read, "png", file);
                    LOG.info("头像:{} 保存完毕",file.getName());
                }
            }
            LOG.info("头像全部保存完毕");
            return true;
        }else {
            return false;
        }
    }
}