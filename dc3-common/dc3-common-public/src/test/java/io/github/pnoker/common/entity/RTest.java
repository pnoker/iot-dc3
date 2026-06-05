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

package io.github.pnoker.common.entity;

import io.github.pnoker.common.enums.ResponseEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RTest {

    @Test
    void okWithoutArgsReturnsDefaultSuccessEnvelope() {
        R<String> response = R.ok();
        assertThat(response.isOk()).isTrue();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.OK.getRemark());
        assertThat(response.getData()).isNull();
    }

    @Test
    void okWithMessageReplacesDefaultMessage() {
        R<String> response = R.ok("custom-message");
        assertThat(response.isOk()).isTrue();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getMessage()).isEqualTo("custom-message");
    }

    @Test
    void okWithResponseEnumUsesEnumCodeAndText() {
        R<String> response = R.ok(ResponseEnum.OK);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.OK.getRemark());
    }

    @Test
    void okWithEnumAndCustomMessageRetainsEnumCode() {
        R<String> response = R.ok(ResponseEnum.OK, "fine");
        assertThat(response.getCode()).isEqualTo(ResponseEnum.OK.getCode());
        assertThat(response.getMessage()).isEqualTo("fine");
    }

    @Test
    void okWithDataExposesDataAndDefaultEnvelope() {
        // Note: ok(String) is an overload that customizes the message rather than
        // setting data, so non-String payloads are required to exercise the data
        // branch unambiguously.
        R<Integer> response = R.ok(123);
        assertThat(response.getData()).isEqualTo(123);
        assertThat(response.isOk()).isTrue();
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.OK.getRemark());
    }

    @Test
    void okWithDataAndMessageOverridesMessage() {
        R<Integer> response = R.ok(42, "ack");
        assertThat(response.getData()).isEqualTo(42);
        assertThat(response.getMessage()).isEqualTo("ack");
    }

    @Test
    void failWithoutArgsReturnsDefaultFailureEnvelope() {
        R<String> response = R.fail();
        assertThat(response.isOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.FAILURE.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.FAILURE.getRemark());
    }

    @Test
    void failWithMessageReplacesText() {
        R<String> response = R.fail("kaboom");
        assertThat(response.isOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.FAILURE.getCode());
        assertThat(response.getMessage()).isEqualTo("kaboom");
    }

    @Test
    void failWithEnumUsesEnumCodeAndText() {
        R<String> response = R.fail(ResponseEnum.TOKEN_INVALID);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getCode()).isEqualTo(ResponseEnum.TOKEN_INVALID.getCode());
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.TOKEN_INVALID.getRemark());
    }

    @Test
    void failWithEnumAndCustomMessageKeepsEnumCode() {
        R<String> response = R.fail(ResponseEnum.TOKEN_INVALID, "explicit");
        assertThat(response.getCode()).isEqualTo(ResponseEnum.TOKEN_INVALID.getCode());
        assertThat(response.getMessage()).isEqualTo("explicit");
    }

    @Test
    void failWithDataExposesData() {
        // fail(String) is the message overload; non-String payloads exercise data path.
        R<Integer> response = R.fail(456);
        assertThat(response.getData()).isEqualTo(456);
        assertThat(response.isOk()).isFalse();
        assertThat(response.getMessage()).isEqualTo(ResponseEnum.FAILURE.getRemark());
    }

    @Test
    void failWithDataAndMessageOverridesMessage() {
        R<Integer> response = R.fail(99, "oops");
        assertThat(response.getData()).isEqualTo(99);
        assertThat(response.getMessage()).isEqualTo("oops");
    }
}
