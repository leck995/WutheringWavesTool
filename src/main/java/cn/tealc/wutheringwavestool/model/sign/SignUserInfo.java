package cn.tealc.wutheringwavestool.model.sign;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-06 20:19
 */
public record SignUserInfo(String userId,String roleId,String token) {

    @Override
    public String toString() {
        return String.format("用户ID: %s \n游戏ID: %s", userId, roleId);
    }
}