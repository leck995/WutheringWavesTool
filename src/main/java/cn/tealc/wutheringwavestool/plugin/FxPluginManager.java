package cn.tealc.wutheringwavestool.plugin;

import cn.tealc.fxplugin.FxPlugin;
import cn.tealc.fxplugin.FxPluginLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @program: WutheringWavesTool
 * @description: 管理插件的加载与实例化
 * @author: Leck
 * @create: 2024-12-16 15:38
 */
public class FxPluginManager {
    private static final Logger LOG = LoggerFactory.getLogger(FxPluginManager.class);
    private static FxPluginManager instance;

    private final Map<Integer,FxPluginConfig> plugins;
    private FxPluginManager() {
        plugins = new HashMap<>();
        loadPluginConfig();
    }

    public static FxPluginManager getInstance() {
        if (instance == null) {
            instance = new FxPluginManager();
        }
        return instance;
    }



    private void loadPluginConfig(){
        File dir = new File("plugins");
        if (dir.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            File[] files = dir.listFiles(File::isDirectory);
            if (files != null) {
                try {
                    for (File file : files) {
                        File configFile = new File(file.getAbsolutePath() + "/config.json");
                        if (configFile.exists()) {
                            FxPluginConfig fxPluginConfig = mapper.readValue(configFile, FxPluginConfig.class);
                            if (!plugins.containsKey(fxPluginConfig.getId())) {
                                plugins.put(fxPluginConfig.getId(),fxPluginConfig);
                                LOG.info("载入插件信息： {}",fxPluginConfig.getTitle());
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.error("加载插件配置信息失败");
                    throw new RuntimeException(e);
                }
            }

        }
    }


    /**
     * @description: 根据插件ID加载插件
     * @param:	id
     * @return  java.util.Optional<cn.tealc.fxplugin.FxPlugin>
     * @date:   2024/12/21
     */
    public Optional<FxPlugin> loadPlugins(Integer id) {
        if (plugins.containsKey(id)) { //先判断是否存在
            return initPlugin(plugins.get(id));
        }else {//不存在，重新加载插件
            loadPluginConfig();
            if (plugins.containsKey(id)) { //仍不存在
                return initPlugin(plugins.get(id));
            }
        }
        return Optional.empty();
    }


    /**
     * description: 通过插件配置信息加载插件
     *
     * @param config
     * @return
     */
    private Optional<FxPlugin> initPlugin(FxPluginConfig config){
        File file = new File("plugins/"+config.getPath());
        FxPluginLoader fxPluginLoader = new FxPluginLoader();
        try {
            Optional<FxPlugin> plugin = fxPluginLoader.loadPlugin(file.getAbsolutePath());
            LOG.info("成功加载插件： {}",config.getTitle());
            return plugin;
        } catch (Exception e) {
            LOG.error("加载插件失败",e);
        }
        return Optional.empty();
    }
}