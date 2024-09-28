package cn.tealc.wutheringwavestool.util;

import java.security.GeneralSecurityException;

/**
 * @program: WutheringWavesTool
 * @description: 解密异常
 * @author: Leck
 * @create: 2024-09-28 08:39
 */
public class ApiDecryptException extends GeneralSecurityException {
    public ApiDecryptException() {
        super();
    }

    public ApiDecryptException(String message) {
        super(message);
    }
}