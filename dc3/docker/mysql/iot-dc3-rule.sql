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

CREATE TABLE `node_red_flows`
(
    `id`            int NOT NULL AUTO_INCREMENT,
    `flow_id`       varchar(255) DEFAULT NULL COMMENT '规则id',
    `json_data`     text COMMENT '规则数据',
    `flow_label`    varchar(255) DEFAULT NULL COMMENT '规则标签',
    `flow_type`     varchar(255) DEFAULT NULL COMMENT '规则类型',
    `flow_disabled` tinytext COMMENT '是否启用',
    `operate_time`  datetime     DEFAULT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


CREATE TABLE `node_red_credentials`
(
    `id`        int NOT NULL AUTO_INCREMENT,
    `json_data` text,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `node_red_library`
(
    `id`        int NOT NULL AUTO_INCREMENT,
    `type`      varchar(255) DEFAULT NULL,
    `name`      varchar(255) DEFAULT NULL,
    `meta`      varchar(255) DEFAULT NULL,
    `json_data` text,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `node_red_settings`
(
    `id`        int NOT NULL AUTO_INCREMENT,
    `json_data` text,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;