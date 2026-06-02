package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameGachaDao;
import cn.tealc.wutheringwavestool.model.game.pool.CardInfo;
import cn.tealc.wutheringwavestool.model.game.pool.GameGacha;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 抽卡记录服务，负责将 CardPoolRequestTask 获取的数据保存到 game_gacha 表
 *
 * @author leck
 * @date 2026/05/31
 */
@Singleton
public class GameGachaService {
    private static final Logger LOG = LoggerFactory.getLogger(GameGachaService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final GameGachaDao dao;

    @Inject
    public GameGachaService(GameGachaDao dao) {
        this.dao = dao;
    }

    /**
     * 将卡池数据转为 GameGacha 列表并保存到数据库
     *
     * @param playerId  玩家ID
     * @param gachaData 卡池名称 → 抽卡记录列表
     */
    public void saveGachaData(String playerId, Map<String, List<CardInfo>> gachaData) {
        List<GameGacha> allRecords = new ArrayList<>();
        for (Map.Entry<String, List<CardInfo>> entry : gachaData.entrySet()) {
            String gachaName = entry.getKey();
            for (CardInfo card : entry.getValue()) {
                GameGacha gacha = new GameGacha();
                gacha.setPlayerId(playerId);
                gacha.setGachaName(gachaName);
                gacha.setCardPoolType(card.getCardPoolType());
                gacha.setResourceId(card.getResourceId());
                gacha.setQualityLevel(card.getQualityLevel());
                gacha.setResourceType(card.getResourceType());
                gacha.setName(card.getName());
                gacha.setCount(card.getCount());
                gacha.setTime(LocalDateTime.parse(card.getTime(), TIME_FORMATTER)
                        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
                allRecords.add(gacha);
            }
        }
        if (!allRecords.isEmpty()) {
            dao.batchAdd(allRecords);
            LOG.info("保存玩家 {} 的抽卡数据，共 {} 条", playerId, allRecords.size());
        }
    }

    public List<GameGacha> getByPlayerId(String playerId) {
        return dao.getListByPlayerId(playerId);
    }

    public List<GameGacha> getByPlayerIdAndGachaName(String playerId, String gachaName) {
        return dao.getListByPlayerIdAndGachaName(playerId, gachaName);
    }

    public List<String> getAllPlayerIds() {
        return dao.getAllPlayerIds();
    }

    public int deleteByPlayerId(String playerId) {
        return dao.deleteByPlayerId(playerId);
    }
}
