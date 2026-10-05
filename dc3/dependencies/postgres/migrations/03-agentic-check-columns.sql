-- Copyright 2016-present the IoT DC3 original author or authors.
--
-- Health-profile columns for the agentic provider/model connectivity check.
--
-- WHY: initdb seeds only run on FIRST container start (Docker entrypoint
-- /docker-entrypoint-initdb.d). The connectivity check feature (L1 model-list
-- probe + L2 minimal chat probe) persists its latest result on
-- dc3_agentic.dc3_model_provider / dc3_agentic.dc3_model_config, which now
-- carry last_check_* columns, and both tables' operate_time trigger gains a
-- WHEN clause so health-profile writes do not bump the config-edit timestamp.
-- The canonical DDL lives in initdb/06-iot-dc3-agentic.sql.
--
-- IDEMPOTENT: safe to re-run — ADD COLUMN IF NOT EXISTS, COMMENT ON COLUMN,
-- DROP TRIGGER IF EXISTS + CREATE TRIGGER, and constraint drop/add converge.
--
-- Run against an already-initialized database:
--   docker exec -i <postgres-container> psql -U dc3 -d dc3 \
--     -v ON_ERROR_STOP=1 < 03-agentic-check-columns.sql
--
-- Verify afterwards:
--   SELECT fingerprint_version, ddl_hash FROM public.dc3_schema_fingerprint;
--   -- expect: 2 | c8ff34a0c7534ae479137f4ff95b1c41caa8b666b448870de419933d679fbb85
--   SELECT column_name FROM information_schema.columns
--     WHERE table_schema = 'dc3_agentic' AND column_name LIKE 'last_check%'
--   ORDER BY table_name, column_name;  -- expect 11 rows

BEGIN;

SET search_path TO dc3_agentic;

-- ----------------------------
-- dc3_model_provider: health-profile columns
-- ----------------------------
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_status SMALLINT;
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_time TIMESTAMPTZ;
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_latency_ms INTEGER;
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_error_type VARCHAR(32);
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_error_message VARCHAR(255);
ALTER TABLE dc3_model_provider ADD COLUMN IF NOT EXISTS last_check_model VARCHAR(128);

COMMENT ON COLUMN dc3_model_provider.last_check_status IS 'Last connectivity check status, NULL: not checked, 1: pass, 2: fail';
COMMENT ON COLUMN dc3_model_provider.last_check_time IS 'Last connectivity check completion time (UTC)';
COMMENT ON COLUMN dc3_model_provider.last_check_latency_ms IS 'Last connectivity check round-trip latency in milliseconds';
COMMENT ON COLUMN dc3_model_provider.last_check_error_type IS 'Last connectivity check error type code, e.g. AUTH_FAILED';
COMMENT ON COLUMN dc3_model_provider.last_check_error_message IS 'Last connectivity check sanitized error message, never contains credentials';
COMMENT ON COLUMN dc3_model_provider.last_check_model IS 'Model identifier used by the last L2 chat probe';

ALTER TABLE dc3_model_provider DROP CONSTRAINT IF EXISTS chk_model_provider_last_check_status;
ALTER TABLE dc3_model_provider
    ADD CONSTRAINT chk_model_provider_last_check_status CHECK (last_check_status IS NULL OR last_check_status IN (1, 2));

-- Bump operate_time only when a non-health-profile column changes, so last_check_* narrow updates do not pollute the config-edit timestamp.
DROP TRIGGER IF EXISTS update_operate_time_trigger ON dc3_model_provider;
CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_model_provider
    FOR EACH ROW
    WHEN (NEW.id IS DISTINCT FROM OLD.id
        OR NEW.name IS DISTINCT FROM OLD.name
        OR NEW.provider_type IS DISTINCT FROM OLD.provider_type
        OR NEW.base_url IS DISTINCT FROM OLD.base_url
        OR NEW.api_key IS DISTINCT FROM OLD.api_key
        OR NEW.default_flag IS DISTINCT FROM OLD.default_flag
        OR NEW.enable_flag IS DISTINCT FROM OLD.enable_flag
        OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
        OR NEW.remark IS DISTINCT FROM OLD.remark
        OR NEW.creator_id IS DISTINCT FROM OLD.creator_id
        OR NEW.creator_name IS DISTINCT FROM OLD.creator_name
        OR NEW.create_time IS DISTINCT FROM OLD.create_time
        OR NEW.operator_id IS DISTINCT FROM OLD.operator_id
        OR NEW.operator_name IS DISTINCT FROM OLD.operator_name
        OR NEW.operate_time IS DISTINCT FROM OLD.operate_time
        OR NEW.deleted IS DISTINCT FROM OLD.deleted)
    EXECUTE FUNCTION update_operate_time();

