#!/usr/bin/env python3
"""Onboard a device + points for the VirtualDriver so it reports simulated data."""
import json
import sys
import urllib.error
import urllib.request

B = "http://127.0.0.1:8000/api/v3"
DRIVER_ID = "3727604544552135439"
ATTR_HOST = "6204587825975890275"
ATTR_PORT = "4588366786701394775"
ATTR_TAG = "7413377365928887033"


def http(method, path, body=None, cookie=None):
    url = B + path
    data = json.dumps(body).encode() if body is not None else None
    hdr = {"Content-Type": "application/json"}
    if cookie:
        hdr["Cookie"] = cookie
        hdr["X-Auth-Tenant"] = "default"
        hdr["X-Auth-Login"] = "dc3"
    req = urllib.request.Request(url, data=data, headers=hdr, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as r:
            return r.status, r.read().decode()
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode()


def main():
    _, salt = http("POST", "/auth/token/salt", {"tenant": "default", "name": "dc3"})
    salt = salt.strip().strip('"')
    req = urllib.request.Request(
        B + "/auth/token/generate",
        data=json.dumps({"tenant": "default", "name": "dc3", "salt": salt, "password": "dc3dc3dc3"}).encode(),
        headers={"Content-Type": "application/json"},
        method="POST")
    with urllib.request.urlopen(req, timeout=20) as r:
        cookie = r.headers.get("Set-Cookie", "")
    if not cookie:
        print("LOGIN FAILED")
        sys.exit(1)
    print("login ok")

    def post(path, body):
        s, b = http("POST", path, body, cookie)
        if s not in (200, 201):
            print(f"  FAIL {path}: {s} {b[:200]}")
            sys.exit(1)
        return json.loads(b)

    prof = post("/manager/profile/add", {"profileName": "VirtualStress", "enableFlag": "ENABLE"})
    profile_id = str(prof.get("id"))
    print("profile:", profile_id)

    points = [
        ("Temperature", "temperature", "FLOAT", "READ_WRITE", "°C"),
        ("Status", "status", "BOOLEAN", "READ_ONLY", ""),
        ("DeviceName", "deviceName", "STRING", "READ_WRITE", ""),
    ]
    point_ids = []
    for name, code, ptype, rw, unit in points:
        body = {"pointName": name, "pointCode": code, "pointTypeFlag": ptype, "rwFlag": rw,
                "profileId": profile_id, "enableFlag": "ENABLE",
                "baseValue": 0, "multiple": 1, "valueDecimal": 3}
        if unit:
            body["unit"] = unit
        p = post("/manager/point/add", body)
        point_ids.append(str(p.get("id")))
        print(f"  point {code}: {point_ids[-1]}")

    dev = post("/manager/device/add", {"deviceName": "VirtualDemo", "deviceCode": "virtual-demo-1",
                                       "driverId": DRIVER_ID, "profileId": profile_id, "enableFlag": "ENABLE"})
    device_id = str(dev.get("id"))
    print("device:", device_id)

    post("/manager/driver_attribute_config/add",
         {"deviceId": device_id, "attributeId": ATTR_HOST, "configValue": "localhost"})
    post("/manager/driver_attribute_config/add",
         {"deviceId": device_id, "attributeId": ATTR_PORT, "configValue": "18600"})
    print("driver attribute configs: host+port bound")

    for pid in point_ids:
        post("/manager/point_attribute_config/add",
             {"deviceId": device_id, "pointId": pid, "attributeId": ATTR_TAG, "configValue": "TAG"})
    print("point attribute configs: tag bound")

    print(f"\nDONE device_id={device_id} points={point_ids}")


if __name__ == "__main__":
    main()
