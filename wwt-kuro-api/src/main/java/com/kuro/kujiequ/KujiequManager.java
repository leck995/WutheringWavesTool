package com.kuro.kujiequ;

import com.kuro.kujiequ.api.KujiequAuthApi;
import com.kuro.kujiequ.api.KujiequCalculatorApi;
import com.kuro.kujiequ.api.KujiequRoleApi;
import com.kuro.kujiequ.api.KujiequSignApi;
import com.kuro.kujiequ.api.KujiequTowerApi;
import com.kuro.kujiequ.api.KujiequUserApi;
import com.kuro.kujiequ.model.calculator.exist.ExistedRoleDataForCalculator;
import com.kuro.kujiequ.model.calculator.exist.RoleAim;
import com.kuro.kujiequ.model.calculator.exist.WeaponAim;
import com.kuro.kujiequ.model.calculator.list.RoleForCalculator;
import com.kuro.kujiequ.model.calculator.list.WeaponForCalculator;
import com.kuro.kujiequ.model.calculator.result.CalculatorResult;
import com.kuro.kujiequ.model.newTowerData.NewTowerData;
import com.kuro.kujiequ.model.resourcebriefing.Briefing;
import com.kuro.kujiequ.model.resourcebriefing.Record;
import com.kuro.kujiequ.model.roleData.Role;
import com.kuro.kujiequ.model.roleData.RoleDetail;
import com.kuro.kujiequ.model.roleData.user.RoleDailyData;
import com.kuro.kujiequ.model.roleData.user.RoleInfo;
import com.kuro.kujiequ.model.sign.UserInfo;
import com.kuro.kujiequ.model.slash.SlashData;
import com.kuro.kujiequ.model.towerData.DifficultyTotal;
import com.kuro.model.ResponseBody;

import java.util.List;

/**
 * 库街区 API Facade：将原 19 个 JavaFX Task 的请求逻辑收敛为同步方法。
 * <p>
 * 本类为纯 API 模块，零 JavaFX 依赖。所有方法同步返回 {@link ResponseBody}，
 * 落库（saveToDB）与时间戳转换（convertToHourlyTimestamp）已上移到 app service 层。
 * <p>
 * 自拆分重构后，实际请求逻辑下沉至 {@code com.kuro.kujiequ.api} 子包下的 6 个子 Manager：
 * <ul>
 *   <li>{@link KujiequRoleApi}       — 角色数据</li>
 *   <li>{@link KujiequUserApi}       — 用户数据</li>
 *   <li>{@link KujiequTowerApi}      — 塔/矩阵/海墟</li>
 *   <li>{@link KujiequCalculatorApi} — 养成计算器</li>
 *   <li>{@link KujiequSignApi}       — 签到/资源简报（含 {@link KujiequSignApi.SignGoodsResult} 与 {@link KujiequSignApi.BriefingType}）</li>
 *   <li>{@link KujiequAuthApi}       — 认证/短信/token（含 {@link KujiequAuthApi#randomDevCode()} 等静态方法）</li>
 * </ul>
 * 本 Facade 仅做委托转发，保留原所有 public 方法签名以保持向后兼容。
 * <p>
 * 原 Task 对照：
 * <ul>
 *   <li>{@link #getGameRoleData}        ← GameRoleDataTask</li>
 *   <li>{@link #getGameRoleDetail}      ← GameRoleDetailTask</li>
 *   <li>{@link #seekGameRole}           ← GameRoleSeekTask</li>
 *   <li>{@link #refreshUserData}        ← UserDataRefreshTask</li>
 *   <li>{@link #getUserDailyData}       ← UserDailyDataTask</li>
 *   <li>{@link #getSignGoods}           ← SignGoodsTask</li>
 *   <li>{@link #getBriefingList}        ← BriefingListGetTask</li>
 *   <li>{@link #getBriefingDetail}      ← BriefingDetailGetTask</li>
 *   <li>{@link #getPlayerBaseData}     ← PlayerBaseDataTask</li>
 *   <li>{@link #getTowerData}          ← TowerDataDetailTask</li>
 *   <li>{@link #getNewTowerData}       ← NewTowerDataDetailTask</li>
 *   <li>{@link #getSlashData}          ← SlashDataDetailTask</li>
 *   <li>{@link #refreshCalculatorData} ← CalculatorDataRefreshTask</li>
 *   <li>{@link #listCalculatorRole}    ← ListRoleTask</li>
 *   <li>{@link #listCalculatorWeapon}  ← ListWeaponTask</li>
 *   <li>{@link #queryOwnedRole}        ← QueryOwnedRoleTask</li>
 *   <li>{@link #getRoleCultivateStatus}← RoleCultivateStatusTask</li>
 *   <li>{@link #batchRoleCost}         ← BatchRoleCostTask</li>
 *   <li>{@link #batchWeaponCost}       ← BatchWeaponCostTask</li>
 *   <li>{@link #sendSmsCode}           ← SendSmsTask</li>
 *   <li>{@link #loginUser}             ← LoginUserTask</li>
 * </ul>
 *
 * @author Leck
 */
public class KujiequManager {
    private final KujiequRoleApi roleApi;
    private final KujiequUserApi userApi;
    private final KujiequTowerApi towerApi;
    private final KujiequCalculatorApi calculatorApi;
    private final KujiequSignApi signApi;
    private final KujiequAuthApi authApi;

