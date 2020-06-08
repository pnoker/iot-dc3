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
-- Records of dc3_unit
-- ----------------------------
INSERT INTO "dc3_unit" VALUES (1, '摄氏度', '℃');

-- ----------------------------
-- Records of dc3_wia_gateway
-- ----------------------------
INSERT INTO "dc3_wia_gateway" VALUES (1, '127.0.0.1', NULL, 6000, 6001, 0, '2019-07-31 00:42:14');

-- ----------------------------
-- Records of dc3_wia_device
-- ----------------------------
INSERT INTO "dc3_wia_device" VALUES (1, 1, NULL, 'Wia-Device-Test-01', 0, '2019-07-31 00:42:56');

-- ----------------------------
-- Records of dc3_wia_variable
-- ----------------------------
INSERT INTO "dc3_wia_variable" VALUES (1, 1, '温度', NULL, NULL, NULL, 1, 1.0);

-- ----------------------------
-- Records of dc3_wia_data
-- ----------------------------
INSERT INTO "dc3_wia_data" VALUES (1, 1, 100.0, '2019-07-31 09:44:48');
INSERT INTO "dc3_wia_data" VALUES (2, 1, 100.0, '2019-07-31 09:01:15');

PRAGMA foreign_keys = true;
