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
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.Objects;

/**
 * Apache POI Utility Class
 * <p>
 * Utility class for spreadsheet operations using Apache POI. Provides methods for working
 * with Excel files, cells, and various spreadsheet manipulations.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@Slf4j
public class PoiUtil {

    private PoiUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Get a {@link CellStyle} with both horizontal and vertical alignment set to center.
     *
     * @param workbook Workbook
     * @return Centered CellStyle
     */
    public static CellStyle getCenterCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    /**
     * Merge a range of cells.
     *
     * @param sheet    Sheet
     * @param firstRow Index of first row
     * @param lastRow  Index of last row (inclusive), must be equal to or larger than
     *                 {@code firstRow}
     * @param firstCol Index of first column
     * @param lastCol  Index of last column (inclusive), must be equal to or larger than
     *                 {@code firstCol}
     */
    public static void mergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if ((lastRow - firstRow) < 1 && (lastCol - firstCol) < 1) {
            return;
        }

        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    /**
     * Get the string content of the specified cell.
     *
     * @param sheet     Sheet
     * @param rowIndex  Row index
     * @param cellIndex Cell index
     * @return String value of the cell, or an empty string if the cell is null
     */
    public static String getCellStringValue(Sheet sheet, int rowIndex, int cellIndex) {
        Row driverAttributesRow = sheet.getRow(rowIndex);
        if (Objects.isNull(driverAttributesRow)) {
            return "";
        }

        Cell driverAttributesCell = driverAttributesRow.getCell(cellIndex);
        if (Objects.isNull(driverAttributesCell)) {
            return "";
        }

        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(driverAttributesCell);
    }

    /**
     * Create a cell with the specified value.
     *
     * @param row       Row
     * @param cellIndex Cell index
     * @param cellValue Cell value
     */
    public static void createCell(Row row, int cellIndex, String cellValue) {
        createCellWithStyle(row, cellIndex, cellValue, null);
    }

    /**
     * Create a cell with the specified value and style.
     *
     * @param row       Row
     * @param cellIndex Cell index
     * @param cellValue Cell value
     * @param cellStyle Cell style
     */
    public static void createCellWithStyle(Row row, int cellIndex, String cellValue, CellStyle cellStyle) {
        Cell deviceNameCell = row.createCell(cellIndex);
        deviceNameCell.setCellValue(cellValue);

        if (Objects.nonNull(cellStyle)) {
            deviceNameCell.setCellStyle(cellStyle);
        }
    }

}
