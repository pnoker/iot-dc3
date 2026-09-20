-- Copyright 2016-present the IoT DC3 original author or authors.
--
-- Menu icon semantic re-mapping backfill (settings domain, 12 codes).
--
-- WHY: initdb seeds only run on FIRST container start (Docker entrypoint
-- /docker-entrypoint-initdb.d). Databases initialized before the icon
-- re-mapping keep the old icons until this backfill runs. The canonical
-- mapping lives in initdb/02-iot-dc3-auth.sql and must stay identical to
-- dc3-web/src/config/settingsNav.ts and dc3-web/src/mock/seed/menuTree.ts
-- (enforced by tests/guardrails/settings-menu-consistency.test.ts).
--
-- IDEMPOTENT: safe to re-run — the UPDATE converges to the same values.
-- Scope: only the 12 re-mapped menu codes; operator-managed icons on other
-- menus are untouched (this file intentionally overrides manual edits on
-- these 12 codes to keep the repo mapping authoritative).
--
-- Run against an already-initialized database:
--   docker exec -i <postgres-container> psql -U dc3 -d dc3 \
--     -v ON_ERROR_STOP=1 < 01-menu-icon-backfill.sql
--
-- Verify afterwards:
--   SELECT menu_code, ((menu_ext ->> 'content')::jsonb ->> 'icon') AS icon
--     FROM dc3_menu WHERE menu_code LIKE 'settings%' AND deleted = 0
--    ORDER BY id;

BEGIN;

-- Same schema the seed installs into (initdb/02-iot-dc3-auth.sql).
SET search_path TO dc3_auth;

-- settingsIdentity: User -> UserFilled (split parent/child duplicates)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('UserFilled'::text))::text)
    )
WHERE menu_code = 'settingsIdentity' AND deleted = 0;

-- settingsRole: Lock -> Medal (Lock belongs to the credential domain)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Medal'::text))::text)
    )
WHERE menu_code = 'settingsRole' AND deleted = 0;

-- settingsRolePrincipalBind: Link -> Paperclip (assignment = attach)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Paperclip'::text))::text)
    )
WHERE menu_code = 'settingsRolePrincipalBind' AND deleted = 0;

-- settingsResource: Tickets -> Box (resource container, not a ticket)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Box'::text))::text)
    )
WHERE menu_code = 'settingsResource' AND deleted = 0;

-- settingsMenu: Discount -> Menu (a discount tag is not a menu)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Menu'::text))::text)
    )
WHERE menu_code = 'settingsMenu' AND deleted = 0;

-- settingsModelConfig: ChatDotRound -> MagicStick (AI tuning, not a chat bubble)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('MagicStick'::text))::text)
    )
WHERE menu_code = 'settingsModelConfig' AND deleted = 0;

-- settingsModelProvider: ChatLineSquare -> Shop (marketplace = provider)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Shop'::text))::text)
    )
WHERE menu_code = 'settingsModelProvider' AND deleted = 0;

-- settingsAlarmChannel: Connection -> Postcard (delivery channel, dedupe with API)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Postcard'::text))::text)
    )
WHERE menu_code = 'settingsAlarmChannel' AND deleted = 0;

-- settingsMcpServer: Connection -> Platform (server host, dedupe in MCP group)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Platform'::text))::text)
    )
WHERE menu_code = 'settingsMcpServer' AND deleted = 0;

-- settingsEventHistory: Document -> Lightning (split sibling duplication)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Lightning'::text))::text)
    )
WHERE menu_code = 'settingsEventHistory' AND deleted = 0;

-- settingsCommandHistory: Document -> Position (sent command)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Position'::text))::text)
    )
WHERE menu_code = 'settingsCommandHistory' AND deleted = 0;

-- settingsIdentityAudit: DocumentChecked -> Memo (audit log, dedupe with alarm history)
UPDATE dc3_menu
SET menu_ext = jsonb_set(
      menu_ext, '{content}',
      to_jsonb(jsonb_set((menu_ext ->> 'content')::jsonb, '{icon}', to_jsonb('Memo'::text))::text)
    )
WHERE menu_code = 'settingsIdentityAudit' AND deleted = 0;

COMMIT;
