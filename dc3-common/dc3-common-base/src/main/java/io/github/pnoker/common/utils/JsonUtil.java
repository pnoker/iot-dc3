/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.exception.JsonException;

import java.io.DataInput;
import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Json 工具类
 * 基于 Jackson 实现
 *
 * @author pnoker
 * @since 2022.1.0
 */
public final class JsonUtil {

    private JsonUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, Boolean.FALSE)
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, Boolean.FALSE)
            .serializationInclusion(JsonInclude.Include.NON_NULL).build();


    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param text      Json String
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(String text, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(text, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param bytes     Json Byte Array
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(byte[] bytes, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(bytes, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param jsonParser Json Parser
     * @param valueType  Java 对象
     * @param <T>        Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(JsonParser jsonParser, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(jsonParser, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param dataInput Json DataInput
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(DataInput dataInput, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(dataInput, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param inputStream Json InputStream
     * @param valueType   Java 对象
     * @param <T>         Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(InputStream inputStream, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(inputStream, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param reader    Json Reader
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(Reader reader, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(reader, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象
     *
     * @param file      Json File
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(File file, Class<T> valueType) {
        try {
            return OBJECT_MAPPER.readValue(file, valueType);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将Json 字符串转化为指定对象,并将指定对象中的泛型转为指定的T类型
     *
     * @param text          Json String
     * @param typeReference TypeReference，T 为指定要转化为的数据类型
     * @param <T>           Java 对象类型
     * @return Java 对象
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        try {
            return OBJECT_MAPPER.readValue(text, typeReference);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param text      Json String
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param bytes     Json Byte Array
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param jsonParser Json JsonParser
     * @param valueType  Java 对象
     * @param <T>        Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param dataInput Json DataInput
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param inputStream Json InputStream
     * @param valueType   Java 对象
     * @param <T>         Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param reader    Json Reader
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
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
     * 反序列化，将 Json 字符串解析为 Java 对象集合
     *
     * @param file      Json File
     * @param valueType Java 对象
     * @param <T>       Java 对象类型
     * @return Java 对象集合
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
     * 序列化，将一个 Java 对象或 Java 对象集合转化为 Json 字符串
     *
     * @param type Java 对象引用
     * @param <T>  Java 对象类型
     * @return Json 字符串
     */
    public static <T> String toJsonString(T type) {
        try {
            return OBJECT_MAPPER.writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 序列化，将一个 Java 对象转化为 Json 字符串，并定制 Json 展示内容
     *
     * @param type              Java 对象引用
     * @param serializationView 定制现实内容的 Java 对象
     * @param <T>               Java 对象类型
     * @return Json 字符串
     */
    public static <T> String toJsonString(T type, Class<?> serializationView) {
        try {
            return OBJECT_MAPPER.writerWithView(serializationView).writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 美化序列化，将一个 Java 对象或 Java 对象集合转化为 Json 字符串
     *
     * @param type Java 对象引用
     * @param <T>  Java 对象类型
     * @return Json 字符串
     */
    public static <T> String toPrettyJsonString(T type) {
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 美化序列化，将一个Java 对象转化为 Json 字符串，并定制 Json 展示内容
     *
     * @param type              Java 对象引用
     * @param serializationView 定制现实内容的Java 对象
     * @param <T>               Java 对象类型
     * @return Json 字符串
     */
    public static <T> String toPrettyJsonString(T type, Class<?> serializationView) {
        try {
            return OBJECT_MAPPER.writerWithView(serializationView).withDefaultPrettyPrinter().writeValueAsString(type);
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 序列化，将一个 Java 对象或 Java 对象集合转化为 Json Byte Array
     *
     * @param type Java 对象引用
     * @param <T>  Java 对象类型
     * @return Json 字符串
     */
    public static <T> byte[] toJsonBytes(T type) {
        try {
            return DecodeUtil.stringToByte(OBJECT_MAPPER.writeValueAsString(type));
        } catch (Exception e) {
            throw new JsonException(e);
        }
    }

    /**
     * 返回一个ObjectMapper对象
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}