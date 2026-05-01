package com.chenhaonee.agents.domain.coordination.model;

import com.alibaba.fastjson2.JSON;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class QuestionListConverter implements AttributeConverter<List<Question>, String> {

    @Override
    public String convertToDatabaseColumn(List<Question> attribute) {
        if (attribute == null) {
            return null;
        }
        return JSON.toJSONString(attribute);
    }

    @Override
    public List<Question> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        return JSON.parseArray(dbData, Question.class);
    }
}
