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

import {on} from '../dispatch';
import {paginate} from '../query';
import {ok, responseOf} from '../response';
import {newId, stamp} from '../crud';
import {currentMockLocale} from '../locale';

type LocalizedRow = Record<string, unknown> & {
  titleI18n?: Record<'zh' | 'en', string>;
  summaryI18n?: Record<'zh' | 'en', string>;
  descriptionI18n?: Record<'zh' | 'en', string>;
  contentI18n?: Record<'zh' | 'en', string>;
  contentExtI18n?: Record<'zh' | 'en', unknown>;
};

const localizeAgenticRow = (row: Record<string, unknown>): Record<string, unknown> => {
  const locale = currentMockLocale();
  const source = row as LocalizedRow;
  const localized: Record<string, unknown> = {
    ...row,
    ...(source.titleI18n ? {title: source.titleI18n[locale]} : {}),
    ...(source.summaryI18n ? {summary: source.summaryI18n[locale]} : {}),
    ...(source.descriptionI18n ? {description: source.descriptionI18n[locale]} : {}),
    ...(source.contentI18n ? {content: source.contentI18n[locale]} : {}),
    ...(source.contentExtI18n ? {contentExt: source.contentExtI18n[locale]} : {}),
  };
  delete localized.titleI18n;
  delete localized.summaryI18n;
  delete localized.descriptionI18n;
  delete localized.contentI18n;
  delete localized.contentExtI18n;
  return localized;
};

/** add/update/delete for the mutable agentic config collections. */
const cud = (url: string, key: 'agenticModelConfigs' | 'agenticProviders') => {
  on('post', `${url}/add`, (ctx) => {
    const row: Record<string, unknown> = {...ctx.body, id: newId(), createTime: stamp(), operateTime: stamp()};
    ctx.db[key].push(row);
    return responseOf(ctx.config, ok(String(row.id)));
  });
  on('post', `${url}/update`, (ctx) => {
    const coll = ctx.db[key];
    const i = coll.findIndex((r) => String(r.id) === String(ctx.body?.id));
    if (i >= 0) coll[i] = {...coll[i], ...ctx.body, operateTime: stamp()};
    return responseOf(ctx.config, ok(String(ctx.body?.id ?? '')));
  });
  on('post', `${url}/delete`, (ctx) => {
    const coll = ctx.db[key];
    const i = coll.findIndex((r) => String(r.id) === String(ctx.params.id));
    if (i >= 0) coll.splice(i, 1);
    return responseOf(ctx.config, ok(String(ctx.params.id)));
  });
};

