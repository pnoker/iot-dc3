/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

db = db.getSiblingDB('admin');
if (!db.getUser("dc3")) {
    db.createUser({
        user: "dc3",
        pwd: "dc3",
        roles: [{
            role: "readWrite",
            db: "admin"
        }]
    });
}

db = db.getSiblingDB('dc3');

if (!db.getUser("dc3")) {
    db.createUser({
        user: "dc3",
        pwd: "dc3",
        roles: [{
            role: "readWrite",
            db: "dc3"
        }]
    });
}

if (db.createCollection("pointValue")) {
    db.pointValue.ensureIndex({"deviceId": 1}, {name: "point_value_device_id_index", unique: false, background: true});
    db.pointValue.ensureIndex({"pointId": 1}, {name: "point_value_point_id_index", unique: false, background: true});
    db.pointValue.ensureIndex({"originTime": -1}, {name: "point_value_create_time_index", unique: false, background: true});
}

if (db.createCollection("deviceEvent")) {
    db.deviceEvent.ensureIndex({"deviceId": 1}, {name: "device_event_device_id_index", unique: false, background: true});
    db.deviceEvent.ensureIndex({"pointId": 1}, {name: "device_event_point_id_index", unique: false, background: true});
    db.deviceEvent.ensureIndex({"originTime": -1}, {name: "device_event_create_time_index", unique: false, background: true});
}