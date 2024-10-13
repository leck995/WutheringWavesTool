package cn.tealc.wutheringwavestool.util;

import cn.tealc.wutheringwavestool.base.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @program: WutheringWavesTool
 * @description: 对库街区接口数据进行解密
 * @author: Leck
 * @create: 2024-09-28 07:06
 */
public class ApiUtil {
    private static final Logger LOG = LoggerFactory.getLogger(ApiUtil.class);
    public static String decrypt(String value) throws ApiDecryptException {
        String keyBase64 = Config.apiDecryptKey;
        byte[] key = Base64.getDecoder().decode(keyBase64);
        byte[] encryptedData = Base64.getDecoder().decode(value);
        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedData = cipher.doFinal(encryptedData);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException | IllegalBlockSizeException |
                 BadPaddingException e) {
            LOG.error("数据解密出现错误，原内容{}",value);
            LOG.error(e.getMessage(),e);
            return null;
        }
    }
}