-- ----------------------------
-- dc3_model_config: health-profile columns
-- ----------------------------
ALTER TABLE dc3_model_config ADD COLUMN IF NOT EXISTS last_check_status SMALLINT;
ALTER TABLE dc3_model_config ADD COLUMN IF NOT EXISTS last_check_time TIMESTAMPTZ;
ALTER TABLE dc3_model_config ADD COLUMN IF NOT EXISTS last_check_latency_ms INTEGER;
ALTER TABLE dc3_model_config ADD COLUMN IF NOT EXISTS last_check_error_type VARCHAR(32);
ALTER TABLE dc3_model_config ADD COLUMN IF NOT EXISTS last_check_error_message VARCHAR(255);

COMMENT ON COLUMN dc3_model_config.last_check_status IS 'Last connectivity check status, NULL: not checked, 1: pass, 2: fail';
COMMENT ON COLUMN dc3_model_config.last_check_time IS 'Last connectivity check completion time (UTC)';
COMMENT ON COLUMN dc3_model_config.last_check_latency_ms IS 'Last connectivity check round-trip latency in milliseconds';
COMMENT ON COLUMN dc3_model_config.last_check_error_type IS 'Last connectivity check error type code, e.g. AUTH_FAILED';
COMMENT ON COLUMN dc3_model_config.last_check_error_message IS 'Last connectivity check sanitized error message, never contains credentials';

ALTER TABLE dc3_model_config DROP CONSTRAINT IF EXISTS chk_model_config_last_check_status;
ALTER TABLE dc3_model_config
    ADD CONSTRAINT chk_model_config_last_check_status CHECK (last_check_status IS NULL OR last_check_status IN (1, 2));

-- Bump operate_time only when a non-health-profile column changes, so last_check_* narrow updates do not pollute the config-edit timestamp.
DROP TRIGGER IF EXISTS update_operate_time_trigger ON dc3_model_config;
CREATE TRIGGER update_operate_time_trigger
    BEFORE UPDATE
    ON dc3_model_config
    FOR EACH ROW
    WHEN (NEW.id IS DISTINCT FROM OLD.id
        OR NEW.model IS DISTINCT FROM OLD.model
        OR NEW.label IS DISTINCT FROM OLD.label
        OR NEW.provider_id IS DISTINCT FROM OLD.provider_id
        OR NEW.stream IS DISTINCT FROM OLD.stream
        OR NEW.tool_call IS DISTINCT FROM OLD.tool_call
        OR NEW.vision IS DISTINCT FROM OLD.vision
        OR NEW.reasoning IS DISTINCT FROM OLD.reasoning
        OR NEW.temperature IS DISTINCT FROM OLD.temperature
        OR NEW.max_tokens IS DISTINCT FROM OLD.max_tokens
        OR NEW.default_flag IS DISTINCT FROM OLD.default_flag
        OR NEW.enable_flag IS DISTINCT FROM OLD.enable_flag
        OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
        OR NEW.remark IS DISTINCT FROM OLD.remark
        OR NEW.creator_id IS DISTINCT FROM OLD.creator_id
        OR NEW.creator_name IS DISTINCT FROM OLD.creator_name
        OR NEW.create_time IS DISTINCT FROM OLD.create_time
        OR NEW.operator_id IS DISTINCT FROM OLD.operator_id
        OR NEW.operator_name IS DISTINCT FROM OLD.operator_name
        OR NEW.operate_time IS DISTINCT FROM OLD.operate_time
        OR NEW.deleted IS DISTINCT FROM OLD.deleted)
    EXECUTE FUNCTION update_operate_time();

-- ----------------------------
-- Schema fingerprint resync (databases initialized before this feature)
-- ----------------------------
UPDATE public.dc3_schema_fingerprint
SET ddl_hash      = 'c8ff34a0c7534ae479137f4ff95b1c41caa8b666b448870de419933d679fbb85',
    generated_at  = CURRENT_TIMESTAMP
WHERE fingerprint_version = 2
  AND ddl_hash = '5146811645192cebabe055c434a2f278c92174d600581f993e69adc7a3893226';

COMMIT;
