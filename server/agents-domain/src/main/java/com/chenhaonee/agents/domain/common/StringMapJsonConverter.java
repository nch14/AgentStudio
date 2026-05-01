package com.chenhaonee.agents.domain.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Map;

/**
 * JPA Converter for Map&lt;String, String&gt; &lt;-&gt; JSON string.
 */
@Converter
public class StringMapJsonConverter implements AttributeConverter<Map<String, String>, String> {

    @Override
    public String convertToDatabaseColumn(Map<String, String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(attribute);
    }

    @Override
    public Map<String, String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        return JSON.parseObject(dbData, new TypeReference<Map<String, String>>() {});
    }
}
