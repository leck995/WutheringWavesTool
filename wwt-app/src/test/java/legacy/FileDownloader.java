package legacy;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

public class FileDownloader {
    public static void main(String[] args) throws IOException {
        downloadFile("https://prod-cn-alicdn-gamestarter.kurogame.com/pcstarter/prod/game/G152/10003_Y8xXrXk65DqFHEDgApn3cpK5lfczpFx5/index.json","C:\\Leck\\Code\\Reposity\\Java-Other\\WutheringWavesTool");
    }
    public static void downloadFile(String fileURL, String saveDir) throws IOException {
        URL url = new URL(fileURL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        
        // 设置请求头
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0");
        httpConn.setRequestProperty("Accept-Encoding", "gzip"); // 添加 Accept-Encoding 头

        // 处理重定向
        int responseCode = httpConn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStream;
            String encoding = httpConn.getContentEncoding();

            // 根据 Content-Encoding 处理输入流
            if ("gzip".equalsIgnoreCase(encoding)) {
                inputStream = new GZIPInputStream(httpConn.getInputStream());
            } else {
                inputStream = httpConn.getInputStream();
            }

            String saveFilePath = saveDir + File.separator + "downloaded_file.json"; // 保存为 JSON 文件

            FileOutputStream outputStream = new FileOutputStream(saveFilePath);

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.close();
            inputStream.close();

            System.out.println("文件已下载到: " + saveFilePath);
        } else {
            System.out.println("没有文件可下载，响应代码: " + responseCode);
        }
        httpConn.disconnect();
    }
}
