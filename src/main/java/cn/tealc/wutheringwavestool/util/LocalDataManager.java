package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.model.roleData.weight.PhantomWeight;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.scene.image.Image;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-10-03 00:01
 */
public class LocalDataManager {
    private static final Logger LOG= LoggerFactory.getLogger(LocalDataManager.class);
    private static final String WEIGHT_DEFAULT_DIR_TEMPLATE="assets/data/weight/default/%s.json";
    private static final String WEIGHT_CUSTOM_DIR_TEMPLATE="assets/data/weight/custom/%s.json";

    static {
        File dir=new File("assets/data/weight/default");
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (!result){
                LOG.error("无法创建缓存目录");
            }
        }
        File dir2=new File("assets/data/weight/custom");
        if (!dir2.exists()) {
            boolean result = dir2.mkdirs();
            if (!result){
                LOG.error("无法创建缓存目录");
            }
        }
    }

    public static PhantomWeight getWeight(String roleName){
        ObjectMapper mapper = new ObjectMapper();
        File file=new File(String.format(WEIGHT_CUSTOM_DIR_TEMPLATE,roleName));
        try {
            if (file.exists()){
                return mapper.readValue(file, PhantomWeight.class);
            }else {
                file=new File(String.format(WEIGHT_DEFAULT_DIR_TEMPLATE,roleName));
                if (file.exists()){
                    return mapper.readValue(file, PhantomWeight.class);
                }
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return null;
        }
        return null;
    }

}