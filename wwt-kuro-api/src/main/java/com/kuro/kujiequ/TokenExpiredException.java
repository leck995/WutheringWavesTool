package com.kuro.kujiequ;

/**
 * @program: WutheringWavesTool
 * @description: 用户主 Token 过期（非 B-At），Kuro API 返回"登录已过期，请重新登录"时抛出
 * @author: Leck
 * @create: 2026-06-21 10:00
 */
public class TokenExpiredException extends Exception {
    public TokenExpiredException() {
        super("Kuro 账号 Token 已过期，请重新登录");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
