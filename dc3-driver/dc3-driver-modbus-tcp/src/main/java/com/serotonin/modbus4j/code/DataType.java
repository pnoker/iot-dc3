/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.serotonin.modbus4j.code;

import java.math.BigInteger;

/**
 * <p>DataType class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
 */
public class DataType {
    /**
     * Constant <code>BINARY=1</code>
     */
    public static final int BINARY = 1;

    /**
     * Constant <code>TWO_BYTE_INT_UNSIGNED=2</code>
     */
    public static final int TWO_BYTE_INT_UNSIGNED = 2;
    /**
     * Constant <code>TWO_BYTE_INT_SIGNED=3</code>
     */
    public static final int TWO_BYTE_INT_SIGNED = 3;
    /**
     * Constant <code>TWO_BYTE_INT_UNSIGNED_SWAPPED=22</code>
     */
    public static final int TWO_BYTE_INT_UNSIGNED_SWAPPED = 22;
    /**
     * Constant <code>TWO_BYTE_INT_SIGNED_SWAPPED=23</code>
     */
    public static final int TWO_BYTE_INT_SIGNED_SWAPPED = 23;

    /**
     * Constant <code>FOUR_BYTE_INT_UNSIGNED=4</code>
     */
    public static final int FOUR_BYTE_INT_UNSIGNED = 4;
    /**
     * Constant <code>FOUR_BYTE_INT_SIGNED=5</code>
     */
    public static final int FOUR_BYTE_INT_SIGNED = 5;
    /**
     * Constant <code>FOUR_BYTE_INT_UNSIGNED_SWAPPED=6</code>
     */
    public static final int FOUR_BYTE_INT_UNSIGNED_SWAPPED = 6;
    /**
     * Constant <code>FOUR_BYTE_INT_SIGNED_SWAPPED=7</code>
     */
    public static final int FOUR_BYTE_INT_SIGNED_SWAPPED = 7;
    /* 0xAABBCCDD is transmitted as 0xDDCCBBAA */
    /**
     * Constant <code>FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED=24</code>
     */
    public static final int FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED = 24;
    /**
     * Constant <code>FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED=25</code>
     */
    public static final int FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED = 25;

    /**
     * Constant <code>FOUR_BYTE_FLOAT=8</code>
     */
    public static final int FOUR_BYTE_FLOAT = 8;
    /**
     * Constant <code>FOUR_BYTE_FLOAT_SWAPPED=9</code>
     */
    public static final int FOUR_BYTE_FLOAT_SWAPPED = 9;
    /**
     * Constant <code>FOUR_BYTE_FLOAT_SWAPPED_INVERTED=21</code>
     */
    public static final int FOUR_BYTE_FLOAT_SWAPPED_INVERTED = 21;

    /**
     * Constant <code>EIGHT_BYTE_INT_UNSIGNED=10</code>
     */
    public static final int EIGHT_BYTE_INT_UNSIGNED = 10;
    /**
     * Constant <code>EIGHT_BYTE_INT_SIGNED=11</code>
     */
    public static final int EIGHT_BYTE_INT_SIGNED = 11;
    /**
     * Constant <code>EIGHT_BYTE_INT_UNSIGNED_SWAPPED=12</code>
     */
    public static final int EIGHT_BYTE_INT_UNSIGNED_SWAPPED = 12;
    /**
     * Constant <code>EIGHT_BYTE_INT_SIGNED_SWAPPED=13</code>
     */
    public static final int EIGHT_BYTE_INT_SIGNED_SWAPPED = 13;
    /**
     * Constant <code>EIGHT_BYTE_FLOAT=14</code>
     */
    public static final int EIGHT_BYTE_FLOAT = 14;
    /**
     * Constant <code>EIGHT_BYTE_FLOAT_SWAPPED=15</code>
     */
    public static final int EIGHT_BYTE_FLOAT_SWAPPED = 15;

    /**
     * Constant <code>TWO_BYTE_BCD=16</code>
     */
    public static final int TWO_BYTE_BCD = 16;
    /**
     * Constant <code>FOUR_BYTE_BCD=17</code>
     */
    public static final int FOUR_BYTE_BCD = 17;
    /**
     * Constant <code>FOUR_BYTE_BCD_SWAPPED=20</code>
     */
    public static final int FOUR_BYTE_BCD_SWAPPED = 20;

    /**
     * Constant <code>CHAR=18</code>
     */
    public static final int CHAR = 18;
    /**
     * Constant <code>VARCHAR=19</code>
     */
    public static final int VARCHAR = 19;

    //MOD10K two, three and four register types
    /**
     * Constant <code>FOUR_BYTE_MOD_10K=26</code>
     */
    public static final int FOUR_BYTE_MOD_10K = 26;
    /**
     * Constant <code>SIX_BYTE_MOD_10K=27</code>
     */
    public static final int SIX_BYTE_MOD_10K = 27;
    /**
     * Constant <code>EIGHT_BYTE_MOD_10K=28</code>
     */
    public static final int EIGHT_BYTE_MOD_10K = 28;
    /**
     * Constant <code>FOUR_BYTE_MOD_10K_SWAPPED=29</code>
     */
    public static final int FOUR_BYTE_MOD_10K_SWAPPED = 29;
    /**
     * Constant <code>SIX_BYTE_MOD_10K_SWAPPED=30</code>
     */
    public static final int SIX_BYTE_MOD_10K_SWAPPED = 30;
    /**
     * Constant <code>EIGHT_BYTE_MOD_10K_SWAPPED=31</code>
     */
    public static final int EIGHT_BYTE_MOD_10K_SWAPPED = 31;

