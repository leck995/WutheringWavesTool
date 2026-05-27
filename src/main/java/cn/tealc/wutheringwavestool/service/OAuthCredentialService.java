package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.OAuthCredentialDao;
import cn.tealc.wutheringwavestool.model.OAuthCredential;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Singleton
public class OAuthCredentialService {
    private static final Logger LOG = LoggerFactory.getLogger(OAuthCredentialService.class);
    private final OAuthCredentialDao dao;

    @Inject
    public OAuthCredentialService(OAuthCredentialDao dao) {
        this.dao = dao;
    }

    public Optional<OAuthCredential> getByRoleId(String roleId) {
        LOG.info("通过 roleId 查询 OAuth 凭证: {}", roleId);
        return Optional.ofNullable(dao.getByRoleId(roleId));
    }

    public Optional<OAuthCredential> getByOauthCode(String oauthCode) {
        LOG.info("通过 oauthCode 查询 OAuth 凭证");
        return Optional.ofNullable(dao.getByOauthCode(oauthCode));
    }

    public Optional<String> getRoleIdByOauthCode(String oauthCode) {
        LOG.info("通过 oauthCode 查询 roleId");
        String roleId = dao.getRoleIdByOauthCode(oauthCode);
        LOG.debug("查询结果, oauthCode: {}, roleId: {}", oauthCode, roleId);
        return Optional.ofNullable(roleId);
    }

    public List<OAuthCredential> getAll() {
        LOG.info("查询所有 OAuth 凭证");
        List<OAuthCredential> list = dao.getAll();
        LOG.debug("共 {} 条记录", list.size());
        return list;
    }

    public int saveOrUpdate(String roleId, String oauthCode) {
        long now = System.currentTimeMillis() / 1000;
        LOG.info("保存 OAuth 凭证, roleId: {}, oauthCode: {}", roleId, oauthCode);
        return dao.saveOrUpdate(roleId, oauthCode, now);
    }

    public int updateOauthCode(String oldOauthCode, String newOauthCode) {
        long now = System.currentTimeMillis() / 1000;
        LOG.info("更新 oauthCode, old: {}, new: {}", oldOauthCode, newOauthCode);
        return dao.updateOauthCode(oldOauthCode, newOauthCode, now);
    }

    public int deleteByRoleId(String roleId) {
        LOG.info("删除 OAuth 凭证, roleId: {}", roleId);
        return dao.deleteByRoleId(roleId);
    }

    public int deleteByOauthCode(String oauthCode) {
        LOG.info("删除 OAuth 凭证, oauthCode: {}", oauthCode);
        return dao.deleteByOauthCode(oauthCode);
    }

    public int count() {
        return dao.count();
    }
}
