package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.GameRoleDataDao;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kuro.kujiequ.model.roleData.Role;

/**
 * 游戏角色数据服务，包装 {@link GameRoleDataDao}
 */
@Singleton
public class GameRoleDataService {

    private final GameRoleDataDao gameRoleDataDao;

    @Inject
    public GameRoleDataService(GameRoleDataDao gameRoleDataDao) {
        this.gameRoleDataDao = gameRoleDataDao;
    }

    public Role getRoleDataById(int id) {
        return gameRoleDataDao.getRoleDataById(id);
    }

    public int add(Role role) {
        return gameRoleDataDao.add(role);
    }
}