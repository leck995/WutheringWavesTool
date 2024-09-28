package test;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-09-28 07:01
 */
public class Api {
    public static Object decrypt(String value) throws Exception {
        String keyBase64 = "XSNLFgNCth8j8oJI3cNIdw==";
        byte[] key = Base64.getDecoder().decode(keyBase64);
        byte[] encryptedData = Base64.getDecoder().decode(value);

        SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        String plaintext = new String(decryptedData);

        System.out.println(plaintext);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(plaintext, Object.class);
    }

    public static void main(String[] args) {
        try {
            String encryptedValue = "ejN03/bw3pFDKnYLkWMTJghYWRpto98JzHE2jXOAmblsfjDFqUxw38xMbOi4AP+VFS3nMFucVj6XX8F9J8S8dC3CwZBuAWVGSv95oXrui676xVt2LfFbUSOrfYLPj2+OW2hV/Xs3ruSU7dXVz0Mw1hZ5nF/tLh7kdX4FZrRb1Qrx1sihDdeBFFK/o8b3n4aSh4rOLPyjb65kHNt4nS951F7VVSeKJpEGi7u6fpgZhw6mvPZDtaE565TM+pdogfH0aVXdJHCmM6+uFhgPICgB4Vny6K+Rkd5Vv+XIgr/FyyAT8O52D4faPX5z/L57K38sjxwp/zsQjw7pDvUzHBiBmaXNowKI8vs9U/9McfFabM9YIevprtM+fu8P5xBmliBL6KWuSnh3DtQxxpfxecEKGxCFt19pdBT30CN/U/1QQzS3Gy/IAc1nSUW6RrTJQyvbBMzeuFpfNhGWXE5LvYH0/FqLktObRQSbqB08IwnNmHI9J6RLRUYqS3uR5kyw97CIQVWkdcA1Yu9G1MbNyYc3G9xdw+2QzrQG6wAvE4PHhy+nibfYpGzhJDeu/EckoAmFE8TWjeJt2zfOs6XS47IfWpskWJoR2bD7SXYdpuj5B7OKuZr/n+HnqhTPYwX2945WoaKKo/K5gicibqCF0OyPow=="; // 替换为实际的加密数据
            Object result = decrypt(encryptedValue);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}