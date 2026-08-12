package com.kuro.util;

import com.kuro.kujiequ.model.sign.UserInfo;

import java.net.URI;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

/**
 * @program: WutheringWavesTool
 * @description:
 * @author: Leck
 * @create: 2024-07-10 19:29
 */
public class HttpRequestUtil {
    public static HttpRequest getRequestWithSource(String url, String body, String token, boolean source, String devCode) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0")
                .header("Accept", "application/json, text/plain, */*")
                .header("pragma", "no-cache")
                .header("cache-control", "no-cache")
                .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Android WebView\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                .header("source", source ? "h5" : "android")
                .header("devcode", devCode)
                .header("sec-ch-ua-platform", "\"Android\"")
                .header("origin", "https://web-static.kurobbs.com")
                .header("x-requested-with", "com.kurogame.kjq")
                .header("sec-fetch-site", "same-site")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("priority", "u=1, i");
        if (token != null) {
            builder.header("token", token);
        }
        if (body != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        }else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return builder.build();
    }

    public static HttpRequest getRequestWithSource(String url, boolean source, String devCodeRaw) {
        return getRequestWithSource(url, null, null, source, devCodeRaw);
    }

    public static HttpRequest getRequestWithSource(String url, String token, boolean source, String devCodeRaw) {
        return getRequestWithSource(url, null, token, source, devCodeRaw);
    }



    public static HttpRequest.Builder getBuilder(String url, String body, String token, boolean source, String devCode) {
        HttpRequest.Builder builder = HttpRequest.newBuilder();
        builder.uri(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; 23116PN5BC Build/PQ3A.190605.02201427; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/124.0.6367.82 Mobile Safari/537.36 Kuro/2.5.0 KuroGameBox/2.5.0")
                .header("Accept", "application/json, text/plain, */*")
                .header("pragma", "no-cache")
                .header("cache-control", "no-cache")
                .header("sec-ch-ua", "\"Chromium\";v=\"124\", \"Android WebView\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                .header("source", source ? "h5" : "android")
                .header("devcode", devCode)
                .header("sec-ch-ua-platform", "\"Android\"")
                .header("origin", "https://web-static.kurobbs.com")
                .header("x-requested-with", "com.kurogame.kjq")
                .header("sec-fetch-site", "same-site")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("accept-language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("priority", "u=1, i");
        if (token != null) {
            builder.header("token", token);
        }
        if (body != null) {
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        }else {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        }
        return builder;
    }


    public static HttpRequest.Builder getBuilder(String url, String token, boolean source, String devCodeRaw) {
        return getBuilder(url, null, token, source, devCodeRaw);
    }



}