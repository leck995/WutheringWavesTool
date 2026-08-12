package com.kuro.kujiequ;

import java.security.GeneralSecurityException;

/**
 * @program: WutheringWavesTool
 * @description: 解密异常，对库街区接口数据进行解密，官方不在进行加密，故废弃
 * @author: Leck
 * @create: 2024-09-28 08:39
 */
@Deprecated
public class ApiDecryptException extends GeneralSecurityException {
    public ApiDecryptException() {
        super();
    }

    public ApiDecryptException(String message) {
        super(message);
    }
}