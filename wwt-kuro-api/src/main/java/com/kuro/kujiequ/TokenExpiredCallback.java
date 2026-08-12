package com.kuro.kujiequ;

import com.kuro.kujiequ.model.sign.UserInfo;

/**
 * token 过期回调，由 app 侧实现（通常转发到 NotificationManager）。
 *
 * @author Leck
 */
@FunctionalInterface
public interface TokenExpiredCallback {
    void onTokenExpired(UserInfo userInfo, String msg);
}
