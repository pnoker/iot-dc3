package com.dc3.common.utils;

import java.math.BigDecimal;

/**
 * 提供精确的浮点数运算，包括加减乘除和四舍五入
 *
 * @author pnoker
 */
public class ArithmeticUtil {

    /**
     * 提供精确的加法运算。
     *
     * @param value1 被加数
     * @param value2 加数
     * @return 两个参数的和
     */
    public static BigDecimal add(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.add(bigDecimal2);
    }

    public static float add(float value1, float value2) {
        return add(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    public static double add(double value1, double value2) {
        return add(Double.toString(value1), Double.toString(value2)).doubleValue();
    }


    /**
     * 提供精确的减法运算。
     *
     * @param value1 被减数
     * @param value2 减数
     * @return 两个参数的差
     */
    public static BigDecimal subtract(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.subtract(bigDecimal2);
    }

    public static float subtract(float value1, float value2) {
        return subtract(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    public static double subtract(double value1, double value2) {
        return subtract(Double.toString(value1), Double.toString(value2)).doubleValue();
    }

    /**
     * 提供精确的乘法运算。
     *
     * @param value1 被乘数
     * @param value2 乘数
     * @return 两个参数的积
     */
    public static BigDecimal multiply(String value1, String value2) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.multiply(bigDecimal2);
    }

    public static float multiply(float value1, float value2) {
        return multiply(Float.toString(value1), Float.toString(value2)).floatValue();
    }

    public static double multiply(double value1, double value2) {
        return multiply(Double.toString(value1), Double.toString(value2)).doubleValue();
    }

    /**
     * 提供（相对）精确的除法运算。当发生除不尽的情况时，由scale参数指
     * 定精度，以后的数字四舍五入。
     *
     * @param value1 被除数
     * @param value2 除数
     * @param scale  表示表示需要精确到小数点以后几位。
     * @return 两个参数的商
     */
    public static BigDecimal divide(String value1, String value2, int scale) {
        BigDecimal bigDecimal1 = new BigDecimal(value1);
        BigDecimal bigDecimal2 = new BigDecimal(value2);
        return bigDecimal1.divide(bigDecimal2, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static float divide(float value1, float value2, int scale) {
        if (scale < 0 || scale > 7) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 7)");
        }
        return divide(Float.toString(value1), Float.toString(value2), scale).floatValue();
    }

    public static double divide(double value1, double value2, int scale) {
        if (scale < 0 || scale > 16) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 16)");
        }
        return divide(Double.toString(value1), Double.toString(value2), scale).doubleValue();
    }

    /**
     * 提供精确的小数位四舍五入处理。
     *
     * @param value 需要四舍五入的数字
     * @param scale 小数点后保留几位
     * @return 四舍五入后的结果
     */
    public static BigDecimal round(String value, int scale) {
        BigDecimal bigDecimal = new BigDecimal(value);
        BigDecimal tmp = new BigDecimal("1");
        return bigDecimal.divide(tmp, scale, BigDecimal.ROUND_HALF_UP);
    }

    public static float round(float value, int scale) {
        if (scale < 0 || scale > 7) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 7)");
        }
        return round(Float.toString(value), scale).floatValue();
    }

    public static double round(double value, int scale) {
        if (scale < 0 || scale > 16) {
            throw new IllegalArgumentException(
                    "The scale must be a positive integer(0 ~ 16)");
        }
        return round(Double.toString(value), scale).doubleValue();
    }
}
