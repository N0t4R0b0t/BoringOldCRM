/*
 * BoringOldCRM - Open-source multi-tenant CRM
 * Copyright (C) 2026 Ricardo Salvador
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later
 * Source: https://github.com/N0t4R0b0t/BoringOldCRM
 */
package com.bocrm.backend.config;

import tools.jackson.databind.ObjectMapper;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;
import org.springframework.stereotype.Component;

/**
 * Hibernate strategy provider for custom JSON format mapping using tools.jackson.databind
 * Hibernate instantiates this with a no-args constructor, so we use ObjectMapperHolder for access
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
@Component
public class JacksonConfigHibernateFormatMapperProvider implements FormatMapper {

    // No-args constructor for Hibernate
    public JacksonConfigHibernateFormatMapperProvider() {
    }

    private ObjectMapper getObjectMapper() {
        ObjectMapper mapper = ObjectMapperHolder.getObjectMapper();
        if (mapper == null) {
            throw new IllegalStateException("ObjectMapper has not been initialized yet");
        }
        return mapper;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        ObjectMapper mapper = getObjectMapper();
        String typeName = javaType.getJavaType().getTypeName();
        try {
            if (charSequence == null) return null;

            // If the target type is String, return the raw JSON string directly
            if (String.class.equals(javaType.getJavaType())) {
                return (T) charSequence.toString();
            }

            tools.jackson.databind.JavaType targetType = mapper.getTypeFactory().constructType(javaType.getJavaType());
            return mapper.readValue(charSequence.toString(), targetType);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize JSON to " + typeName + " with Jackson. Content: " + charSequence, e);
        }
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        ObjectMapper mapper = getObjectMapper();
        try {
            if (value == null) return null;
            // If the value is already a String, return it directly (it's already JSON)
            if (value instanceof String) {
                return (String) value;
            }
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JSON with Jackson", e);
        }
    }
}
