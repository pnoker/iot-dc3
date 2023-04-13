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
package com.serotonin.modbus4j;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>BatchResults class.</p>
 *
 * @author Matthew Lohbihler
 * @version 5.0.0
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return data.toString();
    }
}
