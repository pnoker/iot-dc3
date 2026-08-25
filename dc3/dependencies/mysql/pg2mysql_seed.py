#!/usr/bin/env python3

#
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
#

"""PG seed -> MySQL 8 seed translator for iot-dc3 (storage-abstraction.md R2b).

Usage (from iot-dc3/): regenerate every derived file after editing the
PostgreSQL seed:

    for pair in "01-iot-dc3-common:dc3_manager" "02-iot-dc3-auth:dc3_auth" \
                "03-iot-dc3-data:dc3_data" "04-iot-dc3-manager:dc3_manager" \
                "06-iot-dc3-agentic:dc3_agentic"; do
        f="${pair%%:*}"; db="${pair##*:}"
        python3 dc3/dependencies/mysql/pg2mysql_seed.py \
            "dc3/dependencies/postgres/initdb/$f.sql" "dc3/dependencies/mysql/initdb/$f.sql" "$db"
    done

05-iot-dc3-history is hand-maintained (only the dc3_point_latest projection —
external TSDB deployments) and 00 is a placeholder; neither is derived.
Verified against mysql:8.4 (docker-entrypoint initdb order).
"""
import re
import sys

HEADER = """--
-- MySQL 8 seed for IoT DC3 — auto-derived from the PostgreSQL seed; regenerate
-- together with it (docs/design/storage-abstraction.md §3). Requires MySQL 8.0+
-- (expression DEFAULTs, CTEs, SKIP LOCKED, JSON). Timestamps are DATETIME(6)
-- stored in UTC. Table/column comments live in the PostgreSQL seed (source of
-- truth). TimescaleDB artifacts are absent on purpose: a MySQL core requires an
-- external time-series store (docs/tsdb-stores.md); dc3_history keeps only the
-- dc3_point_latest projection.
--
"""

REVISION_MYSQL = """
--
-- Row-level revision tracking, same semantics as the PostgreSQL row trigger:
-- a device insert/delete/meaningful-update bumps the owning (or formerly
-- owning) driver's device-set revision; row-local +1, no sequence objects.
--
DELIMITER $$
CREATE TRIGGER track_driver_device_revision_insert AFTER INSERT ON dc3_device FOR EACH ROW
BEGIN
    IF NEW.deleted = 0 AND NEW.enable_flag = 0 THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (NEW.tenant_id, NEW.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$

CREATE TRIGGER track_driver_device_revision_delete AFTER DELETE ON dc3_device FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND OLD.enable_flag = 0 THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (OLD.tenant_id, OLD.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$

CREATE TRIGGER track_driver_device_revision_update AFTER UPDATE ON dc3_device FOR EACH ROW
BEGIN
    IF OLD.deleted = 0 AND OLD.enable_flag = 0
        AND (NEW.tenant_id != OLD.tenant_id OR NEW.driver_id != OLD.driver_id
            OR NEW.deleted != OLD.deleted OR NEW.enable_flag != OLD.enable_flag) THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (OLD.tenant_id, OLD.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
    IF NEW.deleted = 0 AND NEW.enable_flag = 0
        AND (NEW.tenant_id != OLD.tenant_id OR NEW.driver_id != OLD.driver_id
            OR NEW.deleted != OLD.deleted OR NEW.enable_flag != OLD.enable_flag) THEN
        INSERT INTO dc3_driver_device_revision (tenant_id, driver_id, revision)
        VALUES (NEW.tenant_id, NEW.driver_id, 1)
        ON DUPLICATE KEY UPDATE revision = dc3_driver_device_revision.revision + 1;
    END IF;
END$$
DELIMITER ;
"""


def strip_operate_time_triggers(s):
    s = re.sub(r"CREATE OR REPLACE FUNCTION update_operate_time\(\).*?LANGUAGE plpgsql;\n?", "", s, flags=re.S)
    s = re.sub(r"CREATE TRIGGER update_operate_time_\w+.*?EXECUTE FUNCTION update_operate_time\(\);\n?", "", s, flags=re.S)
    return s


def drop_pg_specific(s):
    s = re.sub(r"CREATE EXTENSION IF NOT EXISTS \w+;\n?", "", s)
    s = re.sub(r"LOAD '[^']*';\n?", "", s)
    s = re.sub(r"SELECT \*\s*\n?FROM public\.create_hypertable\(.*?\);\n?", "", s, flags=re.S)
    s = re.sub(r"SELECT\s+\*\s+FROM\s+public\.create_hypertable\(.*?\);", "", s, flags=re.S)
    s = re.sub(r"SELECT create_hypertable\(.*?\);\n?", "", s, flags=re.S)
    s = re.sub(r"SELECT \* FROM public\.add_dimension\(.*?\);\n?", "", s, flags=re.S)
    s = re.sub(r"ALTER TABLE \w+\s*\n?\s*SET \(\s*timescaledb[^\)]*\);\n?", "", s, flags=re.S)
    s = re.sub(r"SELECT public\.add_(compression|retention)_policy\(.*?\);\n?", "", s, flags=re.S)
    s = re.sub(r"CREATE (UNIQUE )?INDEX [^;]*\bWHERE\b[^;]*;", "", s, flags=re.S)
    return s


def drop_comments(s):
    s = re.sub(r"COMMENT ON TABLE \w+ IS '(?:[^']|'')*';\n?", "", s)
    s = re.sub(r"COMMENT ON COLUMN \w+\.\w+ IS '(?:[^']|'')*';\n?", "", s)
    s = re.sub(r"COMMENT ON SEQUENCE \w+ IS '(?:[^']|'')*';\n?", "", s)
    return s


def convert_revision_triggers(s):
    s = re.sub(r"-- Device ownership is recomputed.*?LANGUAGE plpgsql;\n?", "", s, flags=re.S)
    s = re.sub(r"CREATE TRIGGER track_driver_device_revision_\w+\s+AFTER \w+\s+ON dc3_device\s+FOR EACH ROW\s+EXECUTE FUNCTION track_driver_device_revision_change\(\);\n?", "", s, flags=re.S)
    if "dc3_driver_device_revision" in s:
        s = s.rstrip() + "\n" + REVISION_MYSQL
    return s


def widen_keyed_text(s):
    """TEXT columns referenced by PRIMARY KEY/UNIQUE/INDEX definitions get
    VARCHAR(191) — MySQL cannot index unbounded TEXT and 191 stays within the
    utf8mb4 767-byte index prefix."""
    out = []
    i = 0
    pattern = re.compile(r"CREATE TABLE (\w+)\s*\(", re.S)
    while True:
        m = pattern.search(s, i)
        if not m:
            out.append(s[i:])
            break
        depth = 0
        j = m.end() - 1
        while j < len(s):
            if s[j] == '(':
                depth += 1
            elif s[j] == ')':
                depth -= 1
                if depth == 0:
                    break
            j += 1
        body = s[m.end():j]
        keyed = set()
        for line in body.split('\n'):
            t = line.strip()
            km = re.match(r"(?:PRIMARY KEY|UNIQUE(?: KEY)?|KEY|INDEX)\s*\(([^)]*)\)", t)
            if km:
                for col in km.group(1).split(','):
                    name = col.strip().split()[0].strip('"`')
                    keyed.add(name)
        for im in re.finditer(r"CREATE (?:UNIQUE )?INDEX \w+\s+ON " + m.group(1) + r" \(([^)]*)\)", s):
            for col in im.group(1).split(','):
                keyed.add(col.strip().split()[0].strip('"`'))
        if keyed:
            lines = []
            for line in body.split('\n'):
                t = line.strip()
                cm = re.match(r'"?(\w+)"?\s+TEXT\b', t)
                if cm and cm.group(1) in keyed:
                    line = re.sub(r"\bTEXT\b", "VARCHAR(191)", line, count=1)
                lines.append(line)
            body = '\n'.join(lines)
        out.append(s[i:m.end()])
        out.append(body)
        out.append(')')
        i = j + 1
    return ''.join(out)


def translate_types(s):
    s = re.sub(r"\bTIMESTAMPTZ\b", "DATETIME(6)", s)
    s = re.sub(r"\bJSONB\b", "JSON", s)
    s = re.sub(r"::TEXT\b", "", s, flags=re.I)
    s = re.sub(r"'(\{[^']*\})'::JSON", r"('\1')", s)
    s = re.sub(r"DEFAULT (\S+)::\w+", r"DEFAULT \1", s)
    s = s.replace("DEFAULT CURRENT_TIMESTAMP NOT NULL", "DEFAULT CURRENT_TIMESTAMP(6) NOT NULL")
    s = s.replace("DEFAULT CURRENT_TIMESTAMP,", "DEFAULT CURRENT_TIMESTAMP(6),")
    return s


def main(src_path, dst_path, database):
    s = open(src_path).read()
    s = strip_operate_time_triggers(s)
    s = drop_pg_specific(s)
    s = drop_comments(s)
    s = convert_revision_triggers(s)
    s = translate_types(s)
    s = widen_keyed_text(s)
    s = re.sub(r"CREATE SCHEMA IF NOT EXISTS \w+;", f"CREATE DATABASE IF NOT EXISTS {database}\n    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;", s)
    s = re.sub(r"SET search_path TO [\w, ]+;", f"USE {database};", s)
    s = re.sub(r"\n?\s*INCLUDE \([^)]*\)", "", s)
    s = re.sub(r"'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}(?:\.\d+)?) \+00:00'", r"'\1'", s)
    # TEXT/JSON columns need expression defaults on MySQL 8 (no literal defaults)
    s = re.sub(r"(TEXT\s+)DEFAULT ('[^']*')", r"\1DEFAULT (\2)", s)
    s = s.replace("operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) NOT NULL",
                  "operate_time DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) NOT NULL")
    # MySQL string literals eat backslash escapes (PostgreSQL does not): the
    # seed's embedded-JSON values rely on \" surviving to the JSON parser, so
    # this session disables backslash escaping for the whole (dedicated) load.
    s = "SET sql_mode = CONCAT(@@sql_mode, ',NO_BACKSLASH_ESCAPES');\n\n" + s
    open(dst_path, 'w').write(HEADER + s)
    print(f"{src_path} -> {dst_path} ({database})")


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], sys.argv[3])
