package cn.tealc.wutheringwavestool.service;

import cn.tealc.wutheringwavestool.dao.ConfigDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class ConfigService {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
    private final ConfigDao dao;
    private final ObjectMapper mapper;

    @Inject
    public ConfigService(ConfigDao dao, ObjectMapper mapper) {
        this.dao = dao;
        this.mapper = mapper;
    }

    // ========== 原始 String ==========

    public Optional<String> get(String key) {
        return Optional.ofNullable(dao.getValue(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    public void set(String key, String value) {
        dao.saveOrUpdate(key, value);
    }

    // ========== int ==========

    public Optional<Integer> getInt(String key) {
        return get(key).map(Integer::parseInt);
    }

    public int getIntOrDefault(String key, int defaultValue) {
        return getInt(key).orElse(defaultValue);
    }

    public void set(String key, int value) {
        set(key, String.valueOf(value));
    }

    // ========== long ==========

    public Optional<Long> getLong(String key) {
        return get(key).map(Long::parseLong);
    }

    public long getLongOrDefault(String key, long defaultValue) {
        return getLong(key).orElse(defaultValue);
    }

    public void set(String key, long value) {
        set(key, String.valueOf(value));
    }

    // ========== double ==========

    public Optional<Double> getDouble(String key) {
        return get(key).map(Double::parseDouble);
    }

    public double getDoubleOrDefault(String key, double defaultValue) {
        return getDouble(key).orElse(defaultValue);
    }

    public void set(String key, double value) {
        set(key, String.valueOf(value));
    }

    // ========== boolean ==========

    public Optional<Boolean> getBoolean(String key) {
        return get(key).map(Boolean::parseBoolean);
    }

    public boolean getBooleanOrDefault(String key, boolean defaultValue) {
        return getBoolean(key).orElse(defaultValue);
    }

    public void set(String key, boolean value) {
        set(key, String.valueOf(value));
    }

    // ========== JSON 对象 ==========

    /** 读取 JSON 并反序列化为指定类型 */
    public <T> Optional<T> getObject(String key, Class<T> type) {
        return get(key).map(json -> {
            try {
                return mapper.readValue(json, type);
            } catch (JsonProcessingException e) {
                LOG.error("反序列化配置失败, key: {}, type: {}", key, type.getSimpleName(), e);
                return null;
            }
        });
    }

    /** 读取 JSON 并反序列化为泛型类型（如 List<Item>） */
    public <T> Optional<T> getObject(String key, TypeReference<T> typeRef) {
        return get(key).map(json -> {
            try {
                return mapper.readValue(json, typeRef);
            } catch (JsonProcessingException e) {
                LOG.error("反序列化配置失败, key: {}", key, e);
                return null;
            }
        });
    }

    public <T> T getObjectOrDefault(String key, T defaultValue, Class<T> type) {
        return this.<T>getObject(key, type).orElse(defaultValue);
    }

    public <T> T getObjectOrDefault(String key, T defaultValue, TypeReference<T> typeRef) {
        return this.<T>getObject(key, typeRef).orElse(defaultValue);
    }

    /** 将对象序列化为 JSON 存储 */
    public <T> void setObject(String key, T value) {
        try {
            String json = mapper.writeValueAsString(value);
            LOG.info("保存配置对象, key: {}, type: {}", key, value.getClass().getSimpleName());
            set(key, json);
        } catch (JsonProcessingException e) {
            LOG.error("序列化对象失败, key: {}", key, e);
        }
    }

    // ========== 删除 ==========

    public int delete(String key) {
        LOG.info("删除配置, key: {}", key);
        return dao.delete(key);
    }
}
