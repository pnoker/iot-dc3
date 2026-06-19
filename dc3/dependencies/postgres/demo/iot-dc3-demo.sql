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

/*
 * IoT DC3 demo dataset.
 *
 * Apply after the base seed files in this directory. The script is
 * repeatable: it deletes the fixed demo ID range first, then inserts a
 * realistic plant telemetry scenario used by the home dashboard, event
 * dashboard, entity lists, detail pages, and screenshot automation.
 *
 * ID ranges:
 *   861000000000-861999999999 drivers
 *   862000000000-862999999999 devices
 *   863000000000-863999999999 profiles
 *   864000000000-864999999999 points
 *   865000000000-869999999999 binds, attributes, rules, events
 */

BEGIN;

DO
    $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'dc3_manager') THEN
        RAISE EXCEPTION 'dc3_manager schema is missing. Run iot-dc3-manager.sql first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'dc3_data') THEN
        RAISE EXCEPTION 'dc3_data schema is missing. Run iot-dc3-data.sql first.';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_namespace WHERE nspname = 'dc3_history') THEN
        RAISE EXCEPTION 'dc3_history schema is missing. Run iot-dc3-history.sql first.';
    END IF;
END;
$$;

-- ---------------------------------------------------------------------
-- Clean previous demo rows
-- ---------------------------------------------------------------------
DELETE
FROM dc3_history.dc3_point_value
WHERE tenant_id = 1
  AND driver_id BETWEEN 861000000000 AND 861999999999;

DELETE
FROM dc3_data.dc3_entity_alarm
WHERE tenant_id = 1
  AND id BETWEEN 868000000000 AND 868999999999;

DELETE
FROM dc3_data.dc3_notify_history
WHERE tenant_id = 1
  AND id BETWEEN 867200000000 AND 867299999999;

DELETE
FROM dc3_data.dc3_rule_state
WHERE tenant_id = 1
  AND id BETWEEN 867100000000 AND 867199999999;

DELETE
FROM dc3_data.dc3_rule
WHERE tenant_id = 1
  AND id BETWEEN 867000000000 AND 867999999999;

DELETE
FROM dc3_data.dc3_message
WHERE tenant_id = 1
  AND id BETWEEN 866500000000 AND 866599999999;

DELETE
FROM dc3_data.dc3_notify_channel_bind
WHERE tenant_id = 1
  AND id BETWEEN 866200000000 AND 866299999999;

DELETE
FROM dc3_data.dc3_notify_channel
WHERE tenant_id = 1
  AND id BETWEEN 866100000000 AND 866199999999;

DELETE
FROM dc3_data.dc3_notify
WHERE tenant_id = 1
  AND id BETWEEN 866000000000 AND 866099999999;

DELETE
FROM dc3_manager.dc3_point_attribute_config
WHERE tenant_id = 1
  AND id BETWEEN 866800000000 AND 866899999999;

DELETE
FROM dc3_manager.dc3_driver_attribute_config
WHERE tenant_id = 1
  AND id BETWEEN 866700000000 AND 866799999999;

DELETE
FROM dc3_manager.dc3_group_bind
WHERE tenant_id = 1
  AND id BETWEEN 865700000000 AND 865799999999;

DELETE
FROM dc3_manager.dc3_label_bind
WHERE tenant_id = 1
  AND id BETWEEN 865800000000 AND 865899999999;

DELETE
FROM dc3_manager.dc3_point
WHERE tenant_id = 1
  AND id BETWEEN 864000000000 AND 864999999999;

DELETE
FROM dc3_manager.dc3_device
WHERE tenant_id = 1
  AND id BETWEEN 862000000000 AND 862999999999;

DELETE
FROM dc3_manager.dc3_profile
WHERE tenant_id = 1
  AND id BETWEEN 863000000000 AND 863999999999;

DELETE
FROM dc3_manager.dc3_point_attribute
WHERE tenant_id = 1
  AND id BETWEEN 866300000000 AND 866399999999;

DELETE
FROM dc3_manager.dc3_driver_attribute
WHERE tenant_id = 1
  AND id BETWEEN 866200000000 AND 866299999999;

DELETE
FROM dc3_manager.dc3_group
WHERE tenant_id = 1
  AND id BETWEEN 865500000000 AND 865599999999;

DELETE
FROM dc3_manager.dc3_label
WHERE tenant_id = 1
  AND id BETWEEN 865600000000 AND 865699999999;

DELETE
FROM dc3_manager.dc3_driver
WHERE tenant_id = 1
  AND id BETWEEN 861000000000 AND 861999999999;

-- ---------------------------------------------------------------------
-- Manager domain: drivers, profiles, devices, points
-- ---------------------------------------------------------------------
INSERT INTO dc3_manager.dc3_driver (id, driver_name, driver_code, service_name, service_host, driver_type_flag,
                                    driver_ext, enable_flag, tenant_id, remark, signature, version,
                                    creator_id, creator_name, create_time, operator_id, operator_name, operate_time,
                                    deleted)
VALUES (861000000001, 'Modbus TCP - Pudong Cooling PLC', 'demo.modbus.pudong.chw', 'dc3-driver-modbus-tcp',
        '10.32.14.21:8600', 0,
        '{"site":"Shanghai Pudong Campus","protocol":"Modbus TCP","rack":"PLC-CW-A","sla":"99.95"}', 0, 1,
        'Chilled-water pump and valve telemetry for the Pudong plant.', 'demo', 1, 1, 'dc3', now() - interval '26 days',
        1, 'dc3', now() - interval '12 minutes', 0),
       (861000000002, 'OPC UA - Suzhou CIP Gateway', 'demo.opcua.suzhou.cip', 'dc3-driver-opc-ua', '10.45.8.12:4840', 0,
        '{"site":"Suzhou Process Hall","protocol":"OPC UA","endpoint":"opc.tcp://10.45.8.12:4840","sla":"99.90"}', 0, 1,
        'Clean-in-place tanks, dosing skid, and steam valve telemetry.', 'demo', 1, 1, 'dc3',
        now() - interval '24 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (861000000003, 'MQTT - Campus Energy Broker', 'demo.mqtt.energy', 'dc3-driver-mqtt', '10.18.6.33:1883', 1,
        '{"site":"Campus Energy Center","protocol":"MQTT","topicPrefix":"dc3/demo/energy","qos":1}', 0, 1,
        'Tenant-wide electrical meter stream from the energy broker.', 'demo', 1, 1, 'dc3', now() - interval '21 days',
        1, 'dc3', now() - interval '4 minutes', 0),
       (861000000004, 'S7 - Packaging Line 3', 'demo.s7.packaging.l3', 'dc3-driver-plcs7', '10.61.3.44:102', 0,
        '{"site":"Hangzhou Packaging Line","protocol":"S7","rack":0,"slot":2,"line":"L3"}', 0, 1,
        'High-speed packaging conveyor and reject-station telemetry.', 'demo', 1, 1, 'dc3', now() - interval '18 days',
        1, 'dc3', now() - interval '18 minutes', 0),
       (861000000005, 'Virtual - Edge Acceptance Lab', 'demo.virtual.edge.lab', 'dc3-driver-virtual', '127.0.0.1:8700',
        2,
        '{"site":"Edge Acceptance Lab","protocol":"Virtual","purpose":"demo baseline and synthetic regression"}', 0, 1,
        'Synthetic edge lab signal source for demos and regression screenshots.', 'demo', 1, 1, 'dc3',
        now() - interval '15 days', 1, 'dc3', now() - interval '6 minutes', 0);

INSERT INTO dc3_manager.dc3_profile (id, profile_name, profile_code, profile_share_flag, profile_type_flag, profile_ext,
                                     enable_flag, tenant_id, remark, signature, version,
                                     creator_id, creator_name, create_time, operator_id, operator_name, operate_time,
                                     deleted)
VALUES (863000000001, 'Chilled Water Pump Profile', 'demo.profile.chw.pump', 0, 2,
        '{"domain":"HVAC","assetClass":"pump","expectedSampleSec":10}', 0, 1,
        'Pressure, temperature, current, vibration, status, and fault points for chilled-water pumps.', 'demo', 1, 1,
        'dc3', now() - interval '25 days', 1, 'dc3', now() - interval '11 minutes', 0),
       (863000000002, 'Air Handler Profile', 'demo.profile.air.handler', 0, 2,
        '{"domain":"HVAC","assetClass":"ahu","expectedSampleSec":15}', 0, 1,
        'AHU telemetry for temperature, humidity, filter pressure, fan, damper, and CO2.', 'demo', 1, 1, 'dc3',
        now() - interval '22 days', 1, 'dc3', now() - interval '13 minutes', 0),
       (863000000003, 'Energy Meter Profile', 'demo.profile.energy.meter', 0, 2,
        '{"domain":"Energy","assetClass":"meter","expectedSampleSec":30}', 0, 1,
        'Electrical meter profile used by feeder and building submeters.', 'demo', 1, 1, 'dc3',
        now() - interval '20 days', 1, 'dc3', now() - interval '5 minutes', 0),
       (863000000004, 'Packaging Line Profile', 'demo.profile.packaging.line', 0, 2,
        '{"domain":"Manufacturing","assetClass":"line","expectedSampleSec":10}', 0, 1,
        'Line speed, torque, count, reject, line state, and emergency-stop telemetry.', 'demo', 1, 1, 'dc3',
        now() - interval '17 days', 1, 'dc3', now() - interval '19 minutes', 0),
       (863000000005, 'Environmental Probe Profile', 'demo.profile.environment.probe', 0, 2,
        '{"domain":"Facility","assetClass":"probe","expectedSampleSec":60}', 0, 1,
        'Battery-backed environmental probes for warehouses and edge rooms.', 'demo', 1, 1, 'dc3',
        now() - interval '16 days', 1, 'dc3', now() - interval '16 minutes', 0),
       (863000000006, 'CIP Skid Profile', 'demo.profile.cip.skid', 0, 2,
        '{"domain":"Process","assetClass":"cip","expectedSampleSec":10}', 0, 1,
        'CIP skid level, conductivity, pH, flow, valve, and recipe-step telemetry.', 'demo', 1, 1, 'dc3',
        now() - interval '14 days', 1, 'dc3', now() - interval '7 minutes', 0);

INSERT INTO dc3_manager.dc3_device (id, device_name, device_code, driver_id, profile_id, device_ext, enable_flag,
                                    tenant_id,
                                    remark, signature, version, creator_id, creator_name, create_time, operator_id,
                                    operator_name, operate_time, deleted)
VALUES (862000000001, 'PD-CHW-PUMP-01', 'demo.device.pd.chw.pump.01', 861000000001, 863000000001,
        '{"site":"Shanghai Pudong Campus","area":"Cooling Plant","floor":"B1","assetNo":"PD-CW-P-001"}', 0, 1,
        'Primary chilled-water pump for office tower A.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '12 minutes', 0),
       (862000000002, 'PD-CHW-PUMP-02', 'demo.device.pd.chw.pump.02', 861000000001, 863000000001,
        '{"site":"Shanghai Pudong Campus","area":"Cooling Plant","floor":"B1","assetNo":"PD-CW-P-002"}', 0, 1,
        'Standby chilled-water pump for office tower A.', 'demo', 1, 1, 'dc3', now() - interval '24 days', 1, 'dc3',
        now() - interval '10 minutes', 0),
       (862000000003, 'PD-AHU-7F-EAST', 'demo.device.pd.ahu.7f.east', 861000000001, 863000000002,
        '{"site":"Shanghai Pudong Campus","area":"Tower A","floor":"7F","assetNo":"PD-AHU-7E"}', 0, 1,
        'East-zone AHU serving 7F office open area.', 'demo', 1, 1, 'dc3', now() - interval '23 days', 1, 'dc3',
        now() - interval '9 minutes', 0),
       (862000000004, 'PD-AHU-9F-WEST', 'demo.device.pd.ahu.9f.west', 861000000001, 863000000002,
        '{"site":"Shanghai Pudong Campus","area":"Tower A","floor":"9F","assetNo":"PD-AHU-9W"}', 0, 1,
        'West-zone AHU serving 9F meeting rooms.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1, 'dc3',
        now() - interval '15 minutes', 0),
       (862000000005, 'SZ-CIP-SKID-01', 'demo.device.sz.cip.skid.01', 861000000002, 863000000006,
        '{"site":"Suzhou Process Hall","area":"CIP Room","line":"Filling A","assetNo":"SZ-CIP-001"}', 0, 1,
        'CIP skid for filling line A.', 'demo', 1, 1, 'dc3', now() - interval '21 days', 1, 'dc3',
        now() - interval '7 minutes', 0),
       (862000000006, 'SZ-CIP-SKID-02', 'demo.device.sz.cip.skid.02', 861000000002, 863000000006,
        '{"site":"Suzhou Process Hall","area":"CIP Room","line":"Filling B","assetNo":"SZ-CIP-002"}', 0, 1,
        'CIP skid for filling line B.', 'demo', 1, 1, 'dc3', now() - interval '21 days', 1, 'dc3',
        now() - interval '14 minutes', 0),
       (862000000007, 'SZ-TANK-FARM-MTR-01', 'demo.device.sz.tank.meter.01', 861000000003, 863000000003,
        '{"site":"Suzhou Process Hall","area":"Tank Farm","feeder":"TF-MDB-01"}', 0, 1,
        'Tank-farm low-voltage feeder meter.', 'demo', 1, 1, 'dc3', now() - interval '19 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (862000000008, 'SH-HQ-MAIN-MTR-01', 'demo.device.sh.hq.main.meter.01', 861000000003, 863000000003,
        '{"site":"Shanghai HQ","area":"Main Switch Room","feeder":"HQ-MDB-01"}', 0, 1,
        'Main incoming energy meter for HQ campus.', 'demo', 1, 1, 'dc3', now() - interval '18 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (862000000009, 'HZ-PACK-L3-PLC', 'demo.device.hz.pack.l3.plc', 861000000004, 863000000004,
        '{"site":"Hangzhou Packaging","area":"Line 3","assetNo":"HZ-L3-PLC"}', 0, 1, 'Packaging line 3 conveyor PLC.',
        'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3', now() - interval '18 minutes', 0),
       (862000000010, 'HZ-PACK-L3-CHECKWEIGHER', 'demo.device.hz.pack.l3.checkweigher', 861000000004, 863000000004,
        '{"site":"Hangzhou Packaging","area":"Line 3","station":"Checkweigher"}', 0, 1,
        'Checkweigher and reject station telemetry.', 'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '21 minutes', 0),
       (862000000011, 'NJ-WH-ENV-01', 'demo.device.nj.wh.env.01', 861000000005, 863000000005,
        '{"site":"Nanjing Warehouse","area":"Cold Room A","assetNo":"NJ-ENV-001"}', 0, 1,
        'Cold-room environmental probe near inbound dock.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '26 minutes', 0),
       (862000000012, 'NJ-WH-ENV-02', 'demo.device.nj.wh.env.02', 861000000005, 863000000005,
        '{"site":"Nanjing Warehouse","area":"Cold Room B","assetNo":"NJ-ENV-002"}', 0, 1,
        'Cold-room environmental probe near outbound dock.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '9 minutes', 0),
       (862000000013, 'EDGE-LAB-SIM-01', 'demo.device.edge.sim.01', 861000000005, 863000000005,
        '{"site":"Edge Acceptance Lab","area":"Rack R12","assetNo":"EDGE-SIM-001"}', 0, 1,
        'Synthetic mixed-signal edge lab device.', 'demo', 1, 1, 'dc3', now() - interval '15 days', 1, 'dc3',
        now() - interval '3 minutes', 0),
       (862000000014, 'EDGE-LAB-SIM-02', 'demo.device.edge.sim.02', 861000000005, 863000000005,
        '{"site":"Edge Acceptance Lab","area":"Rack R13","assetNo":"EDGE-SIM-002"}', 0, 1,
        'Synthetic high-volume regression source.', 'demo', 1, 1, 'dc3', now() - interval '15 days', 1, 'dc3',
        now() - interval '6 minutes', 0),
       (862000000015, 'PD-ROOF-AHU-01', 'demo.device.pd.roof.ahu.01', 861000000001, 863000000002,
        '{"site":"Shanghai Pudong Campus","area":"Roof","assetNo":"PD-AHU-R01"}', 0, 1,
        'Roof fresh-air AHU with filter pressure monitoring.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1,
        'dc3', now() - interval '22 minutes', 0),
       (862000000016, 'HZ-PACK-L3-VISION', 'demo.device.hz.pack.l3.vision', 861000000004, 863000000004,
        '{"site":"Hangzhou Packaging","area":"Line 3","station":"Vision"}', 1, 1,
        'Vision station under maintenance after lens replacement.', 'demo', 1, 1, 'dc3', now() - interval '13 days', 1,
        'dc3', now() - interval '2 hours', 0);

INSERT INTO dc3_manager.dc3_point (id, point_name, point_code, point_type_flag, rw_flag, base_value, multiple,
                                   value_decimal, unit,
                                   profile_id, point_ext, enable_flag, tenant_id, remark, signature, version,
                                   creator_id, creator_name, create_time, operator_id, operator_name, operate_time,
                                   deleted)
VALUES (864000000001, 'Supply Pressure', 'supply_pressure', 6, 0, 0, 1, 2, 'kPa', 863000000001, '{"normal":"260-340"}',
        0, 1, 'Pump discharge pressure after VFD ramp.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '11 minutes', 0),
       (864000000002, 'Return Temperature', 'return_temperature', 6, 0, 0, 1, 2, 'C', 863000000001, '{"normal":"8-15"}',
        0, 1, 'Return-water temperature from secondary loop.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1,
        'dc3', now() - interval '11 minutes', 0),
       (864000000003, 'Motor Current', 'motor_current', 6, 0, 0, 1, 2, 'A', 863000000001, '{"normal":"35-95"}', 0, 1,
        'Three-phase motor current average.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '11 minutes', 0),
       (864000000004, 'Vibration RMS', 'vibration_rms', 5, 0, 0, 1, 3, 'mm/s', 863000000001, '{"alarmHigh":4.5}', 0, 1,
        'Bearing vibration RMS from pump DE side.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '11 minutes', 0),
       (864000000005, 'Run Status', 'run_status', 7, 0, 0, 1, 0, '', 863000000001,
        '{"true":"running","false":"stopped"}', 0, 1, 'Pump running feedback.', 'demo', 1, 1, 'dc3',
        now() - interval '25 days', 1, 'dc3', now() - interval '11 minutes', 0),
       (864000000006, 'Fault Code', 'fault_code', 3, 0, 0, 1, 0, 'code', 863000000001, '{"0":"normal"}', 0, 1,
        'VFD fault code.', 'demo', 1, 1, 'dc3', now() - interval '25 days', 1, 'dc3', now() - interval '11 minutes', 0),
       (864000000011, 'Supply Air Temperature', 'supply_air_temperature', 6, 0, 0, 1, 2, 'C', 863000000002,
        '{"normal":"17-22"}', 0, 1, 'AHU supply air temperature.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1,
        'dc3', now() - interval '13 minutes', 0),
       (864000000012, 'Return Air Humidity', 'return_air_humidity', 6, 0, 0, 1, 1, '%RH', 863000000002,
        '{"normal":"35-65"}', 0, 1, 'Return air relative humidity.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1,
        'dc3', now() - interval '13 minutes', 0),
       (864000000013, 'Filter Differential Pressure', 'filter_dp', 6, 0, 0, 1, 1, 'Pa', 863000000002,
        '{"alarmHigh":280}', 0, 1, 'AHU filter differential pressure.', 'demo', 1, 1, 'dc3', now() - interval '22 days',
        1, 'dc3', now() - interval '13 minutes', 0),
       (864000000014, 'Fan Speed', 'fan_speed', 6, 2, 0, 1, 1, '%', 863000000002, '{"normal":"35-90"}', 0, 1,
        'Supply fan VFD speed command and feedback.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1, 'dc3',
        now() - interval '13 minutes', 0),
       (864000000015, 'Damper Command', 'damper_command', 6, 2, 0, 1, 1, '%', 863000000002, '{"normal":"20-100"}', 0, 1,
        'Fresh-air damper command.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1, 'dc3',
        now() - interval '13 minutes', 0),
       (864000000016, 'CO2 Concentration', 'co2', 6, 0, 0, 1, 0, 'ppm', 863000000002, '{"alarmHigh":1000}', 0, 1,
        'Return air CO2 concentration.', 'demo', 1, 1, 'dc3', now() - interval '22 days', 1, 'dc3',
        now() - interval '13 minutes', 0),
       (864000000021, 'Active Power', 'active_power', 6, 0, 0, 1, 2, 'kW', 863000000003, '{"normal":"20-850"}', 0, 1,
        'Three-phase active power.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (864000000022, 'Total Energy', 'total_energy', 6, 0, 0, 1, 1, 'kWh', 863000000003, '{"counter":true}', 0, 1,
        'Forward active energy counter.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (864000000023, 'Phase A Voltage', 'phase_a_voltage', 6, 0, 0, 1, 1, 'V', 863000000003, '{"normal":"214-238"}', 0,
        1, 'Phase A RMS voltage.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (864000000024, 'Phase A Current', 'phase_a_current', 6, 0, 0, 1, 2, 'A', 863000000003, '{"normal":"10-900"}', 0,
        1, 'Phase A RMS current.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (864000000025, 'Power Factor', 'power_factor', 6, 0, 0, 1, 3, '', 863000000003, '{"normal":"0.88-1.00"}', 0, 1,
        'Average power factor.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '5 minutes', 0),
       (864000000026, 'Frequency', 'frequency', 6, 0, 0, 1, 2, 'Hz', 863000000003, '{"normal":"49.8-50.2"}', 0, 1,
        'Grid frequency.', 'demo', 1, 1, 'dc3', now() - interval '20 days', 1, 'dc3', now() - interval '5 minutes', 0),
       (864000000031, 'Conveyor Speed', 'conveyor_speed', 6, 2, 0, 1, 1, 'm/min', 863000000004, '{"normal":"35-68"}', 0,
        1, 'Packaging conveyor line speed.', 'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '19 minutes', 0),
       (864000000032, 'Motor Torque', 'motor_torque', 6, 0, 0, 1, 1, '%', 863000000004, '{"alarmHigh":85}', 0, 1,
        'Main conveyor motor torque.', 'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '19 minutes', 0),
       (864000000033, 'Product Count', 'product_count', 4, 0, 0, 1, 0, 'pcs', 863000000004, '{"counter":true}', 0, 1,
        'Shift product count.', 'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '19 minutes', 0),
       (864000000034, 'Reject Count', 'reject_count', 3, 0, 0, 1, 0, 'pcs', 863000000004, '{"counter":true}', 0, 1,
        'Vision and checkweigher reject count.', 'demo', 1, 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '19 minutes', 0),
       (864000000035, 'Line State', 'line_state', 0, 0, 0, 1, 0, '', 863000000004,
        '{"values":["RUNNING","STARVED","BLOCKED","MAINT"]}', 0, 1, 'Packaging line state text.', 'demo', 1, 1, 'dc3',
        now() - interval '17 days', 1, 'dc3', now() - interval '19 minutes', 0),
       (864000000036, 'Emergency Stop', 'emergency_stop', 7, 0, 0, 1, 0, '', 863000000004,
        '{"true":"pressed","false":"released"}', 0, 1, 'Emergency-stop safety relay state.', 'demo', 1, 1, 'dc3',
        now() - interval '17 days', 1, 'dc3', now() - interval '19 minutes', 0),
       (864000000041, 'Ambient Temperature', 'ambient_temperature', 6, 0, 0, 1, 2, 'C', 863000000005,
        '{"normal":"2-8 cold room, 18-28 lab"}', 0, 1, 'Probe ambient temperature.', 'demo', 1, 1, 'dc3',
        now() - interval '16 days', 1, 'dc3', now() - interval '16 minutes', 0),
       (864000000042, 'Relative Humidity', 'relative_humidity', 6, 0, 0, 1, 1, '%RH', 863000000005,
        '{"normal":"35-70"}', 0, 1, 'Probe relative humidity.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1,
        'dc3', now() - interval '16 minutes', 0),
       (864000000043, 'PM2.5', 'pm25', 6, 0, 0, 1, 1, 'ug/m3', 863000000005, '{"normal":"0-35"}', 0, 1,
        'Fine particulate concentration.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '16 minutes', 0),
       (864000000044, 'Noise Level', 'noise_level', 6, 0, 0, 1, 1, 'dBA', 863000000005, '{"normal":"35-70"}', 0, 1,
        'A-weighted sound pressure level.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '16 minutes', 0),
       (864000000045, 'Battery Level', 'battery_level', 6, 0, 0, 1, 1, '%', 863000000005, '{"alarmLow":25}', 0, 1,
        'Probe battery charge level.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '16 minutes', 0),
       (864000000046, 'RSSI', 'rssi', 6, 0, 0, 1, 1, 'dBm', 863000000005, '{"alarmLow":-85}', 0, 1,
        'Wireless signal strength.', 'demo', 1, 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '16 minutes', 0),
       (864000000051, 'Tank Level', 'tank_level', 6, 0, 0, 1, 1, '%', 863000000006, '{"normal":"15-92"}', 0, 1,
        'CIP tank level.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3', now() - interval '7 minutes', 0),
       (864000000052, 'Conductivity', 'conductivity', 6, 0, 0, 1, 2, 'mS/cm', 863000000006, '{"normal":"0.5-32"}', 0, 1,
        'Cleaning solution conductivity.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3',
        now() - interval '7 minutes', 0),
       (864000000053, 'pH', 'ph', 6, 0, 0, 1, 2, 'pH', 863000000006, '{"normal":"6.5-12.5"}', 0, 1,
        'Cleaning solution pH.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3',
        now() - interval '7 minutes', 0),
       (864000000054, 'Flow Rate', 'flow_rate', 6, 0, 0, 1, 1, 'm3/h', 863000000006, '{"normal":"8-28"}', 0, 1,
        'CIP circulation flow.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3',
        now() - interval '7 minutes', 0),
       (864000000055, 'Steam Valve', 'steam_valve', 6, 2, 0, 1, 1, '%', 863000000006, '{"normal":"0-100"}', 0, 1,
        'Steam valve opening command.', 'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3',
        now() - interval '7 minutes', 0),
       (864000000056, 'Recipe Step', 'recipe_step', 3, 0, 0, 1, 0, 'step', 863000000006,
        '{"0":"idle","1":"pre-rinse","2":"caustic","3":"acid","4":"final-rinse"}', 0, 1, 'Current CIP recipe step.',
        'demo', 1, 1, 'dc3', now() - interval '14 days', 1, 'dc3', now() - interval '7 minutes', 0);

-- Commissioning points intentionally have no history rows yet. They make
-- the event availability dashboard's coverage-gap card look like a real
-- tenant with recently configured but not-yet-reporting telemetry.
INSERT INTO dc3_manager.dc3_point (id, point_name, point_code, point_type_flag, rw_flag, base_value, multiple,
                                   value_decimal, unit,
                                   profile_id, point_ext, enable_flag, tenant_id, remark, signature, version,
                                   creator_id, creator_name, create_time, operator_id, operator_name, operate_time,
                                   deleted)
VALUES (864000000101, 'Seal Flush Pressure', 'seal_flush_pressure', 6, 0, 0, 1, 2, 'kPa', 863000000001,
        '{"commissioning":true,"normal":"120-180"}', 0, 1, 'New seal-flush pressure point pending PLC mapping.', 'demo',
        1, 1, 'dc3', now() - interval '3 days', 1, 'dc3', now() - interval '21 minutes', 0),
       (864000000102, 'Coil Valve Feedback', 'coil_valve_feedback', 6, 0, 0, 1, 1, '%', 863000000002,
        '{"commissioning":true,"normal":"0-100"}', 0, 1, 'AHU cooling-coil valve feedback point awaiting BMS publish.',
        'demo', 1, 1, 'dc3', now() - interval '3 days', 1, 'dc3', now() - interval '19 minutes', 0),
       (864000000103, 'Voltage THD', 'voltage_thd', 6, 0, 0, 1, 2, '%', 863000000003,
        '{"commissioning":true,"alarmHigh":5}', 0, 1, 'Power quality THD point planned for meter firmware upgrade.',
        'demo', 1, 1, 'dc3', now() - interval '2 days', 1, 'dc3', now() - interval '18 minutes', 0),
       (864000000104, 'Reject Gate Position', 'reject_gate_position', 6, 0, 0, 1, 1, 'deg', 863000000004,
        '{"commissioning":true,"normal":"0-90"}', 0, 1, 'Reject gate position feedback not wired in PLC yet.', 'demo',
        1, 1, 'dc3', now() - interval '2 days', 1, 'dc3', now() - interval '16 minutes', 0),
       (864000000105, 'Door Contact', 'door_contact', 7, 0, 0, 1, 0, '', 863000000005,
        '{"commissioning":true,"true":"closed","false":"open"}', 0, 1,
        'Cold-room door contact point awaiting wireless enrollment.', 'demo', 1, 1, 'dc3', now() - interval '2 days', 1,
        'dc3', now() - interval '14 minutes', 0),
       (864000000106, 'Return Turbidity', 'return_turbidity', 6, 0, 0, 1, 2, 'NTU', 863000000006,
        '{"commissioning":true,"alarmHigh":12}', 0, 1, 'CIP return turbidity point pending analyzer calibration.',
        'demo', 1, 1, 'dc3', now() - interval '2 days', 1, 'dc3', now() - interval '12 minutes', 0);

-- ---------------------------------------------------------------------
-- Labels and groups make list pages look like a populated tenant.
-- ---------------------------------------------------------------------
INSERT INTO dc3_manager.dc3_group (id, parent_group_id, group_name, group_code, group_level, group_index,
                                   entity_type_flag,
                                   enable_flag, tenant_id, remark, creator_id, creator_name, create_time, operator_id,
                                   operator_name, operate_time, deleted)
VALUES (865500000001, 0, 'Shanghai Pudong Campus', 'demo.group.site.pudong', 1, 1, 6, 0, 1,
        'Device group for Pudong campus HVAC assets.', 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865500000002, 0, 'Suzhou Process Hall', 'demo.group.site.suzhou', 1, 2, 6, 0, 1,
        'Device group for Suzhou process and CIP assets.', 1, 'dc3', now() - interval '23 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865500000003, 0, 'Hangzhou Packaging', 'demo.group.site.hangzhou', 1, 3, 6, 0, 1,
        'Device group for packaging line telemetry.', 1, 'dc3', now() - interval '18 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865500000004, 0, 'Nanjing Warehouse', 'demo.group.site.nanjing', 1, 4, 6, 0, 1,
        'Device group for cold-room environmental probes.', 1, 'dc3', now() - interval '16 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865500000005, 0, 'Edge Acceptance Lab', 'demo.group.site.edge.lab', 1, 5, 6, 0, 1,
        'Synthetic devices used by demo and screenshot automation.', 1, 'dc3', now() - interval '15 days', 1, 'dc3',
        now() - interval '8 minutes', 0);

INSERT INTO dc3_manager.dc3_label (id, label_name, label_code, label_color, entity_type_flag, enable_flag, tenant_id,
                                   remark, creator_id, creator_name, create_time, operator_id, operator_name,
                                   operate_time, deleted)
VALUES (865600000001, 'Critical HVAC', 'demo.label.critical.hvac', '#F56C6C', 6, 0, 1,
        'Critical cooling and air-handling devices.', 1, 'dc3', now() - interval '25 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865600000002, 'Energy Metering', 'demo.label.energy.metering', '#409EFF', 6, 0, 1,
        'Devices feeding energy dashboards.', 1, 'dc3', now() - interval '20 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865600000003, 'SLA Watch', 'demo.label.sla.watch', '#E6A23C', 6, 0, 1, 'Assets with active SLA watch.', 1,
        'dc3', now() - interval '18 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (865600000004, 'Process Quality', 'demo.label.process.quality', '#67C23A', 6, 0, 1,
        'Quality-critical process devices.', 1, 'dc3', now() - interval '17 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (865600000005, 'Wireless Probe', 'demo.label.wireless.probe', '#909399', 6, 0, 1,
        'Battery-backed wireless probes.', 1, 'dc3', now() - interval '16 days', 1, 'dc3', now() - interval '8 minutes',
        0);

INSERT INTO dc3_manager.dc3_group_bind (id, entity_type_flag, group_id, entity_id, tenant_id, remark, creator_id,
                                        creator_name, create_time, operator_id, operator_name, operate_time, deleted)
SELECT 865700000000 + row_number() OVER (),
       6,
       group_id,
       device_id,
       1,
       'Demo device group binding.',
       1,
       'dc3',
       now() - interval '12 days',
       1,
       'dc3',
       now() - interval '8 minutes',
       0
FROM (VALUES (865500000001, 862000000001),
             (865500000001, 862000000002),
             (865500000001, 862000000003),
             (865500000001, 862000000004),
             (865500000001, 862000000015),
             (865500000002, 862000000005),
             (865500000002, 862000000006),
             (865500000002, 862000000007),
             (865500000003, 862000000009),
             (865500000003, 862000000010),
             (865500000003, 862000000016),
             (865500000004, 862000000011),
             (865500000004, 862000000012),
             (865500000005, 862000000013),
             (865500000005, 862000000014),
             (865500000001, 862000000008)) AS t(group_id, device_id);

INSERT INTO dc3_manager.dc3_label_bind (id, entity_type_flag, label_id, entity_id, tenant_id, remark, creator_id,
                                        creator_name, create_time, operator_id, operator_name, operate_time, deleted)
SELECT 865800000000 + row_number() OVER (),
       6,
       label_id,
       device_id,
       1,
       'Demo device label binding.',
       1,
       'dc3',
       now() - interval '12 days',
       1,
       'dc3',
       now() - interval '8 minutes',
       0
FROM (VALUES (865600000001, 862000000001),
             (865600000001, 862000000002),
             (865600000001, 862000000003),
             (865600000001, 862000000004),
             (865600000001, 862000000015),
             (865600000002, 862000000007),
             (865600000002, 862000000008),
             (865600000002, 862000000013),
             (865600000003, 862000000005),
             (865600000003, 862000000006),
             (865600000003, 862000000009),
             (865600000003, 862000000010),
             (865600000004, 862000000005),
             (865600000004, 862000000006),
             (865600000004, 862000000009),
             (865600000004, 862000000010),
             (865600000004, 862000000016),
             (865600000005, 862000000011),
             (865600000005, 862000000012),
             (865600000005, 862000000013),
             (865600000005, 862000000014)) AS t(label_id, device_id);

-- ---------------------------------------------------------------------
-- Driver and point attributes for the device edit workflow.
-- ---------------------------------------------------------------------
INSERT INTO dc3_manager.dc3_driver_attribute (id, attribute_name, attribute_code, attribute_type_flag, default_value,
                                              driver_id, attribute_ext,
                                              enable_flag, tenant_id, remark, signature, version, creator_id,
                                              creator_name, create_time, operator_id, operator_name, operate_time,
                                              deleted)
SELECT 866200000000 + row_number() OVER (),
       attribute_name,
       attribute_code,
       attribute_type_flag,
       default_value,
       driver_id,
       attribute_ext::json, 0,
       1,
       remark,
       'demo',
       1,
       1,
       'dc3',
       now() - interval '12 days',
       1,
       'dc3',
       now() - interval '8 minutes',
       0
FROM (VALUES (861000000001, 'Modbus Unit ID', 'unitId', 3, '1', '{"scope":"device"}', 'Slave / unit identifier.'),
             (861000000001, 'Register Base', 'registerBase', 3, '40001', '{"scope":"device"}',
              'Holding-register base address.'),
             (861000000002, 'OPC UA Namespace', 'namespaceIndex', 3, '2', '{"scope":"device"}',
              'Namespace index used by node ids.'),
             (861000000002, 'Security Mode', 'securityMode', 0, 'SignAndEncrypt', '{"scope":"device"}',
              'OPC UA security mode.'),
             (861000000003, 'MQTT Topic Prefix', 'topicPrefix', 0, 'dc3/demo/energy', '{"scope":"device"}',
              'Device topic prefix.'),
             (861000000003, 'QoS', 'qos', 3, '1', '{"scope":"device"}', 'MQTT quality-of-service level.'),
             (861000000004, 'S7 Rack', 'rack', 3, '0', '{"scope":"device"}', 'PLC rack number.'),
             (861000000004, 'S7 Slot', 'slot', 3, '2', '{"scope":"device"}', 'PLC CPU slot.'),
             (861000000005, 'Seed', 'seed', 3, '202605', '{"scope":"device"}', 'Synthetic stream seed.'),
             (861000000005, 'Fault Injection', 'faultInjection', 7, 'false', '{"scope":"device"}',
              'Enable demo fault injection.')) AS t(driver_id, attribute_name, attribute_code, attribute_type_flag,
                                                    default_value, attribute_ext, remark);

INSERT INTO dc3_manager.dc3_point_attribute (id, attribute_name, attribute_code, attribute_type_flag, default_value,
                                             driver_id, attribute_ext,
                                             enable_flag, tenant_id, remark, signature, version, creator_id,
                                             creator_name, create_time, operator_id, operator_name, operate_time,
                                             deleted)
SELECT 866300000000 + row_number() OVER (),
       attribute_name,
       attribute_code,
       attribute_type_flag,
       default_value,
       driver_id,
       attribute_ext::json, 0,
       1,
       remark,
       'demo',
       1,
       1,
       'dc3',
       now() - interval '12 days',
       1,
       'dc3',
       now() - interval '8 minutes',
       0
FROM (VALUES (861000000001, 'Register Address', 'registerAddress', 3, '40001', '{"scope":"point"}',
              'Protocol register address.'),
             (861000000001, 'Polling Interval', 'pollingIntervalMs', 3, '10000', '{"scope":"point"}',
              'Per-point polling interval in milliseconds.'),
             (861000000002, 'Node ID', 'nodeId', 0, 'ns=2;s=Tag', '{"scope":"point"}', 'OPC UA node id.'),
             (861000000002, 'Deadband', 'deadband', 6, '0.05', '{"scope":"point"}',
              'Deadband used for noisy analog values.'),
             (861000000003, 'Topic', 'topic', 0, 'dc3/demo/energy/value', '{"scope":"point"}', 'MQTT point topic.'),
             (861000000003, 'JSON Path', 'jsonPath', 0, '$.value', '{"scope":"point"}', 'JSON value extraction path.'),
             (861000000004, 'DB Address', 'dbAddress', 0, 'DB10.DBD0', '{"scope":"point"}', 'S7 DB address.'),
             (861000000004, 'Sample Mode', 'sampleMode', 0, 'cyclic', '{"scope":"point"}', 'PLC sample mode.'),
             (861000000005, 'Waveform', 'waveform', 0, 'sin', '{"scope":"point"}', 'Synthetic waveform.'),
             (861000000005, 'Noise Ratio', 'noiseRatio', 6, '0.03', '{"scope":"point"}',
              'Synthetic noise ratio.')) AS t(driver_id, attribute_name, attribute_code, attribute_type_flag,
                                              default_value, attribute_ext, remark);

INSERT INTO dc3_manager.dc3_driver_attribute_config (id, attribute_id, config_value, device_id, config_ext, enable_flag,
                                                     tenant_id, remark, signature, version,
                                                     creator_id, creator_name, create_time, operator_id, operator_name,
                                                     operate_time, deleted)
SELECT 866700000000 + row_number() OVER (),
       a.id,
       CASE a.attribute_code
           WHEN 'unitId' THEN ((d.id % 16) + 1)::text
           WHEN 'registerBase' THEN '40001'
           WHEN 'namespaceIndex' THEN '2'
           WHEN 'securityMode' THEN 'SignAndEncrypt'
           WHEN 'topicPrefix' THEN 'dc3/demo/' || lower(replace(d.device_code, '.', '/'))
           WHEN 'qos' THEN '1'
           WHEN 'rack' THEN '0'
           WHEN 'slot' THEN '2'
           WHEN 'seed' THEN (202605 + (d.id % 100))::text
           WHEN 'faultInjection' THEN 'false'
           ELSE a.default_value
           END,
       d.id,
       '{"source":"demo"}',
       0,
       1,
       'Demo driver attribute config.',
       'demo',
       1,
       1,
       'dc3',
       now() - interval '10 days',
       1,
       'dc3',
       now() - interval '7 minutes',
       0
FROM dc3_manager.dc3_device d
         JOIN dc3_manager.dc3_driver_attribute a ON a.driver_id = d.driver_id
WHERE d.id BETWEEN 862000000000 AND 862999999999
  AND a.id BETWEEN 866200000000 AND 866299999999;

INSERT INTO dc3_manager.dc3_point_attribute_config (id, attribute_id, config_value, device_id, config_ext, point_id,
                                                    enable_flag, tenant_id, remark, signature, version,
                                                    creator_id, creator_name, create_time, operator_id, operator_name,
                                                    operate_time, deleted)
SELECT 866800000000 + row_number() OVER (),
       a.id,
       CASE a.attribute_code
           WHEN 'registerAddress' THEN (40000 + ((p.id % 1000):: int))::text
           WHEN 'pollingIntervalMs' THEN '10000'
           WHEN 'nodeId' THEN 'ns=2;s=' || replace(d.device_code, '.', '_') || '.' || p.point_code
           WHEN 'deadband' THEN '0.05'
           WHEN 'topic' THEN 'dc3/demo/' || lower(replace(d.device_code, '.', '/')) || '/' || p.point_code
           WHEN 'jsonPath' THEN '$.value'
           WHEN 'dbAddress' THEN 'DB10.DBD' || ((p.id % 24):: int * 4)::text
           WHEN 'sampleMode' THEN 'cyclic'
           WHEN 'waveform' THEN 'sin'
           WHEN 'noiseRatio' THEN '0.03'
           ELSE a.default_value
           END,
       d.id,
       '{"source":"demo"}',
       p.id,
       0,
       1,
       'Demo point attribute config.',
       'demo',
       1,
       1,
       'dc3',
       now() - interval '10 days',
       1,
       'dc3',
       now() - interval '7 minutes',
       0
FROM dc3_manager.dc3_device d
         JOIN dc3_manager.dc3_point p ON p.profile_id = d.profile_id
         JOIN dc3_manager.dc3_point_attribute a ON a.driver_id = d.driver_id
WHERE d.profile_id IS NOT NULL
  AND a.id BETWEEN 866300000000 AND 866399999999;

-- ---------------------------------------------------------------------
-- Data domain: notification, messages, rules, alerts, history values
-- ---------------------------------------------------------------------
INSERT INTO dc3_data.dc3_notify (id, notify_name, notify_code, auto_confirm_flag, notify_interval, notify_ext,
                                 enable_flag,
                                 tenant_id, remark, creator_id, creator_name, create_time, operator_id, operator_name,
                                 operate_time, deleted)
VALUES (866000000001, 'Ops Feishu Alert Policy', 'demo.notify.ops.feishu', 0, 300000,
        $json${"type":"alarm-notify-policy", "version":1,
        "content":"{\"dedup\":{\"enabled\":true,\"key\":\"${tenantId}:${ruleCode}:${entityId}\"},\"rateLimit\":{\"intervalMs\":300000,\"maxCount\":1},\"repeat\":{\"enabled\":true,\"intervalMs\":1800000,\"maxTimes\":0},\"recovery\":{\"enabled\":true,\"sendRecoveryMessage\":true,\"autoConfirmOnRecovery\":false}}",
        "remark":"Operations alert notification policy."}$json$, 0, 1, 'Operations alert notification policy.', 1,
        'dc3', now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (866000000002, 'Maintenance Digest Policy', 'demo.notify.maintenance.digest', 1, 1800000,
        $json${"type":"alarm-notify-policy", "version":1,
        "content":"{\"dedup\":{\"enabled\":true,\"key\":\"${tenantId}:${ruleCode}:${entityId}\"},\"rateLimit\":{\"intervalMs\":1800000,\"maxCount\":1},\"repeat\":{\"enabled\":false},\"recovery\":{\"enabled\":true,\"sendRecoveryMessage\":false,\"autoConfirmOnRecovery\":true}}",
        "remark":"Maintenance digest policy with auto-confirmed informational rules."}$json$, 0, 1,
        'Maintenance digest policy with auto-confirmed informational rules.', 1, 'dc3', now() - interval '13 days', 1,
        'dc3', now() - interval '8 minutes', 0);

INSERT INTO dc3_data.dc3_notify_channel (id, channel_name, channel_code, channel_type_flag, credential_ref, channel_ext,
                                         enable_flag,
                                         tenant_id, remark, creator_id, creator_name, create_time, operator_id,
                                         operator_name, operate_time, deleted)
VALUES (866100000001, 'Production Operations Feishu Bot', 'demo.channel.feishu.ops', 0, 'secret:feishu:demo-ops-bot',
        $json${"type":"notify-channel", "version":1,
        "content":"{\"signEnabled\":true,\"cardVersion\":\"interactive-card-v1\",\"atAllAllowed\":false,\"testMessageEnabled\":true,\"options\":{\"locale\":\"zh-CN\"}}",
        "remark":"Demo Feishu robot for production operations alarm cards."}$json$, 0, 1,
        'Demo Feishu robot for production operations alarm cards.', 1, 'dc3', now() - interval '13 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (866100000002, 'Maintenance Digest Webhook', 'demo.channel.webhook.maintenance', 1,
        'secret:webhook:demo-maintenance', $json${"type":"notify-channel", "version":1,
        "content":"{\"signEnabled\":false,\"testMessageEnabled\":true,\"options\":{\"method\":\"POST\",\"contentType\":\"application/json\"}}",
        "remark":"Demo webhook for maintenance digest notifications."}$json$, 0, 1,
        'Demo webhook for maintenance digest notifications.', 1, 'dc3', now() - interval '13 days', 1, 'dc3',
        now() - interval '8 minutes', 0);

INSERT INTO dc3_data.dc3_notify_channel_bind (id, notify_id, channel_id, bind_ext, enable_flag, tenant_id,
                                              remark, creator_id, creator_name, create_time, operator_id, operator_name,
                                              operate_time, deleted)
VALUES (866200000001, 866000000001, 866100000001, $json${"type":"notify-channel-bind", "version":1,
        "content":"{\"levels\":[\"P0\",\"P1\",\"P2\"],\"sendRecovery\":true,\"rateLimitOverrideMs\":300000}",
        "remark":"Send operations policy alarms to the Feishu operations bot."}$json$, 0, 1,
        'Send operations policy alarms to the Feishu operations bot.', 1, 'dc3', now() - interval '13 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (866200000002, 866000000002, 866100000002, $json${"type":"notify-channel-bind", "version":1,
        "content":"{\"levels\":[\"P2\",\"P3\"],\"sendRecovery\":false,\"rateLimitOverrideMs\":1800000}",
        "remark":"Send maintenance digest policy alarms to the maintenance webhook."}$json$, 0, 1,
        'Send maintenance digest policy alarms to the maintenance webhook.', 1, 'dc3', now() - interval '13 days', 1,
        'dc3', now() - interval '8 minutes', 0);

INSERT INTO dc3_data.dc3_message (id, message_name, message_code, message_level, message_ext, enable_flag, tenant_id,
                                  remark, creator_id, creator_name, create_time, operator_id, operator_name,
                                  operate_time, deleted)
VALUES (866500000001, 'Pump Vibration High', 'demo.msg.pump.vibration.high', 3, $json${"type":"alarm-message-template",
        "version":1,
        "content":"{\"variables\":[\"severity\",\"device\",\"point\",\"value\",\"unit\",\"threshold\",\"triggerTime\"],\"templates\":[{\"channelType\":\"FEISHU_BOT\",\"payloadType\":\"CARD\",\"template\":{\"title\":\"${severity} ${device} vibration alarm\",\"summary\":\"${point} is ${value}${unit}, threshold ${threshold}${unit}.\"}}]}",
        "remark":"Critical pump vibration alarm message."}$json$, 0, 1, 'Critical pump vibration alarm message.', 1,
        'dc3', now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (866500000002, 'AHU Filter Differential Pressure High', 'demo.msg.ahu.filter.dp.high', 2,
        $json${"type":"alarm-message-template", "version":1,
        "content":"{\"variables\":[\"severity\",\"device\",\"point\",\"value\",\"unit\",\"threshold\",\"triggerTime\"],\"templates\":[{\"channelType\":\"FEISHU_BOT\",\"payloadType\":\"CARD\",\"template\":{\"title\":\"${severity} ${device} filter service alarm\",\"summary\":\"${point} is ${value}${unit}, threshold ${threshold}${unit}.\"}}]}",
        "remark":"AHU filter service alarm message."}$json$, 0, 1, 'AHU filter service alarm message.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (866500000003, 'CIP Conductivity Out Of Range', 'demo.msg.cip.conductivity.range', 3,
        $json${"type":"alarm-message-template", "version":1,
        "content":"{\"variables\":[\"severity\",\"device\",\"point\",\"value\",\"unit\",\"low\",\"high\",\"triggerTime\"],\"templates\":[{\"channelType\":\"FEISHU_BOT\",\"payloadType\":\"CARD\",\"template\":{\"title\":\"${severity} ${device} conductivity alarm\",\"summary\":\"${point} is ${value}${unit}, expected ${low}-${high}${unit}.\"}}]}",
        "remark":"CIP quality alarm message."}$json$, 0, 1, 'CIP quality alarm message.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (866500000004, 'Packaging Reject Rate High', 'demo.msg.pack.reject.high', 2,
        $json${"type":"alarm-message-template", "version":1,
        "content":"{\"variables\":[\"severity\",\"device\",\"point\",\"value\",\"unit\",\"threshold\",\"triggerTime\"],\"templates\":[{\"channelType\":\"FEISHU_BOT\",\"payloadType\":\"CARD\",\"template\":{\"title\":\"${severity} packaging reject alarm\",\"summary\":\"${point} counted ${value}${unit}, threshold ${threshold}${unit}.\"}}]}",
        "remark":"Packaging quality alarm message."}$json$, 0, 1, 'Packaging quality alarm message.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0);

INSERT INTO dc3_data.dc3_rule (id, alarm_target_type_flag, rule_name, rule_code, entity_id, notify_id, message_id,
                               rule_ext, enable_flag,
                               tenant_id, remark, creator_id, creator_name, create_time, operator_id, operator_name,
                               operate_time, deleted)
VALUES (867000000001, 0, 'Pump vibration high for 3 samples', 'demo.rule.pump.vibration.high', 864000000004,
        866000000001, 866500000001, $json${"type":"alarm-rule", "version":1,
        "content":"{\"condition\":{\"field\":\"numValue\",\"operator\":\">\",\"threshold\":4.5,\"unit\":\"mm/s\"},\"window\":{\"mode\":\"ALL\",\"minSamples\":3},\"recovery\":{\"enabled\":true,\"operator\":\"<=\",\"threshold\":4.0,\"duration\":\"PT2M\"},\"severity\":\"P1\",\"eventType\":\"ALARM\",\"labels\":[\"pump\",\"vibration\"]}",
        "remark":"Vibration alarm rule for pump bearing health."}$json$, 0, 1,
        'Vibration alarm rule for pump bearing health.', 1, 'dc3', now() - interval '13 days', 1, 'dc3',
        now() - interval '8 minutes', 0),
       (867000000002, 0, 'AHU filter DP high', 'demo.rule.ahu.filter.dp.high', 864000000013, 866000000002, 866500000002,
        $json${"type":"alarm-rule", "version":1,
        "content":"{\"condition\":{\"field\":\"numValue\",\"operator\":\">\",\"threshold\":280,\"unit\":\"Pa\"},\"window\":{\"mode\":\"ALL\",\"minSamples\":2},\"recovery\":{\"enabled\":true,\"operator\":\"<=\",\"threshold\":240,\"duration\":\"PT5M\"},\"severity\":\"P2\",\"eventType\":\"ALARM\",\"labels\":[\"ahu\",\"filter\"]}",
        "remark":"Filter replacement alarm rule."}$json$, 0, 1, 'Filter replacement alarm rule.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (867000000003, 0, 'CIP conductivity out of range', 'demo.rule.cip.conductivity.range', 864000000052,
        866000000001, 866500000003, $json${"type":"alarm-rule", "version":1,
        "content":"{\"condition\":{\"field\":\"numValue\",\"operator\":\"outside\",\"low\":0.5,\"high\":32.0,\"unit\":\"mS/cm\"},\"window\":{\"mode\":\"ANY\",\"minSamples\":2},\"recovery\":{\"enabled\":true,\"operator\":\"between\",\"duration\":\"PT3M\"},\"severity\":\"P1\",\"eventType\":\"ALARM\",\"labels\":[\"cip\",\"quality\"]}",
        "remark":"CIP concentration quality rule."}$json$, 0, 1, 'CIP concentration quality rule.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0),
       (867000000004, 0, 'Packaging reject count spike', 'demo.rule.pack.reject.high', 864000000034, 866000000001,
        866500000004, $json${"type":"alarm-rule", "version":1,
        "content":"{\"condition\":{\"field\":\"numValue\",\"operator\":\">\",\"threshold\":12,\"unit\":\"pcs\"},\"window\":{\"mode\":\"SUM\",\"duration\":\"PT15M\",\"minSamples\":3},\"recovery\":{\"enabled\":true,\"operator\":\"<=\",\"threshold\":6,\"duration\":\"PT15M\"},\"severity\":\"P2\",\"eventType\":\"ALARM\",\"labels\":[\"packaging\",\"quality\"]}",
        "remark":"Reject count spike rule."}$json$, 0, 1, 'Reject count spike rule.', 1, 'dc3',
        now() - interval '13 days', 1, 'dc3', now() - interval '8 minutes', 0);

WITH entity_alarm_seed(alarm_target_type_flag, entity_id, driver_id, device_id, point_id, content, age_min, confirmed)
         AS (VALUES (0, 864000000004, 861000000001, 862000000001, 864000000004,
                     'PD-CHW-PUMP-01 vibration RMS exceeded 4.5 mm/s for 3 consecutive samples.', 8, 0),
                    (0, 864000000003, 861000000001, 862000000002, 864000000003,
                     'PD-CHW-PUMP-02 motor current remained above 92 A during ramp-up.', 23, 0),
                    (0, 864000000013, 861000000001, 862000000003, 864000000013,
                     'PD-AHU-7F-EAST filter differential pressure reached 318 Pa.', 37, 1),
                    (0, 864000000016, 861000000001, 862000000004, 864000000016,
                     'PD-AHU-9F-WEST CO2 concentration exceeded 1000 ppm during meeting load.', 49, 0),
                    (0, 864000000052, 861000000002, 862000000005, 864000000052,
                     'SZ-CIP-SKID-01 conductivity dropped below recipe band during caustic wash.', 64, 0),
                    (0, 864000000053, 861000000002, 862000000006, 864000000053,
                     'SZ-CIP-SKID-02 pH probe drift detected; calibration check required.', 78, 1),
                    (0, 864000000025, 861000000003, 862000000007, 864000000025,
                     'SZ-TANK-FARM-MTR-01 power factor below 0.88 for 20 minutes.', 94, 0),
                    (0, 864000000024, 861000000003, 862000000008, 864000000024,
                     'SH-HQ-MAIN-MTR-01 phase A current imbalance exceeded 14 percent.', 118, 1),
                    (0, 864000000032, 861000000004, 862000000009, 864000000032,
                     'HZ-PACK-L3-PLC motor torque exceeded 85 percent under stable speed.', 143, 0),
                    (0, 864000000034, 861000000004, 862000000010, 864000000034,
                     'HZ-PACK-L3-CHECKWEIGHER reject count spiked above shift baseline.', 177, 0),
                    (0, 864000000045, 861000000005, 862000000011, 864000000045,
                     'NJ-WH-ENV-01 battery level is below 25 percent.', 211, 1),
                    (0, 864000000041, 861000000005, 862000000012, 864000000041,
                     'NJ-WH-ENV-02 cold-room temperature exceeded 8 C for 11 minutes.', 247, 0),
                    (0, 864000000046, 861000000005, 862000000013, 864000000046,
                     'EDGE-LAB-SIM-01 RSSI below -85 dBm; packet loss likely.', 296, 0),
                    (0, 864000000004, 861000000005, 862000000014, 864000000004,
                     'EDGE-LAB-SIM-02 synthetic pump vibration injected above alarm threshold.', 344, 1),
                    (0, 864000000013, 861000000001, 862000000015, 864000000013,
                     'PD-ROOF-AHU-01 filter differential pressure trending toward service threshold.', 396, 0),
                    (0, 864000000036, 861000000004, 862000000016, 864000000036,
                     'HZ-PACK-L3-VISION emergency-stop input is pressed while device disabled.', 1520, 0),
                    (2, 861000000001, 861000000001, 0, 0,
                     'Modbus TCP driver retry rate above 8 percent against PD cooling PLC.', 12, 0),
                    (2, 861000000002, 861000000002, 0, 0,
                     'OPC UA secure-channel renewal exceeded 2 seconds for Suzhou CIP gateway.', 44, 0),
                    (2, 861000000003, 861000000003, 0, 0,
                     'MQTT broker reported retained-message backlog above 1200 topics.', 71, 1),
                    (2, 861000000004, 861000000004, 0, 0,
                     'S7 driver read cycle p95 latency above 420 ms for line 3 PLC.', 122, 0),
                    (2, 861000000005, 861000000005, 0, 0,
                     'Virtual edge lab fault-injection window still active after scheduled test.', 188, 1),
                    (2, 861000000001, 861000000001, 0, 0, 'Modbus TCP socket reconnect occurred 4 times in 15 minutes.',
                     420, 0),
                    (2, 861000000004, 861000000004, 0, 0, 'S7 connection dropped during packaging line format change.',
                     1560, 0))
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000000000 + row_number() OVER (),
       alarm_target_type_flag,
       entity_id,
       driver_id,
       device_id,
       point_id,
       0,
       0,
       4,
       4,
       CASE WHEN confirmed = 0 THEN 2 ELSE 3 END,
       json_build_object('content', content, 'source', 'demo'),
       3600,
       confirmed,
       1,
       now() - (age_min || ' minutes')::interval, now() - (GREATEST(age_min - 3, 0) || ' minutes') ::interval
FROM entity_alarm_seed;

-- Event-dashboard insight scenarios.
-- pump_storm: device point alarm flurry on PD-CHW-PUMP-01
WITH pump_storm AS (SELECT generate_series(0, 119) AS idx)
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000001000 + idx,
       0,
       CASE idx % 3
           WHEN 0 THEN 864000000004
           WHEN 1 THEN 864000000003
           ELSE 864000000001
           END,
       861000000001,
       862000000001,
       CASE idx % 3
           WHEN 0 THEN 864000000004
           WHEN 1 THEN 864000000003
           ELSE 864000000001
           END,
       0,
       0,
       4,
       4,
       CASE WHEN idx % 5 = 0 THEN 3 ELSE 2 END,
       json_build_object(
               'content',
               format(
                       'PD-CHW-PUMP-01 vibration storm sample %s: bearing trend remained above alarm band during condenser-water load swing.',
                       idx + 1),
               'source', 'demo-storm'
       ),
       3600,
       CASE WHEN idx % 5 = 0 THEN 1 ELSE 0 END,
       1,
       now() - (idx * interval '11 minutes') - ((idx % 4) * interval '7 seconds'),
       now() - (idx * interval '11 minutes') + (((idx % 23) + 2) * interval '1 minute')
FROM pump_storm;

-- modbus_storm: driver alarm flurry on Modbus TCP driver
WITH modbus_storm AS (SELECT generate_series(0, 104) AS idx)
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000002000 + idx,
       2,
       861000000001,
       861000000001,
       0,
       0,
       0,
       0,
       4,
       4,
       CASE WHEN idx % 4 = 0 THEN 3 ELSE 2 END,
       json_build_object(
               'content',
               format(
                       'Modbus TCP - Pudong Cooling PLC retry storm sample %s: socket reconnect and register timeout clustered around cooling plant PLC.',
                       idx + 1),
               'source', 'demo-storm'
       ),
       3600,
       CASE WHEN idx % 4 = 0 THEN 1 ELSE 0 END,
       1,
       now() - (idx * interval '12 minutes') - ((idx % 5) * interval '5 seconds'),
       now() - (idx * interval '12 minutes') + (((idx % 19) + 3) * interval '1 minute')
FROM modbus_storm;

-- correlated device alarms on SZ-CIP-SKID-01
WITH correlated AS (SELECT generate_series(0, 35) AS idx)
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000003000 + idx,
       0,
       CASE idx % 3
           WHEN 0 THEN 864000000052
           WHEN 1 THEN 864000000053
           ELSE 864000000054
           END,
       861000000002,
       862000000005,
       CASE idx % 3
           WHEN 0 THEN 864000000052
           WHEN 1 THEN 864000000053
           ELSE 864000000054
           END,
       0,
       0,
       4,
       4,
       2,
       json_build_object(
               'content',
               format(
                       'SZ-CIP-SKID-01 correlated quality alarm %s: conductivity, pH, and flow deviated during recipe transition.',
                       idx + 1),
               'source', 'demo-correlation'
       ),
       3600,
       CASE WHEN idx % 6 = 0 THEN 1 ELSE 0 END,
       1,
       now() - (idx * interval '5 minutes'),
       now() - (idx * interval '5 minutes') + (((idx % 13) + 4) * interval '1 minute')
FROM correlated;

-- correlated driver alarms on OPC UA Suzhou CIP Gateway
WITH correlated AS (SELECT generate_series(0, 35) AS idx)
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000004000 + idx,
       2,
       861000000002,
       861000000002,
       0,
       0,
       0,
       0,
       4,
       4,
       2,
       json_build_object(
               'content',
               format(
                       'OPC UA - Suzhou CIP Gateway correlated secure-channel warning %s within the CIP skid alarm window.',
                       idx + 1),
               'source', 'demo-correlation'
       ),
       3600,
       CASE WHEN idx % 6 = 0 THEN 1 ELSE 0 END,
       1,
       now() - (idx * interval '5 minutes') + interval '10 seconds',
       now() - (idx * interval '5 minutes') + (((idx % 11) + 5) * interval '1 minute')
FROM correlated;

-- historical confirmed alarms for MTTA trend
WITH historical_mtta AS (SELECT generate_series(1, 14) AS idx)
INSERT INTO dc3_data.dc3_entity_alarm (id, alarm_target_type_flag, entity_id, driver_id, device_id, point_id, rule_id,
                                       rule_state_id,
                                       alarm_type_flag, alarm_source_flag, alarm_level_flag, alarm_ext, expired_time,
                                       confirm_flag, tenant_id, create_time, operate_time)
SELECT 868000005000 + idx,
       0,
       CASE idx % 3
           WHEN 0 THEN 864000000013
           WHEN 1 THEN 864000000034
           ELSE 864000000045
           END,
       CASE idx % 3
           WHEN 0 THEN 861000000001
           WHEN 1 THEN 861000000004
           ELSE 861000000005
           END,
       CASE idx % 3
           WHEN 0 THEN 862000000003
           WHEN 1 THEN 862000000009
           ELSE 862000000012
           END,
       CASE idx % 3
           WHEN 0 THEN 864000000013
           WHEN 1 THEN 864000000034
           ELSE 864000000045
           END,
       0,
       0,
       4,
       4,
       3,
       json_build_object(
               'content',
               format('Historical confirmed demo alarm %s used to shape MTTA trend across prior operation days.', idx),
               'source', 'demo-mtta'
       ),
       3600,
       1,
       1,
       now() - (idx * interval '1 day') - interval '2 hours',
       now() - (idx * interval '1 day') - interval '2 hours' + (((idx % 9) + 6) * interval '1 minute')
FROM historical_mtta;

WITH point_params(point_id, base_value, amplitude, decimals, point_type_flag) AS (VALUES (864000000001, 295.0, 22.0, 2,
                                                                                          6),
                                                                                         (864000000002, 11.2, 1.4, 2,
                                                                                          6),
                                                                                         (864000000003, 62.0, 12.0, 2,
                                                                                          6),
                                                                                         (864000000004, 2.4, 1.1, 3, 5),
                                                                                         (864000000005, 1.0, 1.0, 0, 7),
                                                                                         (864000000006, 0.0, 3.0, 0, 3),
                                                                                         (864000000011, 19.0, 2.1, 2,
                                                                                          6),
                                                                                         (864000000012, 48.0, 8.0, 1,
                                                                                          6),
                                                                                         (864000000013, 205.0, 62.0, 1,
                                                                                          6),
                                                                                         (864000000014, 58.0, 14.0, 1,
                                                                                          6),
                                                                                         (864000000015, 42.0, 18.0, 1,
                                                                                          6),
                                                                                         (864000000016, 720.0, 170.0, 0,
                                                                                          6),
                                                                                         (864000000021, 420.0, 190.0, 2,
                                                                                          6),
                                                                                         (864000000022, 188000.0, 42.0,
                                                                                          1, 6),
                                                                                         (864000000023, 228.0, 4.0, 1,
                                                                                          6),
                                                                                         (864000000024, 388.0, 82.0, 2,
                                                                                          6),
                                                                                         (864000000025, 0.94, 0.05, 3,
                                                                                          6),
                                                                                         (864000000026, 50.0, 0.08, 2,
                                                                                          6),
                                                                                         (864000000031, 52.0, 9.0, 1,
                                                                                          6),
                                                                                         (864000000032, 61.0, 17.0, 1,
                                                                                          6),
                                                                                         (864000000033, 12600.0, 33.0,
                                                                                          0, 4),
                                                                                         (864000000034, 28.0, 16.0, 0,
                                                                                          3),
                                                                                         (864000000035, 0.0, 0.0, 0, 0),
                                                                                         (864000000036, 0.0, 1.0, 0, 7),
                                                                                         (864000000041, 7.1, 1.6, 2, 6),
                                                                                         (864000000042, 54.0, 9.0, 1,
                                                                                          6),
                                                                                         (864000000043, 18.0, 14.0, 1,
                                                                                          6),
                                                                                         (864000000044, 55.0, 8.0, 1,
                                                                                          6),
                                                                                         (864000000045, 68.0, 8.0, 1,
                                                                                          6),
                                                                                         (864000000046, -68.0, 13.0, 1,
                                                                                          6),
                                                                                         (864000000051, 58.0, 26.0, 1,
                                                                                          6),
                                                                                         (864000000052, 12.5, 8.5, 2,
                                                                                          6),
                                                                                         (864000000053, 9.2, 2.8, 2, 6),
                                                                                         (864000000054, 18.0, 6.0, 1,
                                                                                          6),
                                                                                         (864000000055, 43.0, 29.0, 1,
                                                                                          6),
                                                                                         (864000000056, 2.0, 1.0, 0,
                                                                                          3)),
     device_points AS (SELECT d.id AS device_id,
                              d.driver_id,
                              p.id AS point_id,
                              pp.base_value,
                              pp.amplitude,
                              pp.decimals,
                              pp.point_type_flag
                       FROM dc3_manager.dc3_device d
                                JOIN dc3_manager.dc3_point p ON p.profile_id = d.profile_id
                                JOIN point_params pp ON pp.point_id = p.id
                       WHERE d.profile_id IS NOT NULL),
     sampled AS (SELECT dp.*,
                        gs.idx,
                        now() - (gs.idx * interval '10 minutes') AS ts,
                        dp.base_value
                            + dp.amplitude * sin((gs.idx + (dp.device_id % 13))::double precision / 5.0)
                            + ((dp.device_id % 7) - 3) * 0.17    AS numeric_value
                 FROM device_points dp
                          CROSS JOIN generate_series(0, 143) AS gs(idx)
    )
INSERT INTO dc3_history.dc3_point_value (device_id, point_id, raw_value, cal_value, num_value, driver_id, tenant_id,
                                         create_time, operate_time)
SELECT device_id,
       point_id,
       CASE
           WHEN point_type_flag = 0 THEN
               CASE (idx + device_id)::bigint % 4
                   WHEN 0 THEN 'RUNNING'
                   WHEN 1 THEN 'RUNNING'
                   WHEN 2 THEN 'STARVED'
                   ELSE 'BLOCKED'
               END
           WHEN point_type_flag = 7 THEN CASE WHEN (idx + device_id)::bigint % 17 = 0 THEN 'false' ELSE 'true' END
           WHEN point_type_flag IN (3, 4) THEN GREATEST(round(numeric_value::numeric, 0), 0)::text
           ELSE round(numeric_value::numeric, decimals)::text
       END AS raw_value,
       CASE
           WHEN point_type_flag = 0 THEN
               CASE (idx + device_id)::bigint % 4
                   WHEN 0 THEN 'RUNNING'
                   WHEN 1 THEN 'RUNNING'
                   WHEN 2 THEN 'STARVED'
                   ELSE 'BLOCKED'
               END
           WHEN point_type_flag = 7 THEN CASE WHEN (idx + device_id)::bigint % 17 = 0 THEN 'false' ELSE 'true' END
           WHEN point_type_flag IN (3, 4) THEN GREATEST(round(numeric_value::numeric, 0), 0)::text
           ELSE round(numeric_value::numeric, decimals)::text
       END AS cal_value,
       CASE
           WHEN point_type_flag = 0 THEN NULL
           WHEN point_type_flag = 7 THEN CASE WHEN (idx + device_id)::bigint % 17 = 0 THEN 0 ELSE 1 END::double precision
           WHEN point_type_flag IN (3, 4) THEN GREATEST(round(numeric_value::numeric, 0), 0)::double precision
           ELSE round(numeric_value::numeric, decimals)::double precision
       END AS num_value,
       driver_id,
       1,
       ts,
       ts + (((idx % 9) + 1) * interval '17 milliseconds')
FROM sampled;

-- Make a few normally active points go quiet recently. This produces
-- realistic "silent source" rows without fabricating a separate table:
-- the same historical samples exist, only the newest two hours are absent.
DELETE
FROM dc3_history.dc3_point_value
WHERE tenant_id = 1
  AND create_time >= now() - interval '2 hours'
    AND (device_id, point_id) IN (
                                  (862000000011, 864000000045),
                                  (862000000011, 864000000046),
                                  (862000000012, 864000000041),
                                  (862000000016, 864000000034),
                                  (862000000006, 864000000053),
                                  (862000000004, 864000000016)
        );

-- ---------------------------------------------------------------------
-- Normalize all demo extension JSON into the backend JsonExt wire format.
--
-- The Java entities map every *_ext column to JsonExt:
--   { "type": "...", "content": "<json-or-text string>", "version": 1, "remark": "" }
-- Keeping demo-specific fields directly at the top level would make
-- MyBatis-Plus JacksonTypeHandler fail before the builders can parse content.
-- ---------------------------------------------------------------------
UPDATE dc3_manager.dc3_driver
SET driver_ext = json_build_object(
        'type', 'demo-driver',
        'content', driver_ext::text,
        'version', 1,
        'remark', 'Demo driver metadata'
                 )
WHERE tenant_id = 1
  AND id BETWEEN 861000000000 AND 861999999999;

UPDATE dc3_manager.dc3_profile
SET profile_ext = json_build_object(
        'type', 'demo-profile',
        'content', profile_ext::text,
        'version', 1,
        'remark', 'Demo profile metadata'
                  )
WHERE tenant_id = 1
  AND id BETWEEN 863000000000 AND 863999999999;

UPDATE dc3_manager.dc3_device
SET device_ext = json_build_object(
        'type', 'demo-device',
        'content', device_ext::text,
        'version', 1,
        'remark', 'Demo device metadata'
                 )
WHERE tenant_id = 1
  AND id BETWEEN 862000000000 AND 862999999999;

UPDATE dc3_manager.dc3_point
SET point_ext = json_build_object(
        'type', 'demo-point',
        'content', point_ext::text,
        'version', 1,
        'remark', 'Demo point metadata'
                )
WHERE tenant_id = 1
  AND id BETWEEN 864000000000 AND 864999999999;

UPDATE dc3_manager.dc3_driver_attribute
SET attribute_ext = json_build_object(
        'type', 'demo-driver-attribute',
        'content', attribute_ext::text,
        'version', 1,
        'remark', 'Demo driver attribute metadata'
                    )
WHERE tenant_id = 1
  AND id BETWEEN 866200000000 AND 866299999999;

UPDATE dc3_manager.dc3_point_attribute
SET attribute_ext = json_build_object(
        'type', 'demo-point-attribute',
        'content', attribute_ext::text,
        'version', 1,
        'remark', 'Demo point attribute metadata'
                    )
WHERE tenant_id = 1
  AND id BETWEEN 866300000000 AND 866399999999;

UPDATE dc3_manager.dc3_driver_attribute_config
SET config_ext = json_build_object(
        'type', 'demo-driver-config',
        'content', config_ext::text,
        'version', 1,
        'remark', 'Demo driver configuration metadata'
                 )
WHERE tenant_id = 1
  AND id BETWEEN 866700000000 AND 866799999999;

UPDATE dc3_manager.dc3_point_attribute_config
SET config_ext = json_build_object(
        'type', 'demo-point-config',
        'content', config_ext::text,
        'version', 1,
        'remark', 'Demo point configuration metadata'
                 )
WHERE tenant_id = 1
  AND id BETWEEN 866800000000 AND 866899999999;

UPDATE dc3_data.dc3_notify
SET notify_ext = json_build_object(
        'type', 'ALARM_NOTIFY_POLICY',
        'content', notify_ext::text,
        'version', 1,
        'remark', 'Demo notification metadata'
                 )
WHERE tenant_id = 1
  AND id BETWEEN 866000000000 AND 866099999999;

UPDATE dc3_data.dc3_notify_channel
SET channel_ext = json_build_object(
        'type', CASE channel_type_flag
                    WHEN 0 THEN 'FEISHU_BOT'
                    WHEN 1 THEN 'WEBHOOK'
                    ELSE 'NOTIFY_CHANNEL'
            END,
        'content', channel_ext::text,
        'version', 1,
        'remark', 'Demo notification channel metadata'
                  )
WHERE tenant_id = 1
  AND id BETWEEN 866100000000 AND 866199999999;

UPDATE dc3_data.dc3_notify_channel_bind
SET bind_ext = json_build_object(
        'type', 'NOTIFY_CHANNEL_BIND',
        'content', bind_ext::text,
        'version', 1,
        'remark', 'Demo notification channel binding metadata'
               )
WHERE tenant_id = 1
  AND id BETWEEN 866200000000 AND 866299999999;

UPDATE dc3_data.dc3_message
SET message_ext = json_build_object(
        'type', 'ALARM_MESSAGE_TEMPLATE',
        'content', message_ext::text,
        'version', 1,
        'remark', 'Demo message template metadata'
                  )
WHERE tenant_id = 1
  AND id BETWEEN 866500000000 AND 866599999999;

UPDATE dc3_data.dc3_rule
SET rule_ext = json_build_object(
        'type', 'POINT_VALUE_RULE',
        'content', rule_ext::text,
        'version', 1,
        'remark', 'Demo rule metadata'
               )
WHERE tenant_id = 1
  AND id BETWEEN 867000000000 AND 867999999999;

UPDATE dc3_data.dc3_entity_alarm
SET alarm_ext = json_build_object(
        'type', 'demo-entity-alarm',
        'content', COALESCE(alarm_ext ->> 'content', alarm_ext::text),
        'version', 1,
        'remark', 'demo'
                )
WHERE tenant_id = 1
  AND id BETWEEN 868000000000 AND 868999999999;

COMMIT;
