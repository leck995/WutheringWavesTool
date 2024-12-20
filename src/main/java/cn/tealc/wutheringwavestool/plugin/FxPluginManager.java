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
 * @description:
 * @author: Leck
 * @create: 2024-12-16 15:38
 */
public class FxPluginManager {
    private static final Logger LOG = LoggerFactory.getLogger(FxPluginManager.class);
    private static FxPluginManager instance;

    private Map<Integer,FxPluginConfig> plugins;
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
            try {
                for (File file : files) {
                    File configFile = new File(file.getAbsolutePath() + "/config.json");
                    if (configFile.exists()) {
                        FxPluginConfig fxPluginConfig = mapper.readValue(configFile, FxPluginConfig.class);
                        plugins.put(fxPluginConfig.getId(),fxPluginConfig);
                        LOG.info("载入插件信息： {}",fxPluginConfig.getTitle());
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    public Optional<FxPlugin> loadPlugins(Integer id) {
        if (plugins.containsKey(id)) {
            return initPlugin(plugins.get(id));
        }
        return Optional.empty();
    }


    private Optional<FxPlugin> initPlugin(FxPluginConfig config){
        File file = new File("plugins/"+config.getPath());
        FxPluginLoader fxPluginLoader = new FxPluginLoader();
        try {
            Optional<FxPlugin> plugin = fxPluginLoader.loadPlugin(file.getAbsolutePath());
            LOG.info("加载插件： {}",config.getTitle());
            return plugin;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}