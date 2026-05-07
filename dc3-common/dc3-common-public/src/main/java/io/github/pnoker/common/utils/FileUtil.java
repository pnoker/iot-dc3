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
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * File Related Utility Class
 * <p>
 * Utility class for file operations including directory creation and random file name
 * generation for temporary files.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
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
        String path = FolderConstant.TEMP_FILE_PATH;
        Path dir = Paths.get(path);
        if (Files.notExists(dir) || !Files.isDirectory(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
        return path;
    }

    /**
     * Generate random XLSX file name
     *
     * @return Random XLSX file name with UUID
     */
    public static String getRandomXlsxName() {
        return UUID.randomUUID().toString() + ".xlsx";
    }

}
