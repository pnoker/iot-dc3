import { describe, it, expect } from 'vitest';
import { formatOutput, detectFormat } from '../src/utils/format';

describe('format utils', () => {
  it('formatOutput json should stringify', () => {
    const result = formatOutput({ ok: true, data: 'test' }, 'json');
    const parsed = JSON.parse(result);
    expect(parsed.ok).toBe(true);
    expect(parsed.data).toBe('test');
  });

  it('formatOutput json should handle arrays', () => {
    const result = formatOutput([{ id: 1, name: 'a' }], 'json');
    const parsed = JSON.parse(result);
    expect(parsed).toHaveLength(1);
    expect(parsed[0].id).toBe(1);
  });

  it('formatOutput table should handle arrays', () => {
    const result = formatOutput(
      [{ id: 1, name: 'Device A' }, { id: 2, name: 'Device B' }],
      'table',
    );
    expect(result).toContain('Device A');
    expect(result).toContain('Device B');
    expect(result).toContain('id');
    expect(result).toContain('name');
  });

  it('formatOutput table should handle empty array', () => {
    const result = formatOutput([], 'table');
    expect(result).toBe('(empty)');
  });

  it('formatOutput table should handle flat object', () => {
    const result = formatOutput({ gateway: 'http://localhost:8000', tenant: 'default' }, 'table');
    expect(result).toContain('gateway');
    expect(result).toContain('default');
  });
});
