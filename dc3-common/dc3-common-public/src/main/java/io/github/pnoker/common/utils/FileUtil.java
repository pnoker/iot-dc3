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
import io.github.pnoker.common.constant.common.FolderConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Temp directory and random XLSX file name helpers.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class FileUtil {

    private FileUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get temporary upload file directory
     *
     * @return Temporary upload file directory path
     */
    public static String getTempPath() {
        return getTempPath(new String[0]);
    }

    /**
     * Get module-scoped temporary upload directory.
     *
     * @param segments module or business path segments
     * @return Temporary upload directory path
     */
    public static String getTempPath(String... segments) {
        Path dir = Paths.get(FolderConstant.TEMP_FILE_PATH, safePathSegments(segments));
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("Failed to create temp directory: {}", dir, e);
            }
        }
        return dir.toString() + SymbolConstant.SLASH;
    }

    /**
     * Create a module-scoped temporary upload file path.
     *
     * @param fileName target file name
     * @param segments module or business path segments
     * @return Temporary upload file path
     */
    public static Path getTempUploadFilePath(String fileName, String... segments) {
        return Paths.get(getTempPath(segments), safePathPart(fileName)).toAbsolutePath().normalize();
    }

    /**
     * Generate random XLSX file name
     *
     * @return Random XLSX file name with UUID
     */
    public static String getRandomXlsxName() {
        return UUID.randomUUID() + SymbolConstant.DOT + "xlsx";
    }

    /**
     * Sanitize each path segment via {@link #safePathPart}, returning an empty array
     * for null input.
     *
     * @param segments raw path segments
     * @return sanitized segments
     */
    private static String[] safePathSegments(String... segments) {
        if (segments == null) {
            return new String[0];
        }
        String[] safeSegments = new String[segments.length];
        for (int i = 0; i < segments.length; i++) {
            safeSegments[i] = safePathPart(segments[i]);
        }
        return safeSegments;
    }

    /**
     * Sanitize a single path segment: blank input falls back to {@code "upload"}, and
     * any character outside {@code [a-zA-Z0-9._-]} is replaced with an underscore.
     *
     * @param value raw path segment
     * @return sanitized segment, never blank
     */
    private static String safePathPart(String value) {
        if (value == null || value.isBlank()) {
            return "upload";
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", SymbolConstant.UNDERSCORE);
        return sanitized.isBlank() ? "upload" : sanitized;
    }

}