    //One byte unsigned integer types
    /**
     * Constant <code>ONE_BYTE_INT_UNSIGNED_LOWER=32</code>
     */
    public static final int ONE_BYTE_INT_UNSIGNED_LOWER = 32;
    /**
     * Constant <code>ONE_BYTE_INT_UNSIGNED_UPPER=33</code>
     */
    public static final int ONE_BYTE_INT_UNSIGNED_UPPER = 33;

    /**
     * <p>getRegisterCount.</p>
     *
     * @param id a int.
     * @return a int.
     */
    public static int getRegisterCount(int id) {
        switch (id) {
            case BINARY:
            case TWO_BYTE_INT_UNSIGNED:
            case TWO_BYTE_INT_SIGNED:
            case TWO_BYTE_INT_UNSIGNED_SWAPPED:
            case TWO_BYTE_INT_SIGNED_SWAPPED:
            case TWO_BYTE_BCD:
            case ONE_BYTE_INT_UNSIGNED_LOWER:
            case ONE_BYTE_INT_UNSIGNED_UPPER:
                return 1;
            case FOUR_BYTE_INT_UNSIGNED:
            case FOUR_BYTE_INT_SIGNED:
            case FOUR_BYTE_INT_UNSIGNED_SWAPPED:
            case FOUR_BYTE_INT_SIGNED_SWAPPED:
            case FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED:
            case FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED:
            case FOUR_BYTE_FLOAT:
            case FOUR_BYTE_FLOAT_SWAPPED:
            case FOUR_BYTE_FLOAT_SWAPPED_INVERTED:
            case FOUR_BYTE_BCD:
            case FOUR_BYTE_BCD_SWAPPED:
            case FOUR_BYTE_MOD_10K:
            case FOUR_BYTE_MOD_10K_SWAPPED:
                return 2;
            case SIX_BYTE_MOD_10K:
            case SIX_BYTE_MOD_10K_SWAPPED:
                return 3;
            case EIGHT_BYTE_INT_UNSIGNED:
            case EIGHT_BYTE_INT_SIGNED:
            case EIGHT_BYTE_INT_UNSIGNED_SWAPPED:
            case EIGHT_BYTE_INT_SIGNED_SWAPPED:
            case EIGHT_BYTE_FLOAT:
            case EIGHT_BYTE_FLOAT_SWAPPED:
            case EIGHT_BYTE_MOD_10K:
            case EIGHT_BYTE_MOD_10K_SWAPPED:
                return 4;
        }
        return 0;
    }

    /**
     * <p>getJavaType.</p>
     *
     * @param id a int.
     * @return a {@link Class} object.
     */
    public static Class<?> getJavaType(int id) {
        switch (id) {
            case ONE_BYTE_INT_UNSIGNED_LOWER:
            case ONE_BYTE_INT_UNSIGNED_UPPER:
                return Integer.class;
            case BINARY:
                return Boolean.class;
            case TWO_BYTE_INT_UNSIGNED:
            case TWO_BYTE_INT_UNSIGNED_SWAPPED:
                return Integer.class;
            case TWO_BYTE_INT_SIGNED:
            case TWO_BYTE_INT_SIGNED_SWAPPED:
                return Short.class;
            case FOUR_BYTE_INT_UNSIGNED:
                return Long.class;
            case FOUR_BYTE_INT_SIGNED:
                return Integer.class;
            case FOUR_BYTE_INT_UNSIGNED_SWAPPED:
            case FOUR_BYTE_INT_UNSIGNED_SWAPPED_SWAPPED:
                return Long.class;
            case FOUR_BYTE_INT_SIGNED_SWAPPED:
            case FOUR_BYTE_INT_SIGNED_SWAPPED_SWAPPED:
                return Integer.class;
            case FOUR_BYTE_FLOAT:
                return Float.class;
            case FOUR_BYTE_FLOAT_SWAPPED:
                return Float.class;
            case FOUR_BYTE_FLOAT_SWAPPED_INVERTED:
                return Float.class;
            case EIGHT_BYTE_INT_UNSIGNED:
                return BigInteger.class;
            case EIGHT_BYTE_INT_SIGNED:
                return Long.class;
            case EIGHT_BYTE_INT_UNSIGNED_SWAPPED:
                return BigInteger.class;
            case EIGHT_BYTE_INT_SIGNED_SWAPPED:
                return Long.class;
            case EIGHT_BYTE_FLOAT:
                return Double.class;
            case EIGHT_BYTE_FLOAT_SWAPPED:
                return Double.class;
            case TWO_BYTE_BCD:
                return Short.class;
            case FOUR_BYTE_BCD:
            case FOUR_BYTE_BCD_SWAPPED:
                return Integer.class;
            case CHAR:
            case VARCHAR:
                return String.class;
            case FOUR_BYTE_MOD_10K:
            case SIX_BYTE_MOD_10K:
            case EIGHT_BYTE_MOD_10K:
            case FOUR_BYTE_MOD_10K_SWAPPED:
            case SIX_BYTE_MOD_10K_SWAPPED:
            case EIGHT_BYTE_MOD_10K_SWAPPED:
                return BigInteger.class;
        }
        return null;
    }
}
