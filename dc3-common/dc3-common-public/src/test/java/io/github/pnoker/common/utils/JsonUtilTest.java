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

import io.github.pnoker.common.exception.JsonException;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonUtilTest {

    record Sample(String name, int age) {
    }

    @Test
    void parseObjectFromString() {
        Sample sample = JsonUtil.parseObject("{\"name\":\"a\",\"age\":1}", Sample.class);
        assertThat(sample).isEqualTo(new Sample("a", 1));
    }

    @Test
    void parseObjectFromBytes() {
        byte[] bytes = "{\"name\":\"b\",\"age\":2}".getBytes(StandardCharsets.UTF_8);
        Sample sample = JsonUtil.parseObject(bytes, Sample.class);
        assertThat(sample).isEqualTo(new Sample("b", 2));
    }

    @Test
    void parseObjectFromInputStream() {
        Sample sample = JsonUtil.parseObject(
                new ByteArrayInputStream("{\"name\":\"c\",\"age\":3}".getBytes(StandardCharsets.UTF_8)),
                Sample.class);
        assertThat(sample).isEqualTo(new Sample("c", 3));
    }

    @Test
    void parseObjectReturnsNullOnBlankString() {
        assertThat(JsonUtil.parseObject((String) null, Sample.class)).isNull();
        assertThat(JsonUtil.parseObject("", Sample.class)).isNull();
    }

    @Test
    void parseObjectFailsOnInvalidJson() {
        assertThatThrownBy(() -> JsonUtil.parseObject("{not-json", Sample.class))
                .isInstanceOf(JsonException.class);
    }

    @Test
    void parseObjectIgnoresUnknownProperties() {
        Sample sample = JsonUtil.parseObject("{\"name\":\"x\",\"age\":7,\"extra\":42}", Sample.class);
        assertThat(sample).isEqualTo(new Sample("x", 7));
    }

    @Test
    void parseArrayFromString() {
        List<Sample> samples = JsonUtil.parseArray("[{\"name\":\"a\",\"age\":1},{\"name\":\"b\",\"age\":2}]",
                Sample.class);
        assertThat(samples).containsExactly(new Sample("a", 1), new Sample("b", 2));
    }

    @Test
    void parseArrayFromBytes() {
        byte[] bytes = "[{\"name\":\"a\",\"age\":1}]".getBytes(StandardCharsets.UTF_8);
        List<Sample> samples = JsonUtil.parseArray(bytes, Sample.class);
        assertThat(samples).hasSize(1).first().isEqualTo(new Sample("a", 1));
    }

    @Test
    void parseObjectWithTypeReferenceForGenericMap() {
        Map<String, Integer> map = JsonUtil.parseObject(
                "{\"a\":1,\"b\":2}",
                new TypeReference<Map<String, Integer>>() {
                });
        assertThat(map).containsExactlyInAnyOrderEntriesOf(Map.of("a", 1, "b", 2));
    }

    @Test
    void toJsonStringSerialisesRecord() {
        String json = JsonUtil.toJsonString(new Sample("a", 1));
        assertThat(json).isEqualTo("{\"name\":\"a\",\"age\":1}");
    }

    @Test
    void toPrettyJsonStringIncludesIndentation() {
        String json = JsonUtil.toPrettyJsonString(new Sample("a", 1));
        assertThat(json).contains("\n").contains("  ");
    }

    @Test
    void toJsonBytesRoundTrips() {
        byte[] bytes = JsonUtil.toJsonBytes(new Sample("a", 1));
        Sample sample = JsonUtil.parseObject(bytes, Sample.class);
        assertThat(sample).isEqualTo(new Sample("a", 1));
    }

    @Test
    void isJsonValidatesStructure() {
        assertThat(JsonUtil.isJson("{\"a\":1}")).isTrue();
        assertThat(JsonUtil.isJson("[1,2,3]")).isTrue();
        assertThat(JsonUtil.isJson("\"abc\"")).isTrue();
        assertThat(JsonUtil.isJson("not json")).isFalse();
        assertThat(JsonUtil.isJson("")).isFalse();
        assertThat(JsonUtil.isJson(null)).isFalse();
    }

    @Test
    void utilityClassConstructorMustReject() throws NoSuchMethodException {
        Constructor<JsonUtil> constructor = JsonUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThatThrownBy(constructor::newInstance)
                .isInstanceOf(InvocationTargetException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }
}
