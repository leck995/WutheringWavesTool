package com.kuro.kujiequ;

import java.security.GeneralSecurityException;

/**
 * @program: WutheringWavesTool
 * @description: 用户B-AT过期
 * @author: Leck
 * @create: 2024-09-28 08:39
 */
public class AccessTokenException extends Exception {
    public AccessTokenException() {
        super("用户Bat获取失败失败或过期");
    }

    public AccessTokenException(String message) {
        super(message);
    }
}