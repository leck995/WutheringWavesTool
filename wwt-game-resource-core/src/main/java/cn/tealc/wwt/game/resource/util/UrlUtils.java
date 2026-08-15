package cn.tealc.wwt.game.resource.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * URL 工具。
 */
public final class UrlUtils {
    private UrlUtils() {}

    /** 拼接 baseUrl 与相对路径，把路径中的空格转义为 %20，并避免重复斜杠。 */
    public static String join(String baseUrl, String relativePath) {
        String base = baseUrl == null ? "" : baseUrl;
        String rel = relativePath == null ? "" : relativePath;
        if (rel.startsWith("/") && base.endsWith("/")) {
            rel = rel.substring(1);
        } else if (!rel.startsWith("/") && !base.endsWith("/") && !base.isEmpty()) {
            rel = "/" + rel;
        }
        return base + rel.replace(" ", "%20");
    }

    public static String encodePath(String path) {
        return URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
    }
}