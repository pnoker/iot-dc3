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

package io.github.pnoker.test.support;

import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * Convenience facade over {@link JSONAssert} so tests do not depend on the
 * {@code org.skyscreamer.jsonassert} package directly. Differences are surfaced as
 * {@link AssertionError} carrying the structural diff produced by JSONAssert.
 */
public final class JsonAssertions {

    private JsonAssertions() {
    }

    /**
     * Strict comparison: object order is irrelevant but field set must match exactly.
     */
    public static void assertJsonEquals(String expected, String actual) {
        try {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
        } catch (org.json.JSONException e) {
            throw new AssertionError("Failed to compare JSON payloads", e);
        }
    }

    /**
     * Loose comparison: extra fields in {@code actual} are tolerated. Useful for
     * asserting envelope subsets without locking the full contract.
     */
    public static void assertJsonContains(String expected, String actual) {
        try {
            JSONAssert.assertEquals(expected, actual, JSONCompareMode.LENIENT);
        } catch (org.json.JSONException e) {
            throw new AssertionError("Failed to compare JSON payloads", e);
        }
    }
}
