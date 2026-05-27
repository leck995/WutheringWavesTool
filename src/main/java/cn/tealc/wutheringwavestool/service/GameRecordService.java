package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameRecordDao;
import cn.tealc.wutheringwavestool.model.game.GameRecord;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class GameRecordService {
    private final GameRecordDao gameRecordDao;

    @Inject
    public GameRecordService(GameRecordDao gameRecordDao) {
        this.gameRecordDao = gameRecordDao;
    }

    /** 聚合单条或多个 GameRecord 的字段求和 */
    public static GameRecordAggregation aggregate(List<GameRecord> records) {
        GameRecordAggregation agg = new GameRecordAggregation();
        if (records == null) return agg;
        for (GameRecord r : records) {
            agg.battle += r.getBattle();
            agg.paralysis += r.getParalysis();
            agg.parryFront += r.getParryFront();
            agg.parryBack += r.getParryBack();
            agg.parryAttack += r.getParryAttack();
            agg.phantomGet += r.getPhantomGet();
            agg.phantomCallSkill += r.getPhantomCallSkill();
            agg.phantomTransformSkill += r.getPhantomTransformSkill();
            agg.roleChange += r.getRoleChange();
            agg.roleDeath += r.getRoleDeath();
            agg.transfer += r.getTransfer();
            agg.usedStrength += r.getUsedStrength();
        }
        return agg;
    }

    public List<GameRecord> getRecordsByDate(String date) {
        return gameRecordDao.getRecordListByDate(date);
    }

    public List<GameRecord> getRecordsByRoleIdAndDate(String roleId, String date) {
        return gameRecordDao.getRecordListByRoleIdAndDate(roleId, date);
    }

    public List<GameRecord> getRecordsByRoleId(String roleId) {
        return gameRecordDao.getRecordListByRoleId(roleId);
    }

    public List<String> getAllRoleIds() {
        return gameRecordDao.getAllRoleId();
    }

    public static class GameRecordAggregation {
        public int battle;
        public int paralysis;
        public int parryFront;
        public int parryBack;
        public int parryAttack;
        public int phantomGet;
        public int phantomCallSkill;
        public int phantomTransformSkill;
        public int roleChange;
        public int roleDeath;
        public int transfer;
        public int usedStrength;

        /** 招架总数 = 极限闪避 + 闪避反击 */
        public int getParryTotal() {
            return parryFront + parryBack;
        }

        /** 声骸技能总数 = 召唤 + 变身 */
        public int getPhantomSkillTotal() {
            return phantomCallSkill + phantomTransformSkill;
        }
    }
}
