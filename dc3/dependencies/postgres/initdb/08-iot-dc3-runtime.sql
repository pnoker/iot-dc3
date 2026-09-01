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

/* Runtime coordination tables shared by all reactive centers. */
SET search_path TO public;

CREATE TABLE IF NOT EXISTS dc3_schema_fingerprint
(
    fingerprint_version SMALLINT PRIMARY KEY,
    ddl_hash            TEXT        NOT NULL,
    schema_contract     TEXT        NOT NULL,
    id_format           TEXT        NOT NULL,
    time_format         TEXT        NOT NULL,
    json_format         TEXT        NOT NULL,
    generated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
COMMENT ON TABLE dc3_schema_fingerprint IS 'Canonical clean-DDL fingerprint used for startup validation';
COMMENT ON COLUMN dc3_schema_fingerprint.fingerprint_version IS 'Fingerprint schema version';
COMMENT ON COLUMN dc3_schema_fingerprint.ddl_hash IS 'SHA-256 hash of canonical initialization SQL';
COMMENT ON COLUMN dc3_schema_fingerprint.schema_contract IS 'Flag-day architecture contract identifier';
COMMENT ON COLUMN dc3_schema_fingerprint.id_format IS 'Identifier encoding contract';
COMMENT ON COLUMN dc3_schema_fingerprint.time_format IS 'Timestamp precision and timezone contract';
COMMENT ON COLUMN dc3_schema_fingerprint.json_format IS 'Canonical JSON encoding contract';
COMMENT ON COLUMN dc3_schema_fingerprint.generated_at IS 'Fingerprint generation timestamp in UTC';

CREATE TABLE IF NOT EXISTS dc3_operation
(
    operation_id    UUID PRIMARY KEY,
    tenant_id       BIGINT      NOT NULL,
    idempotency_key TEXT        NOT NULL,
    request_hash    CHAR(64)      NOT NULL,
    status          TEXT        NOT NULL,
    progress        SMALLINT    NOT NULL DEFAULT 0,
    result          JSONB,
    error           JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ,
    CONSTRAINT chk_dc3_operation_idempotency_key CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 191),
    CONSTRAINT chk_dc3_operation_progress CHECK (progress BETWEEN 0 AND 100),
    CONSTRAINT chk_dc3_operation_request_hash CHECK (request_hash ~ '^[0-9a-fA-F]{64}$'),
    CONSTRAINT chk_dc3_operation_status CHECK (status IN ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT uq_dc3_operation_tenant_key UNIQUE (tenant_id, idempotency_key)
);
COMMENT ON TABLE dc3_operation IS 'Long-running operation state and result';
COMMENT ON COLUMN dc3_operation.operation_id IS 'UUIDv7 operation identifier';
COMMENT ON COLUMN dc3_operation.tenant_id IS 'Owning tenant identifier';
COMMENT ON COLUMN dc3_operation.idempotency_key IS 'Client idempotency key; unique within tenant';
COMMENT ON COLUMN dc3_operation.request_hash IS 'Canonical request SHA-256';
COMMENT ON COLUMN dc3_operation.status IS 'Operation lifecycle status';
COMMENT ON COLUMN dc3_operation.progress IS 'Completion percentage from 0 to 100';
COMMENT ON COLUMN dc3_operation.result IS 'Successful operation result JSON';
COMMENT ON COLUMN dc3_operation.error IS 'Failure Problem Details JSON';
COMMENT ON COLUMN dc3_operation.created_at IS 'Creation timestamp in UTC';
COMMENT ON COLUMN dc3_operation.updated_at IS 'Last update timestamp in UTC';
COMMENT ON COLUMN dc3_operation.expires_at IS 'Optional expiration timestamp in UTC';

CREATE INDEX IF NOT EXISTS idx_dc3_operation_tenant_status ON dc3_operation (tenant_id, status, created_at DESC);
CREATE UNIQUE INDEX IF NOT EXISTS idx_dc3_operation_tenant_id ON dc3_operation (tenant_id, operation_id);

CREATE TABLE IF NOT EXISTS dc3_manager.dc3_device_import_job
(
    operation_id  UUID PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    driver_id     BIGINT      NOT NULL,
    profile_id    BIGINT      NOT NULL,
    operator_id   BIGINT      NOT NULL DEFAULT 0,
    operator_name TEXT        NOT NULL DEFAULT '',
    file_name     TEXT        NOT NULL,
    file_data     BYTEA       NOT NULL,
    claimed_by    TEXT        NOT NULL DEFAULT '',
    claimed_until TIMESTAMPTZ,
    attempts      INTEGER     NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_import_job_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES dc3_operation (tenant_id, operation_id) ON DELETE CASCADE,
    CONSTRAINT chk_device_import_job_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_device_import_job_file_size CHECK (octet_length(file_data) BETWEEN 1 AND 20971520)
);
CREATE INDEX IF NOT EXISTS idx_device_import_job_claim
    ON dc3_manager.dc3_device_import_job (claimed_until, created_at);
COMMENT ON TABLE dc3_manager.dc3_device_import_job IS 'Durable XLSX device-import job payload';

CREATE TABLE IF NOT EXISTS dc3_outbox
(
    event_id      UUID PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL,
    aggregate_type TEXT       NOT NULL,
    aggregate_id  UUID,
    event_type    TEXT        NOT NULL,
    payload       JSONB       NOT NULL,
    status        TEXT        NOT NULL DEFAULT 'PENDING',
    attempts      INTEGER     NOT NULL DEFAULT 0,
    available_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at    TIMESTAMPTZ,
    published_at  TIMESTAMPTZ,
    last_error    TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_dc3_outbox_status CHECK (status IN ('PENDING', 'CLAIMED', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_dc3_outbox_attempts CHECK (attempts >= 0)
);
CREATE INDEX IF NOT EXISTS idx_dc3_outbox_claim ON dc3_outbox (status, available_at, created_at);
CREATE INDEX IF NOT EXISTS idx_dc3_outbox_tenant ON dc3_outbox (tenant_id, created_at DESC);
COMMENT ON TABLE dc3_outbox IS 'Transactional outbox for durable event publication';
COMMENT ON COLUMN dc3_outbox.event_id IS 'UUIDv7 event identifier';
COMMENT ON COLUMN dc3_outbox.tenant_id IS 'Owning tenant identifier';
COMMENT ON COLUMN dc3_outbox.aggregate_type IS 'Aggregate type';
COMMENT ON COLUMN dc3_outbox.aggregate_id IS 'Aggregate UUID';
COMMENT ON COLUMN dc3_outbox.event_type IS 'Event type';
COMMENT ON COLUMN dc3_outbox.payload IS 'Canonical event JSON payload';
COMMENT ON COLUMN dc3_outbox.status IS 'Publication lifecycle status';
COMMENT ON COLUMN dc3_outbox.attempts IS 'Publication attempt count';
COMMENT ON COLUMN dc3_outbox.available_at IS 'Earliest retry publication time';
COMMENT ON COLUMN dc3_outbox.claimed_at IS 'Claim timestamp';
COMMENT ON COLUMN dc3_outbox.published_at IS 'Successful publication timestamp';
COMMENT ON COLUMN dc3_outbox.last_error IS 'Last publication failure';
COMMENT ON COLUMN dc3_outbox.created_at IS 'Creation timestamp in UTC';

CREATE TABLE IF NOT EXISTS dc3_idempotency
(
    tenant_id       BIGINT      NOT NULL,
    idempotency_key TEXT        NOT NULL,
    request_hash    TEXT        NOT NULL,
    operation_id    UUID        NOT NULL,
    status          TEXT        NOT NULL,
    response        JSONB,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at      TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (tenant_id, idempotency_key),
    CONSTRAINT chk_dc3_idempotency_key CHECK (length(btrim(idempotency_key)) BETWEEN 1 AND 191),
    CONSTRAINT chk_dc3_idempotency_request_hash CHECK (request_hash ~ '^[0-9a-fA-F]{64}$'),
    CONSTRAINT chk_dc3_idempotency_status CHECK (status IN ('PENDING', 'SUCCEEDED', 'FAILED', 'CANCELLED', 'EXPIRED')),
    CONSTRAINT fk_dc3_idempotency_operation FOREIGN KEY (tenant_id, operation_id)
        REFERENCES dc3_operation (tenant_id, operation_id)
);
CREATE INDEX IF NOT EXISTS idx_dc3_idempotency_expiry ON dc3_idempotency (expires_at);
COMMENT ON TABLE dc3_idempotency IS 'Tenant-scoped idempotency keys and operation results';
COMMENT ON COLUMN dc3_idempotency.tenant_id IS 'Owning tenant identifier';
COMMENT ON COLUMN dc3_idempotency.idempotency_key IS 'Client supplied idempotency key';
COMMENT ON COLUMN dc3_idempotency.request_hash IS 'Canonical request SHA-256';
COMMENT ON COLUMN dc3_idempotency.request_hash IS 'Canonical request SHA-256';
COMMENT ON COLUMN dc3_idempotency.operation_id IS 'Associated operation UUID';
COMMENT ON COLUMN dc3_idempotency.status IS 'Idempotency lifecycle status';
COMMENT ON COLUMN dc3_idempotency.response IS 'Canonical response JSON';
COMMENT ON COLUMN dc3_idempotency.created_at IS 'Creation timestamp in UTC';
COMMENT ON COLUMN dc3_idempotency.expires_at IS 'Expiry timestamp in UTC';

CREATE TABLE IF NOT EXISTS dc3_platform_lock
(
    lock_name    TEXT PRIMARY KEY,
    fencing_token BIGINT      NOT NULL,
    holder       UUID,
    expires_at   TIMESTAMPTZ NOT NULL
);
COMMENT ON TABLE dc3_platform_lock IS 'Database row locks with fencing tokens';
COMMENT ON COLUMN dc3_platform_lock.lock_name IS 'Logical lock name';
COMMENT ON COLUMN dc3_platform_lock.fencing_token IS 'Monotonically increasing fencing token';
COMMENT ON COLUMN dc3_platform_lock.holder IS 'Current holder UUID';
COMMENT ON COLUMN dc3_platform_lock.expires_at IS 'Lease expiration timestamp in UTC';

INSERT INTO dc3_schema_fingerprint
    (fingerprint_version, ddl_hash, schema_contract, id_format, time_format, json_format)
VALUES (2, 'bf799fdba88ca7a5be72b7111fd7584a4cea20609f158daa9e53af75b4106840', 'r2dbc-flag-day-v1', 'uuidv7', 'utc-micros', 'canonical-v1')
ON CONFLICT (fingerprint_version) DO NOTHING;

-- The compose default role owns the database; deployments with a dedicated
-- runtime role can grant these tables explicitly during provisioning.

-- Durable point-value ingest receipt. A row is created before TSDB/latest writes;
-- CLAIMED leases make crash recovery deterministic and tenant-scoped message IDs
-- provide the idempotency boundary across Data Center replicas.
CREATE TABLE IF NOT EXISTS dc3_point_value_ingest_outbox
(
    tenant_id      BIGINT      NOT NULL,
    message_id     TEXT        NOT NULL,
    schema_version INTEGER     NOT NULL,
    driver_node    TEXT        NOT NULL,
    sequence       BIGINT      NOT NULL,
    fencing_token  BIGINT      NOT NULL,
    device_id      BIGINT      NOT NULL,
    point_id       BIGINT      NOT NULL,
    raw_value      TEXT        NOT NULL,
    cal_value      TEXT        NOT NULL,
    num_value      DOUBLE PRECISION,
    driver_id      BIGINT      NOT NULL,
    create_time    TIMESTAMPTZ NOT NULL,
    operate_time   TIMESTAMPTZ NOT NULL,
    status         TEXT        NOT NULL DEFAULT 'PENDING',
    attempts       INTEGER     NOT NULL DEFAULT 0,
    available_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    claimed_at     TIMESTAMPTZ,
    claimed_by     TEXT,
    processed_at   TIMESTAMPTZ,
    last_error     TEXT,
    PRIMARY KEY (tenant_id, message_id),
    CONSTRAINT chk_point_value_outbox_status CHECK (status IN ('PENDING', 'CLAIMED', 'PERSISTED', 'PROCESSED', 'FAILED')),
    CONSTRAINT chk_point_value_outbox_attempts CHECK (attempts >= 0)
);
CREATE INDEX IF NOT EXISTS idx_point_value_outbox_claim
    ON dc3_point_value_ingest_outbox (status, available_at, claimed_at);
CREATE INDEX IF NOT EXISTS idx_point_value_outbox_series
    ON dc3_point_value_ingest_outbox (tenant_id, device_id, point_id, create_time DESC);
COMMENT ON TABLE dc3_point_value_ingest_outbox IS 'Durable point-value ingest receipt and replay state';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.tenant_id IS 'Owning tenant ID';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.message_id IS 'Source message idempotency key';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.schema_version IS 'Payload schema version';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.driver_node IS 'Source driver node';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.sequence IS 'Source sequence number';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.fencing_token IS 'Lease fencing token';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.device_id IS 'Device ID';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.point_id IS 'Point ID';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.raw_value IS 'Raw value';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.cal_value IS 'Calibrated value';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.num_value IS 'Numeric value';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.driver_id IS 'Driver ID';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.create_time IS 'Source creation time in UTC';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.operate_time IS 'Source operation time in UTC';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.status IS 'Ingest lifecycle status';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.attempts IS 'Claim attempt count';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.available_at IS 'Earliest replay time';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.claimed_at IS 'Current claim timestamp';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.claimed_by IS 'Current claim owner';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.processed_at IS 'Alert processing completion time';
COMMENT ON COLUMN dc3_point_value_ingest_outbox.last_error IS 'Last processing error';
