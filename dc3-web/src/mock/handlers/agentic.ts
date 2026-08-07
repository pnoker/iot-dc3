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
  on('post', 'api/v3/agentic/session/list', (ctx) =>
    responseOf(ctx.config, ok(paginate(ctx.db.agenticSessions, ctx.body))),
  );
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
    if (i >= 0) coll[i] = {...coll[i], ...ctx.body};
    return responseOf(ctx.config, ok(true));
  });
  on('get', 'api/v3/agentic/message/list', (ctx) => {
    const id = ctx.params.conversation_id;
    const rows = ctx.db.agenticMessages.filter((m) => String(m.conversationId) === String(id));
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
  on('get', 'api/v3/agentic/action/pending', (ctx) => responseOf(ctx.config, ok([])));
  on('post', 'api/v3/agentic/action/confirm', (ctx) => responseOf(ctx.config, ok(true)));
  on('post', 'api/v3/agentic/action/reject', (ctx) => responseOf(ctx.config, ok(true)));

  // ── agentic: attachments ──
  on('post', 'api/v3/agentic/attachment/upload', (ctx) =>
    responseOf(ctx.config, ok({id: newId(), ...ctx.body, createTime: stamp()})),
  );
  on('get', 'api/v3/agentic/attachment/list', (ctx) => responseOf(ctx.config, ok([])));

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
