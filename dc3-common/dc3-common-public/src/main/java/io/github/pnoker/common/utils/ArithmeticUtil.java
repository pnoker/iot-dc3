/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Arithmetic Utility Class
 * <p>
 * Utility class for precise floating-point arithmetic operations.
 * Provides accurate addition, subtraction, multiplication, division,
 * and rounding operations using BigDecimal.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public class ArithmeticUtil {

    private ArithmeticUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Provide precise addition operation.
     *
     * @param value1 Augend
     * @param value2 Addend
     * @return Sum of the two parameters
     */
    public static BigDecimal add(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.add(bigDecimal2);
    }

    /**
     * Provide float addition operation.
     *
     * @param value1 Augend
     * @param value2 Addend
     * @return Sum of the two parameters
     */
    public static float add(float value1, float value2) {
        return add(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    /**
     * Provide double addition operation.
     *
     * @param value1 Augend
     * @param value2 Addend
     * @return Sum of the two parameters
     */
    public static double add(double value1, double value2) {
        return add(Double.toString(value1), Double.toString(value2)).doubleValue();
    }


    /**
     * Provide precise subtraction operation.
     *
     * @param value1 Minuend
     * @param value2 Subtrahend
     * @return Difference of the two parameters
     */
    public static BigDecimal subtract(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.subtract(bigDecimal2);
    }

    /**
     * Provide float subtraction operation.
     *
     * @param value1 Minuend
     * @param value2 Subtrahend
     * @return Difference of the two parameters
     */
    public static float subtract(float value1, float value2) {
        return subtract(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    /**
     * Provide double subtraction operation.
     *
     * @param value1 Minuend
     * @param value2 Subtrahend
     * @return Difference of the two parameters
     */
    public static double subtract(double value1, double value2) {
        return subtract(Double.toString(value1), Double.toString(value2)).doubleValue();
    }

    /**
     * Provide precise multiplication operation.
     *
     * @param value1 Multiplicand
     * @param value2 Multiplier
     * @return Product of the two parameters
     */
    public static BigDecimal multiply(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.multiply(bigDecimal2);
    }

    /**
     * Provide float multiplication operation.
     *
     * @param value1 Multiplicand
     * @param value2 Multiplier
     * @return Product of the two parameters
     */
    public static float multiply(float value1, float value2) {
        return multiply(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    /**
     * Provide double multiplication operation.
     *
     * @param value1 Multiplicand
     * @param value2 Multiplier
     * @return Product of the two parameters
     */
    public static double multiply(double value1, double value2) {
        return multiply(Double.toString(value1), Double.toString(value2)).doubleValue();
    }

    /**
     * Provide (relatively) precise division operation.
     * <p>
     * When an inexact division occurs, the {@code scale} parameter specifies the precision,
     * and the result is rounded using the half-up rule.
     *
     * @param value1 Dividend
     * @param value2 Divisor
     * @param scale  Number of decimal places to keep
     * @return Quotient of the two parameters
     */
    public static BigDecimal divide(String value1, String value2, int scale) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.divide(bigDecimal2, scale, RoundingMode.HALF_UP);
    }

    /**
     * Provide float division operation.
     * <p>
     * When an inexact division occurs, the {@code scale} parameter specifies the precision,
     * and the result is rounded using the half-up rule.
     *
     * @param value1 Dividend
     * @param value2 Divisor
     * @param scale  Number of decimal places to keep
     * @return Quotient of the two parameters
     */
    public static float divide(float value1, float value2, int scale) {
        if (scale < 0 || scale > 7) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 7)");
        }
        return divide(Float.toString(value1), Float.toString(value2), scale).floatValue();
    }

    /**
     * Provide double division operation.
     * <p>
     * When an inexact division occurs, the {@code scale} parameter specifies the precision,
     * and the result is rounded using the half-up rule.
     *
     * @param value1 Dividend
     * @param value2 Divisor
     * @param scale  Number of decimal places to keep
     * @return Quotient of the two parameters
     */
    public static double divide(double value1, double value2, int scale) {
        if (scale < 0 || scale > 16) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 16)");
        }
        return divide(Double.toString(value1), Double.toString(value2), scale).doubleValue();
    }

    /**
     * Provide precise decimal rounding operation.
     *
     * @param value Number to be rounded
     * @param scale Number of decimal places to keep
     * @return Rounded result
     */
    public static BigDecimal round(String value, int scale) {
        BigDecimal bigDecimal = new BigDecimal(value);
        BigDecimal tmp = new BigDecimal("1");
        return bigDecimal.divide(tmp, scale, RoundingMode.HALF_UP);
    }

    /**
     * Provide float decimal rounding operation.
     *
     * @param value Number to be rounded
     * @param scale Number of decimal places to keep
     * @return Rounded result
     */
    public static float round(float value, int scale) {
        if (scale < 0 || scale > 7) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 7)");
        }
        return round(Float.toString(value), scale).floatValue();
    }

    /**
     * Provide double decimal rounding operation.
     *
     * @param value Number to be rounded
     * @param scale Number of decimal places to keep
     * @return Rounded result
     */
    public static double round(double value, int scale) {
        if (scale < 0 || scale > 16) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 16)");
        }
        return round(Double.toString(value), scale).doubleValue();
    }
}
