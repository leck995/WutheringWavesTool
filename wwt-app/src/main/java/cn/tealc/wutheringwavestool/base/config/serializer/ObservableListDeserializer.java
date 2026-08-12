package cn.tealc.wutheringwavestool.base.config.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.util.List;

/**
 * Jackson 反序列化器：将 JSON 数组反序列化为 ObservableList&lt;String&gt;。
 * <p>从 Setting 内部类抽出，供分组配置类复用。
 */
public class ObservableListDeserializer extends JsonDeserializer<ObservableList<String>> {
    @Override
    public ObservableList<String> deserialize(JsonParser p, DeserializationContext ctxt)
            throws IOException {
        List<String> list = p.readValueAs(List.class);
        return FXCollections.observableArrayList(list);
    }
}
