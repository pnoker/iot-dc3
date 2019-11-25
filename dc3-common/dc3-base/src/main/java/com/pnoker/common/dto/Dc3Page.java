/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * <p>分页数据实体类
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Dc3Page<T> implements Serializable {
    private static final long serialVersionUID = 7903078348107905138L;

    private List<T> records;
    private Long total;
    private Long current;
    private Long size;
    private Long pages;

    public Dc3Page(IPage<T> page) {
        this.records = page.getRecords();
        this.total = page.getTotal();
        this.current = page.getCurrent();
        this.size = page.getSize();
        this.pages = page.getPages();
    }
}
