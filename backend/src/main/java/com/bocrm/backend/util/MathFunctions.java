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

/**
 * Static utility methods for math operations in SpEL expressions.
 * These are registered as SpEL functions to allow calculated field expressions
 * to use min(), max(), abs(), round(), floor(), ceil(), sqrt(), pow().
 *
 * @author Ricardo Salvador
 * @since 1.0.0
 */
public class MathFunctions {

    /**
     * Returns the minimum of two numbers.
     */
    public static Double min(Object a, Object b) {
        double aVal = toDouble(a);
        double bVal = toDouble(b);
        return Math.min(aVal, bVal);
    }

    /**
     * Returns the maximum of two numbers.
     */
    public static Double max(Object a, Object b) {
        double aVal = toDouble(a);
        double bVal = toDouble(b);
        return Math.max(aVal, bVal);
    }

    /**
     * Returns the absolute value of a number.
     */
    public static Double abs(Object x) {
        return Math.abs(toDouble(x));
    }

    /**
     * Rounds to the nearest integer.
     */
    public static Long round(Object x) {
        return Math.round(toDouble(x));
    }

    /**
     * Rounds down to the nearest integer.
     */
    public static Double floor(Object x) {
        return Math.floor(toDouble(x));
    }

    /**
     * Rounds up to the nearest integer.
     */
    public static Double ceil(Object x) {
        return Math.ceil(toDouble(x));
    }

    /**
     * Returns the square root of a number.
     */
    public static Double sqrt(Object x) {
        return Math.sqrt(toDouble(x));
    }

    /**
     * Returns x raised to the power of y.
     */
    public static Double pow(Object x, Object y) {
        return Math.pow(toDouble(x), toDouble(y));
    }

    /**
     * Helper to convert an object to Double, handling null and various types.
     */
    private static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
