package com.kr.launcher.util;

import java.util.*;

public class UrlUtils {

    public static String encodeUrlPath(String url) {
        if (url == null)
            return null;
        int queryStart = url.indexOf('?');
        String path = queryStart >= 0 ? url.substring(0, queryStart) : url;
        String query = queryStart >= 0 ? url.substring(queryStart) : "";
        return path.replace(" ", "%20") + query;
    }

    public static String appendPath(String baseUrl, String path) {
        // C# UrlUtils.AppendPath: only checks path==null (returns baseUrl).
        // Does NOT special-case empty baseUrl or empty path. When baseUrl is
        // empty, result is "" + "/" + path = "/path" (with leading '/').
        if (path == null) {
            return baseUrl;
        }
        if (baseUrl == null) {
            // C# would throw NRE; be defensive
            return path;
        }
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String p = path.startsWith("/") ? path.substring(1) : path;
        return base + "/" + p;
    }

    public static String buildUrl(String url, Map<String, String> queries) {
        if (queries == null || queries.isEmpty())
            return url;
        StringBuilder sb = new StringBuilder(url);
        // C# UrlUtils.cs:42-48: if URL already ends with "&", append directly
        // without adding another "&" (prevents double "&").
        if (url.endsWith("&")) {
            // skip separator, just append
        } else {
            sb.append(url.contains("?") ? "&" : "?");
        }
        boolean first = true;
        for (Map.Entry<String, String> entry : queries.entrySet()) {
            if (!first)
                sb.append("&");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }
        return sb.toString();
    }
}
