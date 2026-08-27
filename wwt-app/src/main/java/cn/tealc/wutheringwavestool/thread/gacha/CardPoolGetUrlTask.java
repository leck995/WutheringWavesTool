package cn.tealc.wutheringwavestool.thread.gacha;

import cn.tealc.wutheringwavestool.base.Config;
import cn.tealc.wutheringwavestool.model.ResponseBody;
import cn.tealc.wutheringwavestool.util.GameResourcesManager;
import cn.tealc.wutheringwavestool.util.GachaLogUtil;
import cn.tealc.wutheringwavestool.util.LanguageManager;
import javafx.concurrent.Task;

import java.io.File;

/**
 * @program: WutheringWavesTool
 * @description: 获取卡池数据
 * @author: Leck
 * @create: 2024-07-03 00:14
 */
public class CardPoolGetUrlTask extends Task<ResponseBody<String>> {

    @Override
    protected ResponseBody<String> call() throws Exception {
        String dir = Config.setting().getGameRootDir();
        if (dir != null) {
            File gameLogFile = GameResourcesManager.getGameLogFile();
            if (gameLogFile.exists()) {
                String url = GachaLogUtil.getLogFileUrl(gameLogFile);
                if (url != null){
                    return ResponseBody.create(200,"获取成功",url);
                }else { //找不到卡池链接时
                    return new ResponseBody<>(101,LanguageManager.getString("ui.analysis.message.type04"));
                }
            }else { //日志文件不存在时
                return new ResponseBody<>(102,String.format(LanguageManager.getString("ui.analysis.message.type05"),gameLogFile.getAbsolutePath()));
            }
        }else { //游戏根目录未设置
            return new ResponseBody<>(103,LanguageManager.getString("ui.analysis.message.type06"));
        }
    }
}
