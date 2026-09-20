-- Copyright 2016-present the IoT DC3 original author or authors.
--
-- Schema fingerprint resync for databases initialized before the
-- initdb documentation pass (2026-09).
--
-- WHY: the canonical fingerprint (dc3/bin/schema_fingerprint.py) hashes the
-- initdb SQL files verbatim, comments and whitespace included. The 2026-09
-- documentation commit annotated 07-observability and 08-runtime with the
-- same comment grammar the other seeds already follow — zero DDL change
-- (verified by a comment- and whitespace-stripped comparison) — which moved
-- the canonical hash from 8297d4b5… to 51468116….
--
-- initdb seeds only run on FIRST container start, so existing volumes keep
-- the old fingerprint row and every center then refuses to start with
-- "schema fingerprint mismatch". Databases on 8297d4b5… are structurally
-- identical to the new contract; this resync only rewrites the marker.
--
-- Run against an already-initialized database:
--   docker exec -i <postgres-container> psql -U dc3 -d dc3 \
--     -v ON_ERROR_STOP=1 < 02-schema-fingerprint-resync.sql
--
-- Verify afterwards:
--   SELECT fingerprint_version, ddl_hash FROM public.dc3_schema_fingerprint;
--   -- expect: 2 | 5146811645192cebabe055c434a2f278c92174d600581f993e69adc7a3893226

BEGIN;

UPDATE public.dc3_schema_fingerprint
SET ddl_hash      = '5146811645192cebabe055c434a2f278c92174d600581f993e69adc7a3893226',
    generated_at  = CURRENT_TIMESTAMP
WHERE fingerprint_version = 2
  AND ddl_hash = '8297d4b51aa58ac0a5546ca5e7d494deb530bb8b125d0039f74fb0e18208f4ab';

COMMIT;
