package com.kr.launcher.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;

public class JsonUtils {
    private static final Gson gson = new GsonBuilder()
            .setLenient()
            .create();

    private static final Pattern TRAILING_COMMA = Pattern.compile(",\\s*([}\\]])");

    /**
     * Deserialize JSON, throwing on parse error.
     * Corresponds to C# JsonUtils.Deserialize (no try/catch).
     * Returns null for null/empty input.
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        if (json == null || json.isEmpty())
            return null;
        String cleaned = TRAILING_COMMA.matcher(json).replaceAll("$1");
        return gson.fromJson(cleaned, clazz);
    }

    /**
     * Deserialize JSON with a Type, throwing on parse error.
     */
    public static <T> T deserialize(String json, Type type) {
        if (json == null || json.isEmpty())
            return null;
        String cleaned = TRAILING_COMMA.matcher(json).replaceAll("$1");
        return gson.fromJson(cleaned, type);
    }

    /**
     * Deserialize a JSON array into a List, throwing on parse error.
     */
    public static <T> List<T> deserializeList(String json, Class<T> elementClass) {
        if (json == null || json.isEmpty())
            return null;
        String cleaned = TRAILING_COMMA.matcher(json).replaceAll("$1");
        Type type = TypeToken.getParameterized(List.class, elementClass).getType();
        return gson.fromJson(cleaned, type);
    }

    /**
     * Safe deserialize that never throws, returns null on error.
     * Corresponds to C# JsonUtils.SafeDeserialize.
     */
    public static <T> T safeDeserialize(String json, Class<T> clazz) {
        try {
            return deserialize(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safe deserialize with a Type, never throws, returns null on error.
     */
    public static <T> T safeDeserialize(String json, Type type) {
        try {
            return deserialize(json, type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safe deserialize a JSON array into a List, never throws, returns null on
     * error.
     */
    public static <T> List<T> safeDeserializeList(String json, Class<T> elementClass) {
        try {
            return deserializeList(json, elementClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Safe serialize that never throws. Corresponds to C# JsonUtils.SafeSerialize.
     * C# returns "" on failure (not null).
     */
    public static String safeSerialize(Object obj) {
        try {
            return gson.toJson(obj);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Serialize an object to JSON. Throws on failure (matching C#
     * JsonSerializer.Serialize which has no try/catch). Callers should wrap in
     * try/catch or use {@link #safeSerialize} for a null-safe variant.
     */
    public static String serialize(Object obj) {
        return gson.toJson(obj);
    }
}
