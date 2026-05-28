package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.LocalCachePlayerDataDao;
import cn.tealc.wutheringwavestool.model.LocalCachePlayerData;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Singleton
public class LocalCachePlayerDataService {
    private static final Logger LOG = LoggerFactory.getLogger(LocalCachePlayerDataService.class);
    private final LocalCachePlayerDataDao dao;

    @Inject
    public LocalCachePlayerDataService(LocalCachePlayerDataDao dao) {
        this.dao = dao;
    }

    public Optional<LocalCachePlayerData> getByRoleId(String roleId) {
        LOG.info("通过 roleId 查询玩家缓存数据: {}", roleId);
        return Optional.ofNullable(dao.getByRoleId(roleId));
    }

    public Optional<LocalCachePlayerData> getByOauthCode(String oauthCode) {
        LOG.info("通过 oauthCode 查询玩家缓存数据");
        return Optional.ofNullable(dao.getByOauthCode(oauthCode));
    }

    public Optional<LocalCachePlayerData> getByCuid(String cuid) {
        LOG.info("通过 cuid 查询玩家缓存数据: {}", cuid);
        return Optional.ofNullable(dao.getByCuid(cuid));
    }

    public Optional<String> getRoleIdByOauthCode(String oauthCode) {
        LOG.info("通过 oauthCode 查询 roleId");
        String roleId = dao.getRoleIdByOauthCode(oauthCode);
        LOG.debug("查询结果, oauthCode: {}, roleId: {}", oauthCode, roleId);
        return Optional.ofNullable(roleId);
    }

    public List<LocalCachePlayerData> getAll() {
        LOG.info("查询所有玩家缓存数据");
        List<LocalCachePlayerData> list = dao.getAll();
        LOG.debug("共 {} 条记录", list.size());
        return list;
    }

    public int saveOrUpdate(LocalCachePlayerData data) {
        data.setUpdateTime(System.currentTimeMillis() / 1000);
        LOG.info("保存玩家缓存数据, roleId: {}, roleName: {}, level: {}",
                data.getRoleId(), data.getRoleName(), data.getLevel());
        return dao.saveOrUpdate(data);
    }

    /** 仅保存基本凭证信息，roleName/level 等字段留空 */
    public int saveOrUpdate(String roleId, String oauthCode) {
        LocalCachePlayerData data = new LocalCachePlayerData();
        data.setRoleId(roleId);
        data.setOauthCode(oauthCode);
        return saveOrUpdate(data);
    }

    public int updateOauthCode(String oldOauthCode, String newOauthCode) {
        long now = System.currentTimeMillis() / 1000;
        LOG.info("更新 oauthCode, old: {}, new: {}", oldOauthCode, newOauthCode);
        return dao.updateOauthCode(oldOauthCode, newOauthCode, now);
    }

    public int deleteByRoleId(String roleId) {
        LOG.info("删除玩家缓存数据, roleId: {}", roleId);
        return dao.deleteByRoleId(roleId);
    }

    public int deleteByOauthCode(String oauthCode) {
        LOG.info("删除玩家缓存数据, oauthCode: {}", oauthCode);
        return dao.deleteByOauthCode(oauthCode);
    }

    public int count() {
        return dao.count();
    }
}