export function registerAgenticHandlers(): void {
  // ── agentic: model catalog & config ──
  on('get', 'api/v3/agentic/model/list', (ctx) => {
    const catalog = ctx.db.agenticModelConfigs.map((m) => ({
      model: m.model,
      label: m.label,
      stream: m.stream,
      toolCall: m.toolCall,
      vision: m.vision,
      reasoning: m.reasoning,
    }));
    return responseOf(ctx.config, ok(catalog));
  });
  on('get', 'api/v3/agentic/model/config/list', (ctx) =>
    responseOf(ctx.config, ok(ctx.db.agenticModelConfigs)),
  );
  cud('api/v3/agentic/model/config', 'agenticModelConfigs');

  // ── agentic: provider config ──
  on('get', 'api/v3/agentic/provider/list', (ctx) =>
    responseOf(ctx.config, ok(ctx.db.agenticProviders)),
  );
  cud('api/v3/agentic/provider/config', 'agenticProviders');

  // ── agentic: sessions & messages ──
  on('post', 'api/v3/agentic/session/list', (ctx) => {
    const rows = ctx.db.agenticSessions.map(localizeAgenticRow);
    return responseOf(ctx.config, ok(paginate(rows, ctx.body)));
  });
  on('post', 'api/v3/agentic/session/delete', (ctx) => {
    const id = ctx.body?.conversationId;
    const coll = ctx.db.agenticSessions;
    const i = coll.findIndex((r) => String(r.conversationId) === String(id));
    if (i >= 0) coll.splice(i, 1);
    return responseOf(ctx.config, ok(true));
  });
  on('post', 'api/v3/agentic/session/update', (ctx) => {
    const coll = ctx.db.agenticSessions;
    const i = coll.findIndex((r) => String(r.conversationId) === String(ctx.body?.conversationId));
    if (i >= 0) {
      const current = coll[i]!;
      coll[i] = {
        ...current,
        ...ctx.body,
        sessionExt: ctx.body?.sessionExt
          ? {...(current.sessionExt as Record<string, unknown> | undefined), ...ctx.body.sessionExt}
          : current.sessionExt,
        operateTime: stamp(),
      };
      return responseOf(ctx.config, ok(localizeAgenticRow(coll[i]!)));
    }
    const created = {
      ...ctx.body,
      title: ctx.body?.title || (currentMockLocale() === 'zh' ? '新对话' : 'New conversation'),
      createTime: stamp(),
      operateTime: stamp(),
    };
    coll.unshift(created);
    return responseOf(ctx.config, ok(created));
  });
  on('get', 'api/v3/agentic/message/list', (ctx) => {
    const id = ctx.params.conversation_id;
    const rows = ctx.db.agenticMessages
      .filter((m) => String(m.conversationId) === String(id))
      .sort((a, b) => Number(a.messageIndex ?? 0) - Number(b.messageIndex ?? 0))
      .map(localizeAgenticRow);
    return responseOf(ctx.config, ok(rows));
  });

  // ── agentic: chat & actions ──
  on('post', 'api/v3/agentic/chat/completions', (ctx) =>
    responseOf(
      ctx.config,
      ok({
        conversationId: ctx.body?.conversationId,
        message: {
          role: 'assistant',
          content: '这是一个 mock 演示回复。在真实环境中将调用配置的 AI 模型。',
          status: 2,
          finishReason: 'stop',
        },
        createTime: stamp(),
      }),
    ),
  );
  on('get', 'api/v3/agentic/action/pending', (ctx) => {
    const rows = ctx.db.agenticActions
      .filter((action) =>
        String(action.conversationId) === String(ctx.params.conversation_id) && Number(action.status) === 0)
      .map(localizeAgenticRow);
    return responseOf(ctx.config, ok(rows));
  });
  on('post', 'api/v3/agentic/action/confirm', (ctx) => {
    const action = ctx.db.agenticActions.find((item) => String(item.actionId) === String(ctx.params.action_id));
    if (action) action.status = 1;
    return responseOf(ctx.config, ok(action ? localizeAgenticRow(action) : undefined));
  });
  on('post', 'api/v3/agentic/action/reject', (ctx) => {
    const action = ctx.db.agenticActions.find((item) => String(item.actionId) === String(ctx.params.action_id));
    if (action) action.status = 2;
    return responseOf(ctx.config, ok(action ? localizeAgenticRow(action) : undefined));
  });

  // ── agentic: attachments ──
  on('post', 'api/v3/agentic/attachment/upload', (ctx) =>
    responseOf(ctx.config, ok({id: newId(), ...ctx.body, createTime: stamp()})),
  );
  on('get', 'api/v3/agentic/attachment/list', (ctx) => {
    const rows = ctx.db.agenticAttachments.filter(
      (attachment) => String(attachment.conversationId) === String(ctx.params.conversation_id),
    );
    return responseOf(ctx.config, ok(rows));
  });

  // ── MCP: OAuth metadata & clients ──
  on('get', 'api/v3/auth/mcp/metadata', (ctx) =>
    responseOf(
      ctx.config,
      ok({
        issuer: 'https://demo.dc3.site',
        authorizationEndpoint: '/oauth/authorize',
        tokenEndpoint: '/oauth/token',
        registrationEndpoint: '/oauth/register',
        scopesSupported: ['mcp:tools:list', 'mcp:tools:call'],
      }),
    ),
  );
  on('post', 'api/v3/auth/mcp/client/register', (ctx) => {
    const row: Record<string, unknown> = {id: newId(), ...ctx.body, enableFlag: 'ENABLE'};
    ctx.db.mcpClients.push(row);
    return responseOf(ctx.config, ok(String(row.id)));
  });
  on('post', 'api/v3/auth/mcp/client/list', (ctx) =>
    responseOf(ctx.config, ok(paginate(ctx.db.mcpClients, ctx.body))),
  );

  // ── MCP: connections ──
  on('post', 'api/v3/auth/mcp/connection/list', (ctx) =>
    responseOf(ctx.config, ok(paginate(ctx.db.mcpConnections, ctx.body))),
  );
  on('post', 'api/v3/auth/mcp/connection/add', (ctx) => {
    const row: Record<string, unknown> = {...ctx.body, id: newId()};
    ctx.db.mcpConnections.push(row);
    return responseOf(ctx.config, ok(String(row.id)));
  });
  on('post', 'api/v3/auth/mcp/connection/revoke', (ctx) => {
    const row = ctx.db.mcpConnections.find((r) => String(r.id) === String(ctx.params.id));
    if (row) {
      row.revokeTime = stamp();
      row.enableFlag = 'DISABLE';
    }
    return responseOf(ctx.config, ok(true));
  });
  on('post', 'api/v3/auth/mcp/connection/tools/replace', (ctx) => responseOf(ctx.config, ok(true)));
  on('get', 'api/v3/auth/mcp/connection/tools/list', (ctx) =>
    responseOf(ctx.config, ok(ctx.db.mcpTools)),
  );

  // ── MCP: tool catalog & audit ──
  on('post', 'api/v3/auth/mcp/tool/catalog/refresh', (ctx) => responseOf(ctx.config, ok(true)));
  on('post', 'api/v3/auth/mcp/tool/list', (ctx) =>
    responseOf(ctx.config, ok(paginate(ctx.db.mcpTools, ctx.body))),
  );
  on('post', 'api/v3/auth/mcp/audit/list', (ctx) =>
    responseOf(ctx.config, ok(paginate(ctx.db.mcpAudits, ctx.body))),
  );
}
