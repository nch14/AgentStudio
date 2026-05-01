package com.chenhaonee.agents.domain.coordination.model;

import com.alibaba.fastjson2.JSON;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class AnswerListConverter implements AttributeConverter<List<Answer>, String> {

    @Override
    public String convertToDatabaseColumn(List<Answer> attribute) {
        if (attribute == null) {
            return null;
        }
        return JSON.toJSONString(attribute);
    }

    @Override
    public List<Answer> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(dbData, Answer.class);
    }
}
