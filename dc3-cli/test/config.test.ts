import { describe, it, expect } from 'vitest';
import { ConfigSchema, ProfileConfigSchema, AppSettingsSchema } from '../src/core/config-manager';

describe('config schemas', () => {
  it('ProfileConfigSchema should accept minimal config', () => {
    const result = ProfileConfigSchema.safeParse({
      username: 'test',
    });
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.gateway).toBe('http://localhost:8000');
      expect(result.data.tenant).toBe('default');
      expect(result.data.credential_store).toBe('keychain');
    }
  });

  it('ProfileConfigSchema should reject empty username', () => {
    const result = ProfileConfigSchema.safeParse({ username: '' });
    expect(result.success).toBe(false);
  });

  it('ProfileConfigSchema should accept full config', () => {
    const result = ProfileConfigSchema.safeParse({
      gateway: 'https://example.com',
      tenant: 'production',
      username: 'admin',
      credential_store: 'env',
    });
    expect(result.success).toBe(true);
  });

  it('ProfileConfigSchema should reject invalid credential_store', () => {
    const result = ProfileConfigSchema.safeParse({
      username: 'test',
      credential_store: 'plaintext',
    });
    expect(result.success).toBe(false);
  });

  it('AppSettingsSchema should use defaults', () => {
    const result = AppSettingsSchema.safeParse({});
    expect(result.success).toBe(true);
    if (result.success) {
      expect(result.data.output_format).toBe('table');
      expect(result.data.color).toBe(true);
      expect(result.data.renewal_threshold_hours).toBe(1);
    }
  });

  it('ConfigSchema should accept minimal config', () => {
    const result = ConfigSchema.safeParse({
      version: 1,
      profiles: {
        default: { username: 'test' },
      },
    });
    expect(result.success).toBe(true);
  });

  it('ConfigSchema should reject invalid version', () => {
    const result = ConfigSchema.safeParse({
      version: 2,
      profiles: {},
    });
    expect(result.success).toBe(false);
  });
});
