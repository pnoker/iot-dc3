/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.strategy.factory;

import com.dc3.center.data.strategy.service.SaveStrategyService;
import com.dc3.common.constant.CommonConstant;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Point Value 存储策略工厂
 *
 * @author pnoker
 */
public class SaveStrategyFactory {
    private static final Map<String, SaveStrategyService> savingStrategyServiceMap = new ConcurrentHashMap<>();

    public static List<SaveStrategyService> getAll() {
        return new ArrayList<>(savingStrategyServiceMap.values());
    }

    public static SaveStrategyService get(String name) {
        return savingStrategyServiceMap.get(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY + name);
    }

    public static void put(String name, SaveStrategyService printParamStrategyService) {
        savingStrategyServiceMap.put(CommonConstant.StrategyService.POINT_VALUE_SAVE_STRATEGY + name, printParamStrategyService);
    }
}
