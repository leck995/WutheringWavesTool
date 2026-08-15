package cn.tealc.wwt.game.resource.internal.legacy.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.List;

/** JSON compatibility helpers backed exclusively by Jackson. */
public final class JsonUtils {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private JsonUtils() {
    }

    public static <T> T deserialize(String json, Class<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return read(json, MAPPER.constructType(type));
    }

    public static <T> T deserialize(String json, Type type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return read(json, MAPPER.constructType(type));
    }

    public static <T> List<T> deserializeList(String json, Class<T> elementType) {
        if (json == null || json.isBlank()) {
            return null;
        }
        JavaType listType = MAPPER.getTypeFactory()
                .constructCollectionType(List.class, elementType);
        return read(json, listType);
    }

    public static <T> T safeDeserialize(String json, Class<T> type) {
        try {
            return deserialize(json, type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static <T> T safeDeserialize(String json, Type type) {
        try {
            return deserialize(json, type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static <T> List<T> safeDeserializeList(String json, Class<T> elementType) {
        try {
            return deserializeList(json, elementType);
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String safeSerialize(Object value) {
        try {
            return serialize(value);
        } catch (RuntimeException e) {
            return "";
        }
    }

    public static String serialize(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize JSON", e);
        }
    }

    private static <T> T read(String json, JavaType type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to parse JSON", e);
        }
    }
}
