/*
  Copyright 2018-2020 Pnoker. All Rights Reserved.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

PRAGMA foreign_keys = false;

-- ----------------------------
-- Table structure for dc3_unit
-- ----------------------------
DROP TABLE IF EXISTS "dc3_unit";
CREATE TABLE "dc3_unit" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "name" TEXT,
  "symbol" text,
  UNIQUE ("id" ASC),
  UNIQUE ("name" ASC)
);

-- ----------------------------
-- Records of dc3_unit
-- ----------------------------
INSERT INTO "dc3_unit" VALUES (1, '摄氏度', '℃');

-- ----------------------------
-- Table structure for dc3_wia_data
-- ----------------------------
DROP TABLE IF EXISTS "dc3_wia_data";
CREATE TABLE "dc3_wia_data" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "variable_id" INTEGER,
  "value" REAL,
  "time" INTEGER DEFAULT (datetime(current_timestamp, 'localtime')),
  FOREIGN KEY ("variable_id") REFERENCES "dc3_wia_variable" ("") ON DELETE NO ACTION ON UPDATE NO ACTION,
  UNIQUE ("id" ASC)
);

-- ----------------------------
-- Records of dc3_wia_data
-- ----------------------------
INSERT INTO "dc3_wia_data" VALUES (1, 1, 100.0, '2019-07-31 09:44:48');
INSERT INTO "dc3_wia_data" VALUES (2, 1, 100.0, '2019-07-31 09:01:15');

-- ----------------------------
-- Table structure for dc3_wia_device
-- ----------------------------
DROP TABLE IF EXISTS "dc3_wia_device";
CREATE TABLE "dc3_wia_device" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "gateway_id" INTEGER,
  "long_address" TEXT,
  "name" TEXT,
  "status" NUMERIC DEFAULT 0,
  "time" NUMERIC DEFAULT (datetime(current_timestamp, 'localtime')),
  FOREIGN KEY ("gateway_id") REFERENCES "dc3_wia_gateway" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  UNIQUE ("id" ASC),
  UNIQUE ("name" ASC)
);

-- ----------------------------
-- Records of dc3_wia_device
-- ----------------------------
INSERT INTO "dc3_wia_device" VALUES (1, 1, NULL, 'Wia-Device-Test-01', 0, '2019-07-31 00:42:56');

-- ----------------------------
-- Table structure for dc3_wia_gateway
-- ----------------------------
DROP TABLE IF EXISTS "dc3_wia_gateway";
CREATE TABLE "dc3_wia_gateway" (
  "id" INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  "ip_address" TEXT,
  "long_address" TEXT,
  "port" INTEGER DEFAULT 6000,
  "local_port" INTEGER,
  "ping" NUMERIC DEFAULT 0,
  "time" NUMERIC DEFAULT (datetime(current_timestamp, 'localtime')),
  UNIQUE ("id" ASC),
  UNIQUE ("ip_address" ASC),
  UNIQUE ("port" ASC)
);

-- ----------------------------
-- Records of dc3_wia_gateway
-- ----------------------------
INSERT INTO "dc3_wia_gateway" VALUES (1, '127.0.0.1', NULL, 6000, 6001, 0, '2019-07-31 00:42:14');

-- ----------------------------
-- Table structure for dc3_wia_variable
-- ----------------------------
DROP TABLE IF EXISTS "dc3_wia_variable";
CREATE TABLE "dc3_wia_variable" (
  "id" INTEGER PRIMARY KEY AUTOINCREMENT,
  "device_id" INTEGER,
  "name" TEXT,
  "start_index" INTEGER,
  "end_ingex" INTEGER,
  "parse_type" TEXT,
  "unit_id" INTEGER,
  "ratio" REAL DEFAULT 1,
  FOREIGN KEY ("device_id") REFERENCES "dc3_wia_device" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  FOREIGN KEY ("unit_id") REFERENCES "dc3_unit" ("id") ON DELETE NO ACTION ON UPDATE NO ACTION,
  UNIQUE ("id" ASC)
);

-- ----------------------------
-- Records of dc3_wia_variable
-- ----------------------------
INSERT INTO "dc3_wia_variable" VALUES (1, 1, '温度', NULL, NULL, NULL, 1, 1.0);

-- ----------------------------
-- Table structure for sqlite_sequence
-- ----------------------------
DROP TABLE IF EXISTS "sqlite_sequence";
CREATE TABLE "sqlite_sequence" (
  "name",
  "seq"
);

-- ----------------------------
-- Records of sqlite_sequence
-- ----------------------------
INSERT INTO "sqlite_sequence" VALUES ('dc3_unit', 1);
INSERT INTO "sqlite_sequence" VALUES ('dc3_wia_data', 1);
INSERT INTO "sqlite_sequence" VALUES ('dc3_wia_gateway', 1);
INSERT INTO "sqlite_sequence" VALUES ('dc3_wia_device', 1);
INSERT INTO "sqlite_sequence" VALUES ('dc3_wia_variable', 1);

-- ----------------------------
-- Auto increment value for dc3_unit
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 1 WHERE name = 'dc3_unit';

-- ----------------------------
-- Auto increment value for dc3_wia_data
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 1 WHERE name = 'dc3_wia_data';

-- ----------------------------
-- Auto increment value for dc3_wia_device
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 1 WHERE name = 'dc3_wia_device';

-- ----------------------------
-- Auto increment value for dc3_wia_gateway
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 1 WHERE name = 'dc3_wia_gateway';

-- ----------------------------
-- Indexes structure for table dc3_wia_gateway
-- ----------------------------
CREATE UNIQUE INDEX "dc3_wia_gateway_local_port_uindex"
ON "dc3_wia_gateway" (
  "local_port" ASC
);

-- ----------------------------
-- Auto increment value for dc3_wia_variable
-- ----------------------------
UPDATE "sqlite_sequence" SET seq = 1 WHERE name = 'dc3_wia_variable';

PRAGMA foreign_keys = true;