    public KujiequManager(KujiequApiContext ctx) {
        this.roleApi = new KujiequRoleApi(ctx);
        this.userApi = new KujiequUserApi(ctx);
        this.towerApi = new KujiequTowerApi(ctx);
        this.calculatorApi = new KujiequCalculatorApi(ctx);
        this.signApi = new KujiequSignApi(ctx);
        this.authApi = new KujiequAuthApi(ctx);
    }

    // ==================== 角色数据（委托 roleApi） ====================

    public ResponseBody<List<Role>> getGameRoleData(UserInfo userInfo) {
        return roleApi.getGameRoleData(userInfo);
    }

    public ResponseBody<RoleDetail> getGameRoleDetail(UserInfo userInfo, int cardRoleId) {
        return roleApi.getGameRoleDetail(userInfo, cardRoleId);
    }

    public ResponseBody<List<UserInfo>> seekGameRole(String token, boolean isWeb) {
        return roleApi.seekGameRole(token, isWeb);
    }

    // ==================== 用户数据（委托 userApi） ====================

    public ResponseBody<String> refreshUserData(UserInfo userInfo) {
        return userApi.refreshUserData(userInfo);
    }

    public ResponseBody<RoleDailyData> getUserDailyData(UserInfo userInfo) {
        return userApi.getUserDailyData(userInfo);
    }

    public ResponseBody<RoleInfo> getPlayerBaseData(UserInfo userInfo) {
        return userApi.getPlayerBaseData(userInfo);
    }

    // ==================== 塔/矩阵/海墟（委托 towerApi） ====================

    public ResponseBody<DifficultyTotal> getTowerData(UserInfo userInfo) {
        return towerApi.getTowerData(userInfo);
    }

    public ResponseBody<NewTowerData> getNewTowerData(UserInfo userInfo) {
        return towerApi.getNewTowerData(userInfo);
    }

    public ResponseBody<SlashData> getSlashData(UserInfo userInfo) {
        return towerApi.getSlashData(userInfo);
    }

    // ==================== 养成计算器（委托 calculatorApi） ====================

    public ResponseBody<String> refreshCalculatorData(UserInfo userInfo) {
        return calculatorApi.refreshCalculatorData(userInfo);
    }

    public ResponseBody<List<RoleForCalculator>> listCalculatorRole(UserInfo userInfo) {
        return calculatorApi.listCalculatorRole(userInfo);
    }

    public ResponseBody<List<WeaponForCalculator>> listCalculatorWeapon(UserInfo userInfo) {
        return calculatorApi.listCalculatorWeapon(userInfo);
    }

    public ResponseBody<List<Integer>> queryOwnedRole(UserInfo userInfo) {
        return calculatorApi.queryOwnedRole(userInfo);
    }

    public ResponseBody<List<ExistedRoleDataForCalculator>> getRoleCultivateStatus(UserInfo userInfo, Integer... ids) {
        return calculatorApi.getRoleCultivateStatus(userInfo, ids);
    }

    public ResponseBody<CalculatorResult> batchRoleCost(UserInfo userInfo, RoleAim... roles) {
        return calculatorApi.batchRoleCost(userInfo, roles);
    }

    public ResponseBody<CalculatorResult> batchWeaponCost(UserInfo userInfo, WeaponAim... weapons) {
        return calculatorApi.batchWeaponCost(userInfo, weapons);
    }

    // ==================== 签到/资源简报（委托 signApi） ====================

    public ResponseBody<KujiequSignApi.SignGoodsResult> getSignGoods(UserInfo userInfo) {
        return signApi.getSignGoods(userInfo);
    }

    public ResponseBody<Briefing> getBriefingList(UserInfo userInfo) {
        return signApi.getBriefingList(userInfo);
    }

    public ResponseBody<Record> getBriefingDetail(UserInfo userInfo, String period, KujiequSignApi.BriefingType type) {
        return signApi.getBriefingDetail(userInfo, period, type);
    }

    // ==================== 认证/短信/token（委托 authApi） ====================

    public ResponseBody<Boolean> sendSmsCode(String phone, String geeTestJson) {
        return authApi.sendSmsCode(phone, geeTestJson);
    }

    public ResponseBody<UserInfo> loginUser(String phone, String code, boolean isWeb) {
        return authApi.loginUser(phone, code, isWeb);
    }

    public boolean prefetchAccessToken(UserInfo userInfo) {
        return authApi.prefetchAccessToken(userInfo);
    }

    public boolean isAccessTokenCached(String userId) {
        return authApi.isAccessTokenCached(userId);
    }

    public void invalidateAccessToken(String userId) {
        authApi.invalidateAccessToken(userId);
    }

    public void invalidateAllAccessTokens() {
        authApi.invalidateAllAccessTokens();
    }

    public void notifyTokenExpired(UserInfo userInfo, String msg) {
        authApi.notifyTokenExpired(userInfo, msg);
    }

    // ==================== 静态方法向后兼容委托（原 KujiequManager 静态方法） ====================

    /**
     * 生成 32 位随机 devCode（委托 {@link KujiequAuthApi#randomDevCode()}）。
     */
    public static String randomDevCode() {
        return KujiequAuthApi.randomDevCode();
    }

    /**
     * 校验中国大陆手机号格式（委托 {@link KujiequAuthApi#isValidCnMobile(String)}）。
     */
    public static boolean isValidCnMobile(String mobile) {
        return KujiequAuthApi.isValidCnMobile(mobile);
    }
}
