PRAGMA foreign_keys= OFF;
BEGIN TRANSACTION;
CREATE TABLE dc3_unit
(
    id   INTEGER
        primary key autoincrement
        unique,
    name TEXT
        unique
);
INSERT INTO dc3_unit
VALUES (1, '吨');
CREATE TABLE IF NOT EXISTS "dc3_wia_variable"
(
    id        INTEGER
        primary key autoincrement
        unique,
    device_id INTEGER
        references dc3_wia_device,
    unit_id   INTEGER
        constraint dc3_wia_variable_unit_id_fk
            references dc3_unit,
    name      TEXT,
    ratio     REAL default 1
);
INSERT INTO dc3_wia_variable
VALUES (1, 1, 1, '累积量', 1.0);
CREATE TABLE IF NOT EXISTS "dc3_wia_data"
(
    id          INTEGER
        primary key autoincrement
        unique,
    variable_id INTEGER
        references dc3_wia_variable,
    value       REAL,
    time        integer default (datetime(current_timestamp, 'localtime'))
);
INSERT INTO dc3_wia_data
VALUES (1, 1, 100.0, '2019-07-31 09:44:48');
INSERT INTO dc3_wia_data
VALUES (2, 1, 100.0, '2019-07-31 09:01:15');
CREATE TABLE IF NOT EXISTS "dc3_wia_device"
(
    id         INTEGER
        primary key autoincrement
        unique,
    gateway_id INTEGER
        references dc3_wia_gateway,
    name       TEXT
        unique,
    status     NUMERIC default 0,
    time       NUMERIC default (datetime(current_timestamp, 'localtime'))
);
INSERT INTO dc3_wia_device
VALUES (1, 1, 'TestDevice', 0, '2019-07-31 00:42:56');
CREATE TABLE IF NOT EXISTS "dc3_wia_gateway"
(
    id         INTEGER
        primary key autoincrement
        unique,
    ip_address TEXT
        unique,
    port       INTEGER default 6000
        unique,
    ping       NUMERIC default 0,
    time       NUMERIC default (datetime(current_timestamp, 'localtime'))
);
INSERT INTO dc3_wia_gateway
VALUES (1, '127.0.0.1', 6001, 0, '2019-07-31 00:42:14');
DELETE
FROM sqlite_sequence;
INSERT INTO sqlite_sequence
VALUES ('dc3_unit', 1);
INSERT INTO sqlite_sequence
VALUES ('dc3_wia_variable', 1);
INSERT INTO sqlite_sequence
VALUES ('dc3_wia_data', 2);
INSERT INTO sqlite_sequence
VALUES ('dc3_wia_device', 1);
INSERT INTO sqlite_sequence
VALUES ('dc3_wia_gateway', 1);
COMMIT;
