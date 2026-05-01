package com.chenhaonee.agents.common.validator;

import org.apache.commons.lang3.StringUtils;

/**
 * 领域实体参数校验工具。
 */
public final class ValidationUtils {

    private ValidationUtils() {
    }

    /**
     * 校验文本参数非空（trim 后）。
     */
    public static String requireText(String value, String fieldName) {
        String safeValue = StringUtils.trimToNull(value);
        if (safeValue == null) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return safeValue;
    }
}
