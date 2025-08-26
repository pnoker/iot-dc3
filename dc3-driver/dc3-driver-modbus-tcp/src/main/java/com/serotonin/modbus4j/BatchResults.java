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
package com.serotonin.modbus4j;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>BatchResults class.</p>
 *
 * @author Matthew Lohbihler
 * @version 2025.6.0
 */
public class BatchResults<K> {
    private final Map<K, Object> data = new HashMap<>();

    /**
     * <p>addResult.</p>
     *
     * @param key   a K object.
     * @param value a {@link Object} object.
     */
    public void addResult(K key, Object value) {
        data.put(key, value);
    }

    /**
     * <p>getValue.</p>
     *
     * @param key a K object.
     * @return a {@link Object} object.
     */
    public Object getValue(K key) {
        return data.get(key);
    }

    /**
     * <p>getIntValue.</p>
     *
     * @param key a K object.
     * @return a {@link Integer} object.
     */
    public Integer getIntValue(K key) {
        return (Integer) getValue(key);
    }

    /**
     * <p>getLongValue.</p>
     *
     * @param key a K object.
     * @return a {@link Long} object.
     */
    public Long getLongValue(K key) {
        return (Long) getValue(key);
    }

    /**
     * <p>getDoubleValue.</p>
     *
     * @param key a K object.
     * @return a {@link Double} object.
     */
    public Double getDoubleValue(K key) {
        return (Double) getValue(key);
    }

    /**
     * <p>getFloatValue.</p>
     *
     * @param key a K object.
     * @return a {@link Float} object.
     */
    public Float getFloatValue(K key) {
        return (Float) getValue(key);
    }


    @Override
    public String toString() {
        return data.toString();
    }
}
