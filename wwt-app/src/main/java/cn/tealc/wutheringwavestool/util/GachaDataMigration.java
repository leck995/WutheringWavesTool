package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.base.AppInjector;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.service.GameGachaService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 将 data 目录下所有本地抽卡数据迁移到 game_gacha 表
 *
 * @author leck
 * @date 2026/05/31
 */
public class GachaDataMigration {
    private static final Logger LOG = LoggerFactory.getLogger(GachaDataMigration.class);
    private static final File DATA_DIR = new File("data");

    public static void migrate() {
        if (!DATA_DIR.exists() || !DATA_DIR.isDirectory()) {
            LOG.info("data 目录不存在，跳过迁移");
            return;
        }

        GameGachaService service = AppInjector.getInstance(GameGachaService.class);
        ObjectMapper mapper = AppInjector.getInstance(ObjectMapper.class);

        File[] playerDirs = DATA_DIR.listFiles(File::isDirectory);
        if (playerDirs == null) {
            LOG.info("data 目录下无玩家数据");
            return;
        }

        int totalPlayers = 0;
        int totalRecords = 0;
        for (File playerDir : playerDirs) {
            String playerId = playerDir.getName();
            File poolFile = new File(playerDir, "pool.json");
            if (!poolFile.exists()) {
                LOG.debug("玩家 {} 的 pool.json 不存在，跳过", playerId);
                continue;
            }

            try {
                Map<String, List<CardInfo>> gachaData = mapper.readValue(
                        poolFile, new TypeReference<Map<String, List<CardInfo>>>() {});
                service.saveGachaData(playerId, gachaData);
                totalPlayers++;
                totalRecords += gachaData.values().stream().mapToInt(List::size).sum();
            } catch (IOException e) {
                LOG.error("读取玩家 {} 的 pool.json 失败", playerId, e);
            }
        }

        LOG.info("迁移完成: {} 个玩家, 共 {} 条抽卡记录", totalPlayers, totalRecords);
    }
}
