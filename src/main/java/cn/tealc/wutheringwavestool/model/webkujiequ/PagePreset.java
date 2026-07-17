package cn.tealc.wutheringwavestool.model.webkujiequ;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 内置库街区 H5 页面预设。
 */
public enum PagePreset {
    MC_ROLE_BOX(
            "mc-role-box",
            "数据终端",
            "https://web-static.kurobbs.com/mcbox/index.html#/mc-role-box",
            true),
    RESOURCE_BRIEFING(
            "resource-briefing",
            "资源简报",
            "https://web-static.kurobbs.com/resource-briefing/index.html#/home",
            false),
    MC_CALENDAR(
            "mccalendar",
            "活动日历",
            "https://web-static.kurobbs.com/mccalendar/index.html#/",
            false),
    GROWTH_CALCULATOR(
            "growth-calculator",
            "养成计算器",
            "https://web-static.kurobbs.com/growth-calculator/index.html#/",
            false),
    MC_MONTH_SIGN(
            "mc-month-sign",
            "每日签到",
            "https://web-static.kurobbs.com/events2.0/index.html#/mc-month-sign/home",
            false);

    private final String id;
    private final String title;
    private final String defaultUrl;
    private final boolean autoRoleQuery;

    PagePreset(String id, String title, String defaultUrl, boolean autoRoleQuery) {
        this.id = id;
        this.title = title;
        this.defaultUrl = defaultUrl;
        this.autoRoleQuery = autoRoleQuery;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    public String defaultUrl() {
        return defaultUrl;
    }

    public boolean autoRoleQuery() {
        return autoRoleQuery;
    }

    @Override
    public String toString() {
        return title + " (" + id + ")";
    }

    public static String[] allIds() {
        return Arrays.stream(values()).map(PagePreset::id).toArray(String[]::new);
    }

    public static Optional<PagePreset> parse(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String n = name.trim().toLowerCase();
        for (PagePreset p : values()) {
            if (p.id.equalsIgnoreCase(name.trim())) {
                return Optional.of(p);
            }
        }
        return switch (n) {
            case "mc", "mc-role-box", "mcbox", "role-box", "数据终端" -> Optional.of(MC_ROLE_BOX);
            case "resource", "resource-briefing", "briefing", "资源简报" -> Optional.of(RESOURCE_BRIEFING);
            case "calendar", "mccalendar", "mc-calendar", "活动日历", "鸣潮活动日历" ->
                    Optional.of(MC_CALENDAR);
            case "growth", "growth-calculator", "calculator", "养成", "养成计算器" ->
                    Optional.of(GROWTH_CALCULATOR);
            case "sign", "daily-sign", "month-sign", "mc-month-sign", "events", "events2",
                    "events2.0", "签到", "每日签到" -> Optional.of(MC_MONTH_SIGN);
            default -> Optional.empty();
        };
    }

    public static Optional<PagePreset> detectFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }
        String u = url.toLowerCase();
        if (u.contains("mc-role-box") || u.contains("/mcbox/")) {
            return Optional.of(MC_ROLE_BOX);
        }
        if (u.contains("resource-briefing")) {
            return Optional.of(RESOURCE_BRIEFING);
        }
        if (u.contains("mccalendar")) {
            return Optional.of(MC_CALENDAR);
        }
        if (u.contains("growth-calculator")) {
            return Optional.of(GROWTH_CALCULATOR);
        }
        if (u.contains("mc-month-sign") || u.contains("events2.0")) {
            return Optional.of(MC_MONTH_SIGN);
        }
        return Optional.empty();
    }

    /** 解析入口 URL；数据终端可自动拼 roleId/serverId。 */
    public static String resolveEntryUrl(String baseUrl, AuthConfig auth, PagePreset preset) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        if (base.isEmpty()) {
            base = MC_ROLE_BOX.defaultUrl;
            preset = MC_ROLE_BOX;
        }

        boolean wantsQuery = preset != null
                ? preset.autoRoleQuery
                : base.contains("mc-role-box") || base.contains("role-box");

        if (!wantsQuery || auth == null) {
            return base;
        }
        if (isBlank(auth.roleId) && isBlank(auth.serverId)) {
            return base;
        }

        int hash = base.indexOf('#');
        if (hash < 0) {
            return appendQuery(base, auth);
        }

        String before = base.substring(0, hash);
        String hashPart = base.substring(hash + 1);
        int q = hashPart.indexOf('?');
        String path = q >= 0 ? hashPart.substring(0, q) : hashPart;
        String existing = q >= 0 ? hashPart.substring(q + 1) : "";

        if (queryHas(existing, "roleId") || queryHas(existing, "serverId")) {
            return base;
        }

        Map<String, String> extra = new LinkedHashMap<>();
        extra.put("accessType", "1");
        if (!isBlank(auth.roleId)) {
            extra.put("roleId", auth.roleId);
        }
        if (!isBlank(auth.serverId)) {
            extra.put("serverId", auth.serverId);
        }

        StringBuilder qb = new StringBuilder(existing);
        for (Map.Entry<String, String> e : extra.entrySet()) {
            if (!qb.isEmpty()) {
                qb.append('&');
            }
            qb.append(e.getKey())
                    .append('=')
                    .append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return before + "#" + path + "?" + qb;
    }

    private static String appendQuery(String url, AuthConfig auth) {
        StringBuilder sb = new StringBuilder(url);
        sb.append(url.contains("?") ? '&' : '?');
        sb.append("accessType=1");
        if (!isBlank(auth.roleId)) {
            sb.append("&roleId=").append(URLEncoder.encode(auth.roleId, StandardCharsets.UTF_8));
        }
        if (!isBlank(auth.serverId)) {
            sb.append("&serverId=").append(URLEncoder.encode(auth.serverId, StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    private static boolean queryHas(String query, String key) {
        if (query == null || query.isEmpty()) {
            return false;
        }
        for (String part : query.split("&")) {
            if (part.equals(key) || part.startsWith(key + "=")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
