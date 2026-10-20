-- Copyright 2016-present the IoT DC3 original author or authors.
--
-- Seed the agentic API permission resources into dc3_auth.dc3_resource.
--
-- WHY: initdb seeds only run on FIRST container start (Docker entrypoint
-- /docker-entrypoint-initdb.d). The agentic permission codes
-- ({dc3-center-agentic}:{domain}:{scope}, one row per distinct code) were never part
-- of the seed: on a fresh database the agentic permissions only became grantable
-- after dc3-center-agentic first booted and its resource registrar ran, and never
-- on deployments with the registrar disabled. The canonical seed rows now live in
-- initdb/02-iot-dc3-auth.sql; this migration converges databases initialized before
-- that seed existed.
--
-- COEXISTENCE with the runtime resource registrar: the registrar
-- (ReactiveResourceRegistrySyncServiceImpl) reconciles type-6 rows for the service
-- by resource_code and rewrites entity_id and parent on first sync, so these rows
-- are adopted, not duplicated (the unique index idx_resource_code_service_unique
-- covers resource_code + service_name). entity_id is -1, NOT 0: the registrar
-- treats type-6 rows with entity_id 0 as grouping nodes and would try to re-insert
-- the same codes, violating the unique index. Rows whose codes no longer match any
-- live endpoint are soft-deleted by the registrar's stale-leaf cleanup, which also
-- retires the pre-fix verb codes (chat:list, action:list-for-confirm, ...) on
-- databases where an older agentic center already registered them.
--
-- IDEMPOTENT: safe to re-run — INSERT ... ON CONFLICT DO NOTHING against the partial
-- unique index (resource_code, service_name) WHERE deleted = 0 AND resource_code <> ''.
-- This migration changes no DDL, so the schema fingerprint is intentionally untouched.
--
-- Run against an already-initialized database:
--   docker exec -i <postgres-container> psql -U dc3 -d dc3 \
--     -v ON_ERROR_STOP=1 < 05-agentic-permission-resources.sql
--
-- Verify afterwards:
--   SELECT resource_code FROM dc3_auth.dc3_resource
--     WHERE service_name = 'dc3-center-agentic' AND deleted = 0
--   ORDER BY resource_code;  -- expect 21 rows: chat:add, session:{list,get,update,delete},
--                             -- message:list, attachment:{add,list,delete}, action:{list,update},
--                             -- model:{list,add,update,delete,get}, provider:{list,add,update,delete,get}

BEGIN;

SET search_path TO dc3_auth;

-- ----------------------------
-- dc3_resource: agentic API permission leaves
-- resource_type_flag: 6=API   resource_scope_flag: 0=add, 1=delete, 2=update, 3=list, 4=get
-- ----------------------------
INSERT INTO dc3_resource (id, parent_resource_id, resource_name, resource_code, service_name, resource_type_flag,
                          resource_scope_flag, entity_id, resource_ext, enable_flag, remark, creator_id, creator_name,
                          create_time, operator_id, operator_name, operate_time, deleted)
VALUES (10001, 0, 'Create Chat Completion', 'dc3-center-agentic:chat:add', 'dc3-center-agentic', 6, 0, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10002, 0, 'List Sessions', 'dc3-center-agentic:session:list', 'dc3-center-agentic', 6, 3, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10003, 0, 'Get Session', 'dc3-center-agentic:session:get', 'dc3-center-agentic', 6, 4, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10004, 0, 'Update Session', 'dc3-center-agentic:session:update', 'dc3-center-agentic', 6, 2, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10005, 0, 'Delete Session', 'dc3-center-agentic:session:delete', 'dc3-center-agentic', 6, 1, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10006, 0, 'List Session Messages', 'dc3-center-agentic:message:list', 'dc3-center-agentic', 6, 3, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10007, 0, 'Upload Attachment', 'dc3-center-agentic:attachment:add', 'dc3-center-agentic', 6, 0, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10008, 0, 'List Attachments', 'dc3-center-agentic:attachment:list', 'dc3-center-agentic', 6, 3, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10009, 0, 'Delete Attachment', 'dc3-center-agentic:attachment:delete', 'dc3-center-agentic', 6, 1, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10010, 0, 'List Pending Agent Actions', 'dc3-center-agentic:action:list', 'dc3-center-agentic', 6, 3, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10011, 0, 'Confirm Or Reject Agent Action', 'dc3-center-agentic:action:update', 'dc3-center-agentic', 6, 2,
        -1, '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1,
        'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10012, 0, 'List AI Models', 'dc3-center-agentic:model:list', 'dc3-center-agentic', 6, 3, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10013, 0, 'Add AI Model Configuration', 'dc3-center-agentic:model:add', 'dc3-center-agentic', 6, 0, -1, '{}',
        0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10014, 0, 'Update AI Model Configuration', 'dc3-center-agentic:model:update', 'dc3-center-agentic', 6, 2, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10015, 0, 'Delete AI Model Configuration', 'dc3-center-agentic:model:delete', 'dc3-center-agentic', 6, 1, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10016, 0, 'Check AI Model Configuration', 'dc3-center-agentic:model:get', 'dc3-center-agentic', 6, 4, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10017, 0, 'List Model Providers', 'dc3-center-agentic:provider:list', 'dc3-center-agentic', 6, 3, -1, '{}',
        0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10018, 0, 'Add Model Provider', 'dc3-center-agentic:provider:add', 'dc3-center-agentic', 6, 0, -1, '{}', 0,
        'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10019, 0, 'Update Model Provider', 'dc3-center-agentic:provider:update', 'dc3-center-agentic', 6, 2, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10020, 0, 'Delete Model Provider', 'dc3-center-agentic:provider:delete', 'dc3-center-agentic', 6, 1, -1,
        '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1, 'dc3',
        '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0),
       (10021, 0, 'Check Model Provider Connectivity', 'dc3-center-agentic:provider:get', 'dc3-center-agentic', 6, 4,
        -1, '{}', 0, 'Seeded agentic permission; linked to its API row by the resource registrar on first sync.', 1,
        'dc3', '2026-05-01 00:00:00 +00:00', 1, 'dc3', '2026-05-01 00:00:00 +00:00', 0)
ON CONFLICT (resource_code, service_name) WHERE deleted = 0 AND resource_code <> '' DO NOTHING;

COMMIT;
