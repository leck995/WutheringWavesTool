package cn.tealc.wutheringwavestool.model.webkujiequ;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 库街区 H5 登录/角色字段（对齐 web-kujiequ auth）。
 * 空白字段在 {@link #merge(AuthConfig)} 时保留原值。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthConfig {
    public String token = "";
    public String did = "";
    public String userId = "";
    public String roleId = "";
    public String serverId = "";
    public String channelId = "19";
    public String requestIp = "";
    public String gameId = "3";
    public String url = "";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public AuthConfig() {
    }

    public AuthConfig(String token, String did, String userId, String roleId, String serverId) {
        this.token = nullToEmpty(token);
        this.did = nullToEmpty(did);
        this.userId = nullToEmpty(userId);
        this.roleId = nullToEmpty(roleId);
        this.serverId = nullToEmpty(serverId);
    }

    public static AuthConfig load(Path path) throws IOException {
        if (path == null || !Files.isRegularFile(path)) {
            return new AuthConfig();
        }
        AuthConfig loaded = MAPPER.readValue(Files.readString(path), AuthConfig.class);
        return loaded.normalize();
    }

    public AuthConfig copy() {
        AuthConfig c = new AuthConfig();
        c.token = nullToEmpty(token);
        c.did = nullToEmpty(did);
        c.userId = nullToEmpty(userId);
        c.roleId = nullToEmpty(roleId);
        c.serverId = nullToEmpty(serverId);
        c.channelId = emptyDefault(channelId, "19");
        c.requestIp = nullToEmpty(requestIp);
        c.gameId = emptyDefault(gameId, "3");
        c.url = nullToEmpty(url);
        return c;
    }

    public AuthConfig normalize() {
        token = nullToEmpty(token);
        did = nullToEmpty(did);
        userId = nullToEmpty(userId);
        roleId = nullToEmpty(roleId);
        serverId = nullToEmpty(serverId);
        channelId = emptyDefault(channelId, "19");
        requestIp = nullToEmpty(requestIp);
        gameId = emptyDefault(gameId, "3");
        url = nullToEmpty(url);
        return this;
    }

    /** 非空 patch 字段覆盖当前值（空白保留）。 */
    public AuthConfig merge(AuthConfig patch) {
        if (patch == null) {
            return this;
        }
        if (notBlank(patch.token)) {
            token = patch.token.trim();
        }
        if (notBlank(patch.did)) {
            did = patch.did.trim();
        }
        if (notBlank(patch.userId)) {
            userId = patch.userId.trim();
        }
        if (notBlank(patch.roleId)) {
            roleId = patch.roleId.trim();
        }
        if (notBlank(patch.serverId)) {
            serverId = patch.serverId.trim();
        }
        if (notBlank(patch.channelId)) {
            channelId = patch.channelId.trim();
        }
        if (notBlank(patch.requestIp)) {
            requestIp = patch.requestIp.trim();
        }
        if (notBlank(patch.gameId)) {
            gameId = patch.gameId.trim();
        }
        if (notBlank(patch.url)) {
            url = patch.url.trim();
        }
        return normalize();
    }

    public String toAuthJson() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("token", nullToEmpty(token));
        m.put("did", nullToEmpty(did));
        m.put("userId", nullToEmpty(userId));
        m.put("roleId", nullToEmpty(roleId));
        m.put("serverId", nullToEmpty(serverId));
        m.put("channelId", emptyDefault(channelId, "19"));
        m.put("requestIp", nullToEmpty(requestIp));
        m.put("gameId", emptyDefault(gameId, "3"));
        try {
            return MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("serialize auth failed", e);
        }
    }

    public boolean hasCredentials() {
        return notBlank(token) && notBlank(did);
    }

    public AuthConfig token(String token) {
        this.token = nullToEmpty(token);
        return this;
    }

    public AuthConfig did(String did) {
        this.did = nullToEmpty(did);
        return this;
    }

    public AuthConfig userId(String userId) {
        this.userId = nullToEmpty(userId);
        return this;
    }

    public AuthConfig roleId(String roleId) {
        this.roleId = nullToEmpty(roleId);
        return this;
    }

    public AuthConfig serverId(String serverId) {
        this.serverId = nullToEmpty(serverId);
        return this;
    }

    public AuthConfig channelId(String channelId) {
        this.channelId = emptyDefault(channelId, "19");
        return this;
    }

    public AuthConfig requestIp(String requestIp) {
        this.requestIp = nullToEmpty(requestIp);
        return this;
    }

    public AuthConfig gameId(String gameId) {
        this.gameId = emptyDefault(gameId, "3");
        return this;
    }

    public AuthConfig url(String url) {
        this.url = nullToEmpty(url);
        return this;
    }

    public String getToken() {
        return token;
    }

    public String getDid() {
        return did;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public String getServerId() {
        return serverId;
    }

    public String getChannelId() {
        return emptyDefault(channelId, "19");
    }

    public String getGameId() {
        return emptyDefault(gameId, "3");
    }

    @Override
    public String toString() {
        return "AuthConfig{tokenLen=" + nullToEmpty(token).length()
                + ", didLen=" + nullToEmpty(did).length()
                + ", userId=" + emptyDash(userId)
                + ", roleId=" + emptyDash(roleId)
                + ", serverId=" + emptyDash(serverId)
                + ", gameId=" + getGameId()
                + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AuthConfig that)) {
            return false;
        }
        return Objects.equals(nullToEmpty(token), nullToEmpty(that.token))
                && Objects.equals(nullToEmpty(did), nullToEmpty(that.did))
                && Objects.equals(nullToEmpty(userId), nullToEmpty(that.userId))
                && Objects.equals(nullToEmpty(roleId), nullToEmpty(that.roleId))
                && Objects.equals(nullToEmpty(serverId), nullToEmpty(that.serverId))
                && Objects.equals(getChannelId(), that.getChannelId())
                && Objects.equals(nullToEmpty(requestIp), nullToEmpty(that.requestIp))
                && Objects.equals(getGameId(), that.getGameId())
                && Objects.equals(nullToEmpty(url), nullToEmpty(that.url));
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                nullToEmpty(token),
                nullToEmpty(did),
                nullToEmpty(userId),
                nullToEmpty(roleId),
                nullToEmpty(serverId),
                getChannelId(),
                nullToEmpty(requestIp),
                getGameId(),
                nullToEmpty(url));
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String emptyDefault(String s, String def) {
        String v = nullToEmpty(s);
        return v.isBlank() ? def : v;
    }

    private static String emptyDash(String s) {
        return s == null || s.isBlank() ? "-" : s;
    }
}
