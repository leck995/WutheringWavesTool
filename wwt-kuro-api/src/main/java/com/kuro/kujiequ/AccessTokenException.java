package com.kuro.kujiequ;

/**
 * @program: WutheringWavesTool
 * @description: 用户B-AT过期
 * @author: Leck
 * @create: 2024-09-28 08:39
 */
public class AccessTokenException extends Exception {
    public AccessTokenException() {
        super("用户Bat获取失败或登录状态过期，请重新尝试或重新登录");
    }

    public AccessTokenException(String message) {
        super(message);
    }
}