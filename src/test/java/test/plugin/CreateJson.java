package test.plugin;

import cn.tealc.fxplugin.model.FxPluginType;
import cn.tealc.wutheringwavestool.plugin.FxPluginConfig;
import cn.tealc.wutheringwavestool.plugin.FxPluginLanguage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-12-21 01:00
 */
public class CreateJson {
    @Test
    public void WWTPoolExport() throws IOException {
        FxPluginLanguage cn = new FxPluginLanguage(Locale.SIMPLIFIED_CHINESE,"抽卡数据导入导出","抽卡数据导入导出插件");
        FxPluginLanguage td = new FxPluginLanguage(Locale.TRADITIONAL_CHINESE,"抽卡数据导入导出","抽卡数据导入导出插件");
        FxPluginLanguage en = new FxPluginLanguage(Locale.ENGLISH,"Gacha data import and export","Gacha data import and export");
        Map<Locale,FxPluginLanguage> languageMap = new HashMap<Locale,FxPluginLanguage>();
        languageMap.put(Locale.ENGLISH,en);
        languageMap.put(Locale.SIMPLIFIED_CHINESE,cn);
        languageMap.put(Locale.TRADITIONAL_CHINESE,td);

        FxPluginConfig config = new FxPluginConfig();
        config.setLanguages(languageMap);
        config.setId(1001);
        config.setAuthor("leck");
        config.setPath("wwt-pool-export/wwt-pool-export-1.0.jar");
        config.setVersion("1.0.0");
        config.setPluginType(FxPluginType.VIEW);

        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("plugins/wwt-pool-export/config.json"), config);



    }
}