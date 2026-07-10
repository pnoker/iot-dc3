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
import io.github.pnoker.common.exception.JsonException;
import org.apache.commons.lang3.StringUtils;
import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.DataInput;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JSON serialization/deserialization utility based on Jackson.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = getObjectMapper();

    private JsonUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get a JsonMapper object
     *
     * @return ObjectMapper configured with proper settings
     */
    public static JsonMapper getJsonMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE)
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, Boolean.FALSE)
                .build();
    }

    /**
     * Get a ObjectMapper object
     *
     * @return ObjectMapper configured with proper settings
     */
    public static ObjectMapper getObjectMapper() {
        return JsonMapper.builder()
                .findAndAddModules()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE)
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, Boolean.FALSE)
                .build();
    }

    /**
     * Deserialize JSON string to Java object
     *
     * @param text      JSON string
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object
     */
    public static <T> T parseObject(String text, Class<T> valueType) {
        try {
            if (StringUtils.isEmpty(text)) {
                return null;
            }

            return OBJECT_MAPPER.readValue(text, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON byte array to Java object
     *
     * @param bytes     JSON byte array
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object
     */
    public static <T> T parseObject(byte[] bytes, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(bytes, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON string to Java object using JsonParser
     *
     * @param jsonParser JsonParser instance for parsing
     * @param valueType  Java object type
     * @param <T>        Java object type
     * @return Java object
     */
    public static <T> T parseObject(JsonParser jsonParser, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(jsonParser, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON string to Java object using DataInput
     *
     * @param dataInput DataInput instance for parsing
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object
     */
    public static <T> T parseObject(DataInput dataInput, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(dataInput, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from an {@link InputStream} into a Java object.
     *
     * @param inputStream Json InputStream
     * @param valueType   Java object type
     * @param <T>         Java object type
     * @return Java object
     */
    public static <T> T parseObject(InputStream inputStream, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from a {@link Reader} into a Java object.
     *
     * @param reader    Json Reader
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object
     */
    public static <T> T parseObject(Reader reader, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(reader, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from a {@link File} into a Java object.
     *
     * @param file      Json file
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object
     */
    public static <T> T parseObject(File file, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(file, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON string into a specified generic Java object.
     *
     * @param text          Json string
     * @param typeReference TypeReference, T is the target data type
     * @param <T>           Java object type
     * @return Java object
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON string to Java object collection
     *
     * @param text      JSON string
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(String text, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(text, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON byte array into a collection of Java objects.
     *
     * @param bytes     Json byte array
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(byte[] bytes, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(bytes, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content using {@link JsonParser} into a collection of Java
     * objects.
     *
     * @param jsonParser Json JsonParser
     * @param valueType  Java object type
     * @param <T>        Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(JsonParser jsonParser, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(jsonParser, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content using {@link DataInput} into a collection of Java objects.
     *
     * @param dataInput Json DataInput
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(DataInput dataInput, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(dataInput, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from an {@link InputStream} into a collection of Java
     * objects.
     *
     * @param inputStream Json InputStream
     * @param valueType   Java object type
     * @param <T>         Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(InputStream inputStream, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(inputStream, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from a {@link Reader} into a collection of Java objects.
     *
     * @param reader    Json Reader
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(Reader reader, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(reader, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON content from a {@link File} into a collection of Java objects.
     *
     * @param file      Json file
     * @param valueType Java object type
     * @param <T>       Java object type
     * @return Java object collection
     */
    public static <T> List<T> parseArray(File file, Class<T> valueType) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return OBJECT_MAPPER.readValue(file, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize Java object or collection to JSON string
     *
     * @param type Java object reference
     * @param <T>  Java object type
     * @return JSON string
     */
    public static <T> String toJsonString(T type) {
        try {
            return OBJECT_MAPPER.writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize Java object to JSON string with custom view
     *
     * @param type              Java object reference
     * @param serializationView Custom view class for JSON content
     * @param <T>               Java object type
     * @return JSON string
     */
    public static <T> String toJsonString(T type, Class<?> serializationView) {
        try {
            return OBJECT_MAPPER.writerWithView(serializationView).writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize Java object to pretty JSON string
     *
     * @param type Java object reference
     * @param <T>  Java object type
     * @return Pretty formatted JSON string
     */
    public static <T> String toPrettyJsonString(T type) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize Java object to pretty JSON string with custom view
     *
     * @param type              Java object reference
     * @param serializationView Custom view class for JSON content
     * @param <T>               Java object type
     * @return Pretty formatted JSON string with custom view
     */
    public static <T> String toPrettyJsonString(T type, Class<?> serializationView) {
        try {
            return OBJECT_MAPPER.writerWithView(serializationView).withDefaultPrettyPrinter().writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Serialize Java object to JSON byte array
     *
     * @param type Java object reference
     * @param <T>  Java object type
     * @return JSON byte array
     */
    public static <T> byte[] toJsonBytes(T type) {
        try {
            return DecodeUtil.stringToByte(OBJECT_MAPPER.writeValueAsString(type));
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Check if string is valid JSON format
     *
     * @param text JSON string to validate
     * @return true if valid JSON, false otherwise
     */
    public static boolean isJson(String text) {
        if (Objects.isNull(text) || text.isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(text);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

}
