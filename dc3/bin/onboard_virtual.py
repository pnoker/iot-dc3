#!/usr/bin/env python3
# Copyright 2016-present the IoT DC3 original author or authors.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as
# published by the Free Software Foundation, either version 3 of the
# License, or (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <https://www.gnu.org/licenses/>.

"""Onboard the VirtualDriver demo chain against a running stack.

Provisions the whole read/report/execute chain idempotently (existing rows are
matched by name/code and reused, so the script is safe to re-run):

  template(profile) -> points -> device -> driver/point attribute configs
  plus event definitions + event attribute configs (the virtual driver reports
  every configured event on its 30s cadence)
  plus command (指令) definitions + command attribute configs, and finally one
  verification call through /data/command_history/call.

Attribute identifiers are discovered from the driver registration instead of
being hard-coded, so the script works across environments. The virtual driver
must be running first — it registers its attribute definitions (host/port/tag/
eventCodePath/payloadPath/payloadTemplate/responseTemplate) at startup.

Usage:
  python3 dc3/bin/onboard_virtual.py [--base http://127.0.0.1:8000/api/v3]
                                     [--tenant default --user dc3 --password ...]
"""

import argparse
import json
import sys
import urllib.error
import urllib.request

# Demo chain anchor: the device is the idempotency key for the whole chain.
DEVICE_NAME = "VirtualDemo"
DEVICE_CODE = "virtual-demo-1"
DRIVER_CODE = "VirtualDriver"
PROFILE_NAME = "VirtualStress"

# pointName, pointCode, pointTypeFlag, rwFlag, unit
POINTS = [
    ("Temperature", "temperature", "FLOAT", "READ_WRITE", "°C"),
    ("Status", "status", "BOOLEAN", "READ_ONLY", ""),
    ("DeviceName", "deviceName", "STRING", "READ_WRITE", ""),
]

# eventName, eventCode, eventTypeFlag, eventLevelFlag
EVENTS = [
    ("Heartbeat", "HEARTBEAT", "INFO", "LOW"),
    ("Overheat", "OVERHEAT", "ALERT", "MEDIUM"),
]

# paramName, paramCode, paramTypeFlag — mirrors the payload the virtual
# driver assembles in buildEventReport (value / deviceCode / source). Param
# names must match the manager's name pattern (no spaces).
EVENT_PARAMS = [
    ("Value", "value", "STRING"),
    ("DeviceCode", "deviceCode", "STRING"),
    ("Source", "source", "STRING"),
]

# paramName, paramCode, paramDirectionFlag, paramTypeFlag, requiredFlag, defaultValue
COMMAND_NAME = "Reboot"
COMMAND_CODE = "REBOOT"
COMMAND_PARAMS = [
    ("Mode", "mode", "INPUT", "STRING", False, "graceful"),
]

# Demo alarm rule on the Temperature point so the alert profile cards have
# data: the virtual driver draws random 0-100 values, so >80 fires regularly
# and <=75 closes the alarm again.
RULE_NAME = "VirtualDemo Temperature High"
RULE_CODE = "VIRTUAL_DEMO_TEMP_HIGH"
RULE_POINT = "Temperature"


