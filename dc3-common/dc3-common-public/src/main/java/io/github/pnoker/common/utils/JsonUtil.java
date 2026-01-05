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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.exception.JsonException;
import org.apache.commons.lang3.StringUtils;

import java.io.DataInput;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON Utility Class
 * <p>
 * Utility class for JSON serialization and deserialization operations.
 * Based on Jackson implementation for comprehensive JSON processing.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
public final class JsonUtil {

    private static final JsonMapper JSON_MAPPER = getJsonMapper();

    private JsonUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get a JsonMapper object
     *
     * @return JsonMapper configured with proper settings
     */
    public static JsonMapper getJsonMapper() {
        LocalDateTimeSerializer serializer = new LocalDateTimeSerializer(LocalDateTimeUtil.getCompleteDateTimeFormatter());
        LocalDateTimeDeserializer deserializer = new LocalDateTimeDeserializer(LocalDateTimeUtil.getCompleteDateTimeFormatter());
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, serializer);
        module.addDeserializer(LocalDateTime.class, deserializer);
        return JsonMapper.builder()
                .findAndAddModules()
                .addModule(module)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, Boolean.FALSE)
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, Boolean.TRUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE)
                .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, Boolean.FALSE)
                .serializationInclusion(JsonInclude.Include.NON_NULL).build();
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

            return JSON_MAPPER.readValue(text, valueType);
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
            return JSON_MAPPER.readValue(bytes, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * Deserialize JSON string to Java object using JsonParser
     *
     * @param jsonParser JsonParser instance for parsing
     * @param valueType Java object type
     * @param <T>        Java object type
     * @return Java object
     */
    public static <T> T parseObject(JsonParser jsonParser, Class<T> valueType) {
        try {
            return JSON_MAPPER.readValue(jsonParser, valueType);
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
            return JSON_MAPPER.readValue(dataInput, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象
     *
     * @param inputStream Json InputStream
     * @param valueType   Java 对象
     * @param <T>         Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(InputStream inputStream, Class<T> valueType) {
        try {
            return JSON_MAPPER.readValue(inputStream, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象
     *
     * @param reader    Json Reader
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(Reader reader, Class<T> valueType) {
        try {
            return JSON_MAPPER.readValue(reader, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象
     *
     * @param file      Json File
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(File file, Class<T> valueType) {
        try {
            return JSON_MAPPER.readValue(file, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将Json 字符串转化为指定对象,并将指定对象中的泛型转为指定的T类型
     *
     * @param text          Json String
     * @param typeReference TypeReference, T 为指定要转化为的数据类型
     * @param <T>           Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return JSON_MAPPER.readValue(text, typeReference);
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
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(text, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param bytes     Json Byte Array
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(byte[] bytes, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(bytes, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param jsonParser Json JsonParser
     * @param valueType  Java 对象
     * @param <T>        Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(JsonParser jsonParser, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(jsonParser, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param dataInput Json DataInput
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(DataInput dataInput, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(dataInput, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param inputStream Json InputStream
     * @param valueType   Java 对象
     * @param <T>         Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(InputStream inputStream, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(inputStream, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param reader    Json Reader
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(Reader reader, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(reader, javaType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化, 将 Json 字符串解析为 Java 对象集合
     *
     * @param file      Json File
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
     */
    public static <T> List<T> parseArray(File file, Class<T> valueType) {
        try {
            JavaType javaType = JSON_MAPPER.getTypeFactory().constructParametricType(ArrayList.class, valueType);
            return JSON_MAPPER.readValue(file, javaType);
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
            return JSON_MAPPER.writeValueAsString(type);
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
            return JSON_MAPPER.writerWithView(serializationView).writeValueAsString(type);
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
            return JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(type);
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
            return JSON_MAPPER.writerWithView(serializationView).withDefaultPrettyPrinter().writeValueAsString(type);
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
            return DecodeUtil.stringToByte(JSON_MAPPER.writeValueAsString(type));
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
        if (text == null || text.isEmpty()) return false;
        try {
            JSON_MAPPER.readTree(text);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}