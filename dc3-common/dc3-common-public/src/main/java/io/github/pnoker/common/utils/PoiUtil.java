/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.util.Objects;

/**
 * 表格操作 相关工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class PoiUtil {

    private PoiUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取水平和垂直居中的 CellStyle
     *
     * @param workbook Workbook
     * @return CellStyle
     */
    public static CellStyle getCenterCellStyle(Workbook workbook) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    /**
     * 合并单元格
     *
     * @param sheet    Sheet
     * @param firstRow Index of first row
     * @param lastRow  Index of last row (inclusive), must be equal to or larger than {@code firstRow}
     * @param firstCol Index of first column
     * @param lastCol  Index of last column (inclusive), must be equal to or larger than {@code firstCol}
     */
    public static void mergedRegion(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol) {
        if ((lastRow - firstRow) < 1 && (lastCol - firstCol) < 1) {
            return;
        }

        sheet.addMergedRegion(new CellRangeAddress(firstRow, lastRow, firstCol, lastCol));
    }

    /**
     * 获取指定 Cell 的 String 内容
     *
     * @param sheet     Sheet
     * @param rowIndex  Row Index
     * @param cellIndex Cell Index
     * @return R of String Value
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
     * 创建一个 Cell
     *
     * @param row       Row
     * @param cellIndex Cell Index
     * @param cellValue Cell Value
     */
    public static void createCell(Row row, int cellIndex, String cellValue) {
        createCellWithStyle(row, cellIndex, cellValue, null);
    }

    /**
     * 创建一个 Cell 并设置样式
     *
     * @param row       Row
     * @param cellIndex Cell Index
     * @param cellValue Cell Value
     * @param cellStyle Cell Style
     */
    public static void createCellWithStyle(Row row, int cellIndex, String cellValue, CellStyle cellStyle) {
        Cell deviceNameCell = row.createCell(cellIndex);
        deviceNameCell.setCellValue(cellValue);

        if (Objects.nonNull(cellStyle)) {
            deviceNameCell.setCellStyle(cellStyle);
        }
    }
}