class Api:
    """Minimal cookie-session client for the dc3 gateway."""

    def __init__(self, base, tenant, user, password):
        self.base = base.rstrip("/")
        self.tenant = tenant
        self.user = user
        self.cookie = ""
        salt = self.anonymous("POST", "/auth/token/salt", {"tenant": tenant, "name": user})
        headers = self.anonymous(
            "POST",
            "/auth/token/generate",
            {"tenant": tenant, "name": user, "salt": str(salt).strip().strip('"'), "password": password},
            want_headers=True,
        )
        # Header names arrive lower-cased from some server paths — match
        # case-insensitively and keep only the "name=value" pair.
        for key, value in headers.items():
            if key.lower() == "set-cookie":
                self.cookie = value.split(";")[0].strip()
                break
        if not self.cookie:
            raise RuntimeError("login failed: no session cookie")

    def anonymous(self, method, path, body=None, want_headers=False):
        status, payload, headers = self._call(method, path, body, cookie="")
        if status >= 400:
            raise RuntimeError(f"{method} {path} -> {status}: {payload[:300]}")
        return headers if want_headers else payload

    def call(self, method, path, body=None):
        status, payload, _ = self._call(method, path, body, cookie=self.cookie)
        if status >= 400:
            raise RuntimeError(f"{method} {path} -> {status}: {payload[:300]}")
        if not payload:
            return None
        try:
            return json.loads(payload)
        except json.JSONDecodeError:
            # Some endpoints answer with a bare scalar (e.g. command call
            # returns the raw record-id string, not a JSON-encoded one).
            return payload

    def _call(self, method, path, body, cookie):
        url = self.base + path
        data = json.dumps(body).encode() if body is not None else None
        headers = {"Content-Type": "application/json"}
        if cookie:
            headers["Cookie"] = cookie
            headers["X-Auth-Tenant"] = self.tenant
            headers["X-Auth-Login"] = self.user
        request = urllib.request.Request(url, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                return response.status, response.read().decode(), dict(response.headers)
        except urllib.error.HTTPError as error:
            return error.code, error.read().decode(), {}


def unwrap_items(payload):
    """Normalise list endpoints: bare arrays, page envelopes and id->record maps."""
    if payload is None:
        return []
    if isinstance(payload, list):
        return payload
    if isinstance(payload, dict):
        for key in ("items", "records", "data", "list"):
            if isinstance(payload.get(key), list):
                return payload[key]
        # byIds endpoints answer with id -> record maps.
        if payload and all(isinstance(value, dict) for value in payload.values()):
            return list(payload.values())
    return []


def find_by(rows, **wanted):
    for row in rows:
        if all(str(row.get(key, "")) == str(value) for key, value in wanted.items()):
            return row
    return None


def ensure_attr_ids(api, path, driver_id):
    """attributeCode -> attributeId for one attribute family of the driver."""
    rows = unwrap_items(api.call("GET", f"{path}/list_by_driver_id?driver_id={driver_id}"))
    return {row.get("attributeCode"): str(row.get("id")) for row in rows if row.get("attributeCode")}


def ensure_device_chain(api, driver_id):
    devices = unwrap_items(
        api.call("POST", "/manager/device/list", {"offset": 0, "limit": 50, "deviceName": DEVICE_NAME})
    )
    device = find_by(devices, deviceName=DEVICE_NAME)
    if device:
        print(f"device   : {device['id']} ({DEVICE_NAME}) — reuse")
        return str(device["id"]), str(device["profileId"])

    profiles = unwrap_items(api.call("POST", "/manager/profile/list", {"offset": 0, "limit": 200}))
    profile = find_by(profiles, profileName=PROFILE_NAME)
    if profile:
        profile_id = str(profile["id"])
        print(f"profile  : {profile_id} ({PROFILE_NAME}) — reuse")
    else:
        created = api.call("POST", "/manager/profile/add", {"profileName": PROFILE_NAME, "enableFlag": "ENABLE"})
        profile_id = str(created["id"])
        print(f"profile  : {profile_id} ({PROFILE_NAME}) — created")

    existing_points = unwrap_items(
        api.call("GET", f"/manager/point/list_by_profile_id?profile_id={profile_id}")
    )
    for name, code, ptype, rw, unit in POINTS:
        if find_by(existing_points, pointName=name):
            print(f"point    : {name} — reuse")
            continue
        body = {
            "pointName": name,
            "pointCode": code,
            "pointTypeFlag": ptype,
            "rwFlag": rw,
            "profileId": profile_id,
            "enableFlag": "ENABLE",
            "baseValue": 0,
            "multiple": 1,
            "valueDecimal": 3,
        }
        if unit:
            body["unit"] = unit
        created = api.call("POST", "/manager/point/add", body)
        print(f"point    : {name} ({created['id']}) — created")

    created = api.call(
        "POST",
        "/manager/device/add",
        {
            "deviceName": DEVICE_NAME,
            "deviceCode": DEVICE_CODE,
            "driverId": driver_id,
            "profileId": profile_id,
            "enableFlag": "ENABLE",
        },
    )
    print(f"device   : {created['id']} ({DEVICE_NAME}) — created")
    return str(created["id"]), profile_id


def ensure_driver_point_configs(api, driver_id, device_id, profile_id, attribute_ids):
    existing = unwrap_items(
        api.call("GET", f"/manager/driver_attribute_config/list_by_device_id?device_id={device_id}")
    )
    bound = {str(row.get("attributeId")) for row in existing}
    for code, value in (("host", "localhost"), ("port", "18600")):
        attribute_id = attribute_ids["driver"].get(code)
        if not attribute_id:
            print(f"warn     : driver attribute '{code}' not registered — is {DRIVER_CODE} running?")
            continue
        if attribute_id in bound:
            print(f"drv cfg  : {code}={value} — reuse")
            continue
        api.call(
            "POST",
            "/manager/driver_attribute_config/add",
            {"deviceId": device_id, "attributeId": attribute_id, "configValue": value},
        )
        print(f"drv cfg  : {code}={value} — created")

    points = unwrap_items(api.call("GET", f"/manager/point/list_by_profile_id?profile_id={profile_id}"))
    existing = unwrap_items(
        api.call("GET", f"/manager/point_attribute_config/list_by_device_id?device_id={device_id}")
    )
    bound = {(str(row.get("pointId")), str(row.get("attributeId"))) for row in existing}
    tag_attribute = attribute_ids["point"].get("tag")
    for point in points:
        point_id = str(point.get("id"))
        if not tag_attribute:
            print("warn     : point attribute 'tag' not registered — is " + DRIVER_CODE + " running?")
            break
        if (point_id, tag_attribute) in bound:
            print(f"point cfg: {point.get('pointName')} tag — reuse")
            continue
        api.call(
            "POST",
            "/manager/point_attribute_config/add",
            {"deviceId": device_id, "pointId": point_id, "attributeId": tag_attribute, "configValue": "TAG"},
        )
        print(f"point cfg: {point.get('pointName')} tag — created")


def ensure_events(api, device_id, profile_id, attribute_ids):
    existing = unwrap_items(api.call("GET", f"/manager/event/list_by_profile_id?profile_id={profile_id}"))
    for name, code, event_type, level in EVENTS:
        event = find_by(existing, eventCode=code)
        if event:
            event_id = str(event["id"])
            print(f"event    : {code} ({event_id}) — reuse")
        else:
            created = api.call(
                "POST",
                "/manager/event/add",
                {
                    "eventName": name,
                    "eventCode": code,
                    "eventTypeFlag": event_type,
                    "eventLevelFlag": level,
                    "profileId": profile_id,
                    "enableFlag": "ENABLE",
                },
            )
            event_id = str(created["id"])
            print(f"event    : {code} ({event_id}) — created")

        params = unwrap_items(api.call("GET", f"/manager/event_param/list_by_event_id?event_id={event_id}"))
        for param_name, param_code, param_type in EVENT_PARAMS:
            if find_by(params, paramCode=param_code):
                continue
            api.call(
                "POST",
                "/manager/event_param/add",
                {
                    "paramName": param_name,
                    "paramCode": param_code,
                    "paramTypeFlag": param_type,
                    "eventId": event_id,
                    "enableFlag": "ENABLE",
                },
            )
            print(f"evt param: {code}.{param_code} — created")

        configs = unwrap_items(
            api.call(
                "GET",
                f"/manager/event_attribute_config/list_by_device_id_and_event_id?device_id={device_id}&event_id={event_id}",
            )
        )
        bound = {str(row.get("attributeId")) for row in configs}
        for attribute_code, value in (("eventCodePath", "$.eventCode"), ("payloadPath", "$.payload")):
            attribute_id = attribute_ids["event"].get(attribute_code)
            if not attribute_id:
                print(f"warn     : event attribute '{attribute_code}' not registered — is {DRIVER_CODE} running?")
                continue
            if attribute_id in bound:
                continue
            api.call(
                "POST",
                "/manager/event_attribute_config/add",
                {
                    "deviceId": device_id,
                    "eventId": event_id,
                    "attributeId": attribute_id,
                    "configValue": value,
                },
            )
            print(f"evt cfg  : {code} {attribute_code}={value} — created")


def ensure_demo_rule(api, profile_id):
    """One demo threshold rule on the Temperature point (alarm engine feed)."""
    existing = unwrap_items(api.call("POST", "/data/rule/list", {"offset": 0, "limit": 200}))
    if find_by(existing, ruleCode=RULE_CODE):
        print(f"rule     : {RULE_CODE} — reuse")
        return

    points = unwrap_items(api.call("GET", f"/manager/point/list_by_profile_id?profile_id={profile_id}"))
    point = find_by(points, pointName=RULE_POINT)
    if not point:
        print(f"warn     : point '{RULE_POINT}' not found — demo rule skipped")
        return

    created = api.call(
        "POST",
        "/data/rule/add",
        {
            "alarmTargetTypeFlag": "POINT",
            "ruleName": RULE_NAME,
            "ruleCode": RULE_CODE,
            "entityId": str(point["id"]),
            "enableFlag": "ENABLE",
            "ruleExt": {
                "content": {
                    "condition": {"field": "numValue", "operator": ">", "threshold": 80, "unit": "°C"},
                    "recovery": {"enabled": True, "operator": "<=", "threshold": 75, "duration": "PT1M"},
                    "severity": "P1",
                    "eventType": "OVERHEAT",
                    "labels": ["virtual-demo"],
                }
            },
        },
    )
    print(f"rule     : {RULE_CODE} ({created.get('id')}) — created")


def ensure_command(api, device_id, profile_id, attribute_ids):
    existing = unwrap_items(api.call("GET", f"/manager/command/list_by_profile_id?profile_id={profile_id}"))
    command = find_by(existing, commandCode=COMMAND_CODE)
    if command:
        command_id = str(command["id"])
        print(f"command  : {COMMAND_CODE} ({command_id}) — reuse")
    else:
        created = api.call(
            "POST",
            "/manager/command/add",
            {
                "commandName": COMMAND_NAME,
                "commandCode": COMMAND_CODE,
                "commandTypeFlag": "ACTION",
                "callTypeFlag": "SYNC",
                "timeout": 30,
                "profileId": profile_id,
                "enableFlag": "ENABLE",
            },
        )
        command_id = str(created["id"])
        print(f"command  : {COMMAND_CODE} ({command_id}) — created")

    params = unwrap_items(api.call("GET", f"/manager/command_param/list_by_command_id?command_id={command_id}"))
    for name, code, direction, param_type, required, default in COMMAND_PARAMS:
        if find_by(params, paramCode=code):
            continue
        api.call(
            "POST",
            "/manager/command_param/add",
            {
                "paramName": name,
                "paramCode": code,
                "paramDirectionFlag": direction,
                "paramTypeFlag": param_type,
                "requiredFlag": required,
                "defaultValue": default,
                "commandId": command_id,
                "enableFlag": "ENABLE",
            },
        )
        print(f"cmd param: {code} — created")

    configs = unwrap_items(
        api.call(
            "GET",
            f"/manager/command_attribute_config/list_by_device_id_and_command_id?device_id={device_id}&command_id={command_id}",
        )
    )
    bound = {str(row.get("attributeId")) for row in configs}
    templates = (
        ("payloadTemplate", "${value}"),
        ("responseTemplate", '{"status":"accepted","device":"${deviceCode}","command":"${commandCode}"}'),
    )
    for attribute_code, value in templates:
        attribute_id = attribute_ids["command"].get(attribute_code)
        if not attribute_id:
            print(f"warn     : command attribute '{attribute_code}' not registered — is {DRIVER_CODE} running?")
            continue
        if attribute_id in bound:
            continue
        api.call(
            "POST",
            "/manager/command_attribute_config/add",
            {
                "deviceId": device_id,
                "commandId": command_id,
                "attributeId": attribute_id,
                "configValue": value,
            },
        )
        print(f"cmd cfg  : {attribute_code} — created")
    return command_id


def verify(api, device_id, command_id):
    latest = api.call(
        "POST",
        "/data/point_value/latest",
        {"deviceId": device_id, "offset": 0, "limit": 5},
    )
    items = unwrap_items(latest)
    print(f"\nverify   : point_value/latest -> {len(items)} row(s)")
    for row in items[:5]:
        print(f"           {row.get('pointName', row.get('pointId'))} = {row.get('calValue')}")

    if command_id:
        try:
            result = api.call(
                "POST",
                "/data/command_history/call",
                {"deviceId": device_id, "commandId": command_id, "paramValues": {"mode": "graceful"}},
            )
            print(f"verify   : command call -> {json.dumps(result, ensure_ascii=False)[:300]}")
        except RuntimeError as error:
            # The dispatch may legitimately fail on a stale center build (e.g.
            # before the markSent binding fix ships) — report, don't abort.
            print(f"verify   : command call failed -> {error}")

    print("verify   : event history is reported by the driver every 30s — "
          "check /data/event_history/list or the UI shortly")


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", default="http://127.0.0.1:8000/api/v3")
    parser.add_argument("--tenant", default="default")
    parser.add_argument("--user", default="dc3")
    parser.add_argument("--password", default="dc3dc3dc3")
    args = parser.parse_args()

    api = Api(args.base, args.tenant, args.user, args.password)
    print("login    : ok")

    drivers = unwrap_items(
        api.call("POST", "/manager/driver/list", {"offset": 0, "limit": 50, "driverCode": DRIVER_CODE})
    )
    driver = find_by(drivers, driverCode=DRIVER_CODE)
    if not driver:
        print(f"error    : driver {DRIVER_CODE} not registered — start dc3-driver-virtual first")
        sys.exit(1)
    driver_id = str(driver["id"])
    print(f"driver   : {driver_id} ({DRIVER_CODE})")

    attribute_ids = {
        "driver": ensure_attr_ids(api, "/manager/driver_attribute", driver_id),
        "point": ensure_attr_ids(api, "/manager/point_attribute", driver_id),
        "event": ensure_attr_ids(api, "/manager/event_attribute", driver_id),
        "command": ensure_attr_ids(api, "/manager/command_attribute", driver_id),
    }

    device_id, profile_id = ensure_device_chain(api, driver_id)
    ensure_driver_point_configs(api, driver_id, device_id, profile_id, attribute_ids)
    ensure_events(api, device_id, profile_id, attribute_ids)
    command_id = ensure_command(api, device_id, profile_id, attribute_ids)
    ensure_demo_rule(api, profile_id)
    verify(api, device_id, command_id)


if __name__ == "__main__":
    main()
