package legacy;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;

public class GzipTest {

    public static void main(String[] args) {
        String urlString = "https://aki-config-huoshan.aki-game.com/QRTreox4svKNqe3LDmHrLkOJNAOx8ORw/index.json"; // Replace with your actual URL

        try {
            // 1. Set up the HTTP request
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // 2. Set the "Accept-Encoding" header
            // This tells the server you can handle gzip compression.
            connection.setRequestProperty("Accept-Encoding", "gzip");
            connection.setRequestMethod("GET");

            // 3. Get the response and check for gzip
            // The "Content-Encoding" header will tell you if the server
            // actually compressed the response.
            String contentEncoding = connection.getHeaderField("Content-Encoding");

            BufferedReader reader;

            // If the content is compressed, use GZIPInputStream
            if (contentEncoding != null && contentEncoding.equalsIgnoreCase("gzip")) {
                System.out.println("Response is gzip compressed. Decompressing...");
                GZIPInputStream gis = new GZIPInputStream(connection.getInputStream());
                reader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
            } else {
                System.out.println("Response is not compressed.");
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            }

            // 4. Read the decompressed content
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // 5. Display the original text
            System.out.println("Original Text:");
            System.out.println(response.toString());




            try {

                // Step 1: Base64 解码
                byte[] gzipCompressedData = Base64.getDecoder().decode(response.toString());





                String string = new String(gzipCompressedData, StandardCharsets.UTF_8);
                System.out.println(string);

                // Step 2: Gzip 解压
                ByteArrayInputStream bis = new ByteArrayInputStream(gzipCompressedData);
                GZIPInputStream gis = new GZIPInputStream(bis);

                // Step 3: 读取解压后的数据并转换为字符串
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }

                // 获取解压后的原始文本（假设为 UTF-8 编码）
                String originalText = bos.toString("UTF-8");

                // 关闭流
                gis.close();
                bis.close();
                bos.close();

                // Step 4: 打印结果
                System.out.println("\n--- 解压后的原始 JSON 内容 ---");
                System.out.println(originalText);

            } catch (IOException e) {
                System.err.println("解压失败，请检查数据格式是否正确。");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println("Base64 解码失败，输入的字符串格式不正确。");
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
