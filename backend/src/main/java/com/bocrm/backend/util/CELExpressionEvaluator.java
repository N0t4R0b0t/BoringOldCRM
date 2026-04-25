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
package com.bocrm.backend.util;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.TypedValue;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.expression.Expression;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 * CELExpressionEvaluator.
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */

@Component
public class CELExpressionEvaluator {

    private static final Logger log = LoggerFactory.getLogger(CELExpressionEvaluator.class);

    private final ObjectMapper objectMapper;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    public CELExpressionEvaluator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Evaluate a SpEL expression against a context map.
     * Variables are accessible both as root map keys (e.g. {@code name}) and
     * as named variables (e.g. {@code #name}).
     * Expressions are admin-defined, so StandardEvaluationContext is used.
     * Supports common math functions: min, max, abs, round, floor, ceil.
     */
    public Object evaluate(String expression, Map<String, Object> context) {
        try {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            ctx.setRootObject(context);
            ctx.addPropertyAccessor(new MapPropertyAccessor());
            // Make math functions available as variables that can be called
            MathFunctionsWrapper mathFunctions = new MathFunctionsWrapper();
            context.forEach(ctx::setVariable);
            ctx.setVariable("math", mathFunctions);

            // Preprocess expression to inject math. prefix for function calls
            String processedExpression = preprocessMathFunctions(expression);

            Expression compiled = expressionCache.computeIfAbsent(processedExpression, parser::parseExpression);
            return compiled.getValue(ctx);
        } catch (Exception e) {
            log.error("SpEL evaluation error for expression '{}': {}", expression, e.getMessage());
            throw new RuntimeException("Expression evaluation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Preprocess expression to inject 'math.' prefix before known math function calls.
     * Converts: min(a, b) → math.min(a, b), max(x) → math.max(x), etc.
     * Only matches function calls (word followed by parenthesis), not variable names.
     */
    private String preprocessMathFunctions(String expression) {
        // List of math functions to recognize
        String[] mathFuncs = {"min", "max", "abs", "round", "floor", "ceil", "sqrt", "pow"};

        String result = expression;
        for (String func : mathFuncs) {
            // Match the function name followed by (, but only if not already prefixed with math.
            // Use word boundary and negative lookbehind to avoid double-prefixing
            Pattern pattern = Pattern.compile("(?<!\\.)\\b" + func + "\\s*\\(");
            Matcher matcher = pattern.matcher(result);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                // Check if already prefixed with "math."
                int start = matcher.start();
                if (start >= 5 && result.substring(start - 5, start).equals("math.")) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
                } else {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement("math." + matcher.group()));
                }
            }
            matcher.appendTail(sb);
            result = sb.toString();
        }
        return result;
    }

    /**
     * Build a typed context map from an entity JsonNode and custom fields.
     * Uses ObjectMapper.convertValue to preserve numeric (Double) and boolean types
     * so SpEL arithmetic works correctly.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> buildContext(JsonNode entity, Map<String, JsonNode> customFields) {
        Map<String, Object> context = new HashMap<>();

        // Convert entity to Map using ObjectMapper — preserves types (Long, Double, Boolean, String)
        Map<String, Object> entityMap = objectMapper.convertValue(entity, Map.class);
        entityMap.forEach((key, value) -> context.put(key, toTyped(value)));

        // Add custom fields under both their bare key and a "customField_" prefix.
        // Bare key allows natural expressions like 'upfront_payment * pos_percentage / 100'.
        // The "customField_" prefix is kept for backward compatibility.
        customFields.forEach((key, node) -> {
            Object value = toTyped(objectMapper.convertValue(node, Object.class));
            context.put(key, value);
            context.put("customField_" + key, value);
        });

        return context;
    }

    /**
     * Convert numeric types to Double for consistent SpEL arithmetic.
     * Strings that look like numbers are also converted so that NUMERIC DB columns
     * (serialised as strings by some Jackson configs) work in expressions.
     */
    private Object toTyped(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String s) {
            // Try ISO datetime first (e.g. updatedAt, createdAt, closeDate)
            try { return LocalDateTime.parse(s); } catch (DateTimeParseException ignored) {}
            try { return LocalDate.parse(s); } catch (DateTimeParseException ignored) {}
            // Try numeric string (e.g. BigDecimal columns serialised as strings)
            try { return Double.parseDouble(s); } catch (NumberFormatException ignored) {}
        }
        return value;
    }

    public boolean evaluateExpression(String expression, JsonNode context) {
        // kept for backward compatibility
        return true;
    }

    /**
     * PropertyAccessor that lets SpEL resolve map keys as if they were bean properties.
     * This allows expressions like {@code name + ' ' + status} where the variables are
     * stored in a Map rather than a bean. Also handles method calls for math functions.
     */
    private static class MapPropertyAccessor implements org.springframework.expression.PropertyAccessor {
        @Override
        public Class<?>[] getSpecificTargetClasses() {
            return new Class[]{Map.class};
        }

        @Override
        public boolean canRead(org.springframework.expression.EvaluationContext ctx, Object target, String name) {
            return target instanceof Map;
        }

        @SuppressWarnings("unchecked")
        @Override
        public TypedValue read(org.springframework.expression.EvaluationContext ctx, Object target, String name) {
            return new TypedValue(((Map<String, Object>) target).get(name));
        }

        @Override
        public boolean canWrite(org.springframework.expression.EvaluationContext ctx, Object target, String name) {
            return false;
        }

        @Override
        public void write(org.springframework.expression.EvaluationContext ctx, Object target, String name, Object newValue) {}
    }

    /**
     * Wrapper object that provides math functions accessible from SpEL expressions.
     * Allows calling min(), max(), abs(), etc. directly in expressions.
     */
    public static class MathFunctionsWrapper {
        public Double min(Object a, Object b) {
            return MathFunctions.min(a, b);
        }
        public Double max(Object a, Object b) {
            return MathFunctions.max(a, b);
        }
        public Double abs(Object x) {
            return MathFunctions.abs(x);
        }
        public Long round(Object x) {
            return MathFunctions.round(x);
        }
        public Double floor(Object x) {
            return MathFunctions.floor(x);
        }
        public Double ceil(Object x) {
            return MathFunctions.ceil(x);
        }
        public Double sqrt(Object x) {
            return MathFunctions.sqrt(x);
        }
        public Double pow(Object x, Object y) {
            return MathFunctions.pow(x, y);
        }
    }
}
