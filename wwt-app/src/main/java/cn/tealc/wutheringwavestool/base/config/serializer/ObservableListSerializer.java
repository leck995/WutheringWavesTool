package cn.tealc.wutheringwavestool.base.config.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import javafx.collections.ObservableList;

import java.io.IOException;

/**
 * Jackson 序列化器：将 ObservableList&lt;String&gt; 序列化为 JSON 数组。
 * <p>从 Setting 内部类抽出，供分组配置类复用。
 */
public class ObservableListSerializer extends JsonSerializer<ObservableList<String>> {
    @Override
    public void serialize(ObservableList<String> value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartArray();
        for (String item : value) {
            gen.writeString(item);
        }
        gen.writeEndArray();
    }
}
