#!/usr/bin/env node

/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {createReadStream, existsSync, readFileSync, statSync} from 'node:fs';
import {createServer, request as httpRequest} from 'node:http';
import {request as httpsRequest} from 'node:https';
import {extname, join, normalize, resolve, sep} from 'node:path';
import {pipeline} from 'node:stream';
import {fileURLToPath} from 'node:url';

const projectRoot = resolve(fileURLToPath(new URL('../../', import.meta.url)));
const distDir = resolve(projectRoot, 'dist');
const envDir = resolve(projectRoot, 'src/config/env');
const env = {
  ...readDotEnv(resolve(envDir, '.env')),
  ...readDotEnv(resolve(envDir, '.env.dev')),
  ...process.env,
};

const defaultBaseUrl = `http://localhost:${env.APP_CLI_PORT || '8080'}`;
const baseUrl = new URL(env.E2E_BASE_URL || defaultBaseUrl);
const host = env.E2E_HOST || '0.0.0.0';
const port = Number(env.E2E_PORT || baseUrl.port || env.APP_CLI_PORT || 8080);
const apiPrefix = normalizeApiPrefix(env.APP_API_PREFIX || '/api');
const apiTarget = new URL(
  env.E2E_API_TARGET || `${env.APP_API_PATH || 'http://localhost'}:${env.APP_API_PORT || '8000'}`
);

if (!existsSync(resolve(distDir, 'index.html'))) {
  console.error(`dist/index.html was not found. Run "pnpm build" before starting the E2E server.`);
  process.exit(1);
}

const server = createServer((req, res) => {
  if (!req.url) {
    res.writeHead(400).end('Bad Request');
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host || 'localhost'}`);
  if (isApiRequest(url.pathname)) {
    proxyRequest(req, res);
    return;
  }

  serveStatic(req, res, url.pathname);
});

server.on('clientError', (_error, socket) => {
  socket.end('HTTP/1.1 400 Bad Request\r\n\r\n');
});

server.listen(port, host, () => {
  console.log(`E2E server listening on http://${host}:${port}`);
  console.log(`Serving ${distDir}`);
  console.log(`Proxying ${apiPrefix} -> ${apiTarget.origin}`);
});

function readDotEnv(file) {
  if (!existsSync(file)) return {};

  return Object.fromEntries(
    readFileSync(file, 'utf8')
      .split(/\r?\n/)
      .map((line) => line.trim())
      .filter((line) => line && !line.startsWith('#'))
      .map((line) => {
        const index = line.indexOf('=');
        if (index < 0) return undefined;

        const key = line.slice(0, index).trim();
        let value = line.slice(index + 1).trim();
        if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
          value = value.slice(1, -1);
        }
        return [key, value];
      })
      .filter(Boolean)
  );
}

function normalizeApiPrefix(prefix) {
  const normalized = prefix.startsWith('/') ? prefix : `/${prefix}`;
  return normalized.endsWith('/') && normalized.length > 1 ? normalized.slice(0, -1) : normalized;
}

function isApiRequest(pathname) {
  return pathname === apiPrefix || pathname.startsWith(`${apiPrefix}/`);
}

function proxyRequest(req, res) {
  const target = new URL(req.url, apiTarget);
  const transport = target.protocol === 'https:' ? httpsRequest : httpRequest;
  const headers = {
    ...req.headers,
    host: target.host,
    'x-forwarded-host': req.headers.host || '',
    'x-forwarded-proto': 'http',
  };

  const proxy = transport(
    {
      protocol: target.protocol,
      hostname: target.hostname,
      port: target.port,
      path: `${target.pathname}${target.search}`,
      method: req.method,
      headers,
    },
    (proxyRes) => {
      res.writeHead(proxyRes.statusCode || 502, proxyRes.headers);
      pipeline(proxyRes, res, (error) => {
        if (error && !res.destroyed) {
          res.destroy(error);
        }
      });
    }
  );

  proxy.on('error', (error) => {
    if (res.headersSent) {
      res.destroy(error);
      return;
    }

    res.writeHead(502, {'content-type': 'application/json; charset=utf-8'});
    res.end(
      JSON.stringify({
        ok: false,
        code: 502,
        message: `E2E proxy failed to reach ${apiTarget.origin}`,
      })
    );
  });

  pipeline(req, proxy, (error) => {
    if (error) {
      proxy.destroy(error);
    }
  });
}

function serveStatic(req, res, pathname) {
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    res.writeHead(405, {allow: 'GET, HEAD'}).end('Method Not Allowed');
    return;
  }

  const file = resolveStaticPath(pathname);
  if (!file) {
    res.writeHead(403).end('Forbidden');
    return;
  }

  const {size} = statSync(file);
  res.writeHead(200, {
    'content-length': size,
    'content-type': contentType(file),
  });

  if (req.method === 'HEAD') {
    res.end();
    return;
  }

  pipeline(createReadStream(file), res, (error) => {
    if (error && !res.destroyed) {
      res.destroy(error);
    }
  });
}

function resolveStaticPath(pathname) {
  let decodedPath;
  try {
    decodedPath = decodeURIComponent(pathname);
  } catch {
    decodedPath = '/';
  }

  const normalized = normalize(decodedPath).replace(/^(\.\.[/\\])+/, '');
  const relative = normalized === sep ? 'index.html' : normalized.replace(/^[/\\]+/, '');
  let file = resolve(distDir, relative);

  if (!isInsideDist(file)) return undefined;

  if (existsSync(file) && statSync(file).isDirectory()) {
    file = join(file, 'index.html');
  }

  if (existsSync(file) && statSync(file).isFile()) {
    return file;
  }

  return resolve(distDir, 'index.html');
}

function isInsideDist(file) {
  return file === distDir || file.startsWith(`${distDir}${sep}`);
}

function contentType(file) {
  const types = {
    '.css': 'text/css; charset=utf-8',
    '.gif': 'image/gif',
    '.html': 'text/html; charset=utf-8',
    '.ico': 'image/x-icon',
    '.jpeg': 'image/jpeg',
    '.jpg': 'image/jpeg',
    '.js': 'text/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.map': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.svg': 'image/svg+xml',
    '.txt': 'text/plain; charset=utf-8',
    '.webp': 'image/webp',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
  };
  return types[extname(file).toLowerCase()] || 'application/octet-stream';
}
