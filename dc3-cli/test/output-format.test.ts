import { describe, expect, it } from 'vitest';
import { load as yamlLoad } from 'js-yaml';
import { formatOutput, detectFormat } from '../src/utils/format.js';

/** OffsetPage-style list envelope the gateway returns for paged lists. */
const offsetPage = {
  ok: true,
  total: 42,
  offset: 4,
  limit: 3,
  items: [
    { id: 1, name: 'alpha' },
    { id: 2, name: 'beta' },
    { id: 3, name: 'gamma' },
  ],
  hasNext: true,
};

/** {ok,total,data} envelope variant used by other gateway endpoints. */
const dataEnvelope = {
  ok: true,
  total: 2,
  data: [
    { id: 'a', label: 'one' },
    { id: 'b', label: 'two' },
  ],
};

describe('table rendering of list envelopes (F020)', () => {
  it('renders envelope rows as one line per record, not a single-line JSON blob', () => {
    const table = formatOutput(offsetPage, 'table');

    expect(table).not.toContain('[{"id":');
    expect(table).not.toContain('[object Object]');
    for (const row of offsetPage.items) {
      expect(table).toContain(String(row.id));
      expect(table).toContain(row.name);
    }
    // Each record occupies its own line: header, separator, 3 rows, footer.
    const rows = table.split('\n');
    expect(rows).toHaveLength(6);
  });

  it('appends a pagination footer for offset pages', () => {
    const table = formatOutput(offsetPage, 'table');

    expect(table).toContain('-- 3 of 42 (offset 4, limit 3), hasNext: true');
  });

  it('renders the data-array envelope variant as rows', () => {
    const table = formatOutput(dataEnvelope, 'table');

    expect(table.split('\n')).toHaveLength(5); // header + separator + 2 rows + footer
    expect(table).toContain('one');
    expect(table).toContain('two');
  });

  it('keeps the union of keys across heterogeneous rows (first-seen order)', () => {
    const table = formatOutput([{ a: 1 }, { b: 2 }, { a: 3, b: 4 }], 'table');

    const lines = table.split('\n');
    expect(lines[0].indexOf('a')).toBeLessThan(lines[0].indexOf('b'));
    // The last column is never padded: rows end at their content (no
    // trailing whitespace), including rows with an empty last cell.
    expect(lines[2]).toBe('1 |');
    expect(lines[3]).toBe('  | 2');
    expect(lines[4]).toBe('3 | 4');
  });

  it('cellifies nested objects as compact JSON and null explicitly', () => {
    const table = formatOutput([{ meta: { a: 1 }, tags: ['x', 'y'], note: null }], 'table');

    expect(table).toContain('"a":1');
    expect(table).toContain('["x","y"]');
    expect(table).toContain('null');
    expect(table).not.toContain('[object Object]');
  });

  it('renders scalar arrays as a value column', () => {
    const table = formatOutput([1, 2], 'table');

    expect(table.split('\n')[0].trim()).toBe('value');
    expect(table).toContain('1');
    expect(table).toContain('2');
  });

  it('keeps the single-record key/value view for non-envelope records', () => {
    const table = formatOutput({ gateway: 'http://localhost:8000', tenant: 'default' }, 'table');

    expect(table).toContain('gateway');
    expect(table).toContain('http://localhost:8000');
  });

  it('does not treat records with nested siblings as list envelopes', () => {
    const table = formatOutput({ id: 7, items: ['a', 'b'], detail: { nested: true } }, 'table');

    // Nested object sibling disqualifies the envelope: flattened key/value view.
    expect(table).toContain('detail.nested');
    expect(table).toContain('true');
  });

  it('empty arrays stay empty markers', () => {
    expect(formatOutput([], 'table')).toBe('(empty)');
    expect(formatOutput({ items: [], total: 0 }, 'table')).toContain('(empty)');
  });
});

describe('yaml rendering (F020, F041)', () => {
  it('dumps envelope rows as a real YAML list that round-trips', () => {
    const yaml = formatOutput(offsetPage, 'yaml');
    const parsed = yamlLoad(yaml) as typeof offsetPage;

    expect(parsed.items).toHaveLength(3);
    expect(parsed.total).toBe(42);
    expect(yaml).toContain('- id: 1');
    expect(yaml).toContain('name: alpha');
  });

  it('round-trips keys containing YAML indicators (F041)', () => {
    const payload = { 'conn: status': 'ok', '#hash': 1, '- lead': true };
    const yaml = formatOutput(payload, 'yaml');

    expect(yamlLoad(yaml)).toEqual(payload);
  });

  it('round-trips nested values and arrays', () => {
    const payload = { map: { 'a: b': { c: 1 } }, list: [1, 'two', null] };
    const yaml = formatOutput(payload, 'yaml');

    expect(yamlLoad(yaml)).toEqual(payload);
  });

  it('emits one list entry per record', () => {
    const yaml = formatOutput(dataEnvelope, 'yaml');

    const entries = yaml.split('\n').filter((line) => line.trimStart().startsWith('- '));
    expect(entries).toHaveLength(2);
  });

  it('undefined payloads stay silent', () => {
    expect(formatOutput(undefined, 'yaml')).toBe('');
  });
});

describe('json output is byte-identical (agent wire contract)', () => {
  it('renders envelopes exactly as JSON.stringify(data, null, 2)', () => {
    for (const payload of [offsetPage, dataEnvelope, [{ id: 1 }], { ok: true }, 'plain', 42, null]) {
      expect(formatOutput(payload, 'json')).toBe(JSON.stringify(payload, null, 2));
    }
  });

  it('snapshot-locks the envelope JSON shape', () => {
    expect(formatOutput(offsetPage, 'json')).toMatchInlineSnapshot(`
      "{
        "ok": true,
        "total": 42,
        "offset": 4,
        "limit": 3,
        "items": [
          {
            "id": 1,
            "name": "alpha"
          },
          {
            "id": 2,
            "name": "beta"
          },
          {
            "id": 3,
            "name": "gamma"
          }
        ],
        "hasNext": true
      }"
    `);
  });
});

describe('detectFormat guards (F052)', () => {
  it('an explicit empty format is a usage error, not a silent default', () => {
    expect(() => detectFormat('')).toThrow(/unknown format/);
  });

  it('explicit unknown formats are usage errors', () => {
    expect(() => detectFormat('xml')).toThrow(/unknown format 'xml'/);
  });

  it('supported formats pass through', () => {
    expect(detectFormat('json')).toBe('json');
    expect(detectFormat('table')).toBe('table');
    expect(detectFormat('yaml')).toBe('yaml');
  });
});
