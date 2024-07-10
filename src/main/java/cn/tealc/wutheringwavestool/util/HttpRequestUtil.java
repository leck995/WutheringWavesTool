package cn.tealc.wutheringwavestool.util;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-10 19:29
 */
public class HttpRequestUtil {
    public static HttpRequest getRequest(String url,String token) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36 Edg/126.0.0.0")
                .header("Accept", "application/json, text/plain, */*")
                .header("pragma", "no-cache")
                .header("cache-control", "no-cache")
                .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Android WebView\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                .header("source", "h5")
                .header("devcode", "111.181.85.154, Mozilla/5.0 (Linux; Android 14; 22081212C Build/UKQ1.230917.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.179 Mobile Safari/537.36 Kuro/2.2.0 KuroGameBox/2.2.0")
                .header("token", token)
                .header("sec-ch-ua-platform", "\"Android\"")
                .header("origin", "https://web-static.kurobbs.com")
                .header("x-requested-with", "com.kurogame.kjq")
                .header("sec-fetch-site", "same-site")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("priority", "u=1, i")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
    }
}