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