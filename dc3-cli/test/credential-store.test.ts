import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { existsSync } from 'node:fs';
import { mkdir, mkdtemp, readFile, rm, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

// Redirect os.homedir() to a per-test temp dir; stores and the config manager
// bake their paths at module load, hence resetModules + dynamic imports.
const osStub = vi.hoisted(() => ({ home: '' }));

vi.mock('node:os', async (importOriginal) => {
  const actual = await importOriginal<typeof import('node:os')>();
  return {
    ...actual,
    homedir: () => (osStub.home ? osStub.home : actual.homedir()),
  };
});

// Controllable keychain double: availability and contents are per-test state.
const keychainMock = vi.hoisted(() => ({
  available: false,
  password: null as string | null,
  failGet: false,
  saved: [] as Array<{ identifier: string; password: string }>,
  deleted: [] as string[],
}));

vi.mock('../src/core/credential-keychain.js', () => ({
  KeychainStore: class {
    readonly name = 'keychain';
    async isAvailable(): Promise<boolean> {
      return keychainMock.available;
    }
    async getPassword(_identifier: string): Promise<string | null> {
      if (keychainMock.failGet) {
        throw new Error('keychain transport error');
      }
      return keychainMock.password;
    }
    async savePassword(identifier: string, password: string): Promise<void> {
      keychainMock.saved.push({ identifier, password });
    }
    async deletePassword(identifier: string): Promise<void> {
      keychainMock.deleted.push(identifier);
    }
  },
}));

describe('credential store chain (F033) and save contract (F005)', () => {
  let home: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-chain-'));
    osStub.home = home;
    keychainMock.available = false;
    keychainMock.password = null;
    keychainMock.failGet = false;
    keychainMock.saved = [];
    keychainMock.deleted = [];
    delete process.env.DC3_PASSWORD;
    vi.resetModules();
    vi.spyOn(console, 'error').mockImplementation(() => undefined);
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    vi.unstubAllEnvs();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  async function writeConfigFile(credentialStore: string): Promise<void> {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: {
            gateway: 'http://localhost:8000',
            tenant: 'default',
            username: 'admin',
            credential_store: credentialStore,
          },
        },
      }),
      'utf8',
    );
  }

  it('the configured store is consulted first', async () => {
    await writeConfigFile('encrypted');
    const store = await import('../src/core/credential-encrypted');
    const encrypted = new store.EncryptedFileStore();
    await encrypted.savePassword('admin@default', 'from-encrypted');
    vi.stubEnv('DC3_PASSWORD', 'from-env');

    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBe('from-encrypted');
  });

  it('an unavailable configured store falls through keychain → encrypted → env', async () => {
    await writeConfigFile('keychain'); // unavailable in this fixture
    vi.stubEnv('DC3_PASSWORD', 'from-env');

    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBe('from-env');
  });

  it('the encrypted store is tried before env when it holds the entry', async () => {
    await writeConfigFile('prompt'); // inert configured store
    const store = await import('../src/core/credential-encrypted');
    await new store.EncryptedFileStore().savePassword('admin@default', 'from-encrypted');
    vi.stubEnv('DC3_PASSWORD', 'from-env');

    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBe('from-encrypted');
  });

  it('env is only consulted while DC3_PASSWORD is set', async () => {
    await writeConfigFile('keychain');
    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBeNull();
  });

  it('a store that throws is skipped without failing the chain', async () => {
    await writeConfigFile('keychain');
    keychainMock.available = true;
    keychainMock.failGet = true;
    vi.stubEnv('DC3_PASSWORD', 'from-env');

    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBe('from-env');
  });

  it('keychain entries win over encrypted and env (priority order)', async () => {
    await writeConfigFile('encrypted'); // configured encrypted holds nothing here
    keychainMock.available = true;
    keychainMock.password = 'from-keychain';
    vi.stubEnv('DC3_PASSWORD', 'from-env');

    const { resolvePassword } = await import('../src/core/credential-store');
    expect(await resolvePassword('admin@default')).toBe('from-keychain');
  });

  it('savePasswordToStore reports persisted:false for an unavailable store — never silent (F005)', async () => {
    await writeConfigFile('keychain'); // unavailable

    const { savePasswordToStore } = await import('../src/core/credential-store');
    const result = await savePasswordToStore('admin@default', 'pw');
    expect(result).toEqual({ persisted: false, store: 'keychain' });
  });

  it('savePasswordToStore persists and reports the target store', async () => {
    await writeConfigFile('encrypted');

    const { savePasswordToStore, resolvePassword } = await import('../src/core/credential-store');
    const result = await savePasswordToStore('admin@default', 'pw');
    expect(result).toEqual({ persisted: true, store: 'encrypted' });
    expect(await resolvePassword('admin@default')).toBe('pw');
  });

  it('savePasswordToStore honors an explicit profile name', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: { username: 'admin', credential_store: 'keychain' },
          other: { username: 'bob', credential_store: 'encrypted' },
        },
      }),
      'utf8',
    );

    const { savePasswordToStore } = await import('../src/core/credential-store');
    const result = await savePasswordToStore('bob@default', 'pw', 'other');
    expect(result).toEqual({ persisted: true, store: 'encrypted' });
  });

  it('deletePasswordFromStore is best-effort and never throws', async () => {
    await writeConfigFile('keychain');
    keychainMock.available = true;
    const { deletePasswordFromStore } = await import('../src/core/credential-store');
    await expect(deletePasswordFromStore('admin@default')).resolves.toBeUndefined();
    expect(keychainMock.deleted).toEqual(['admin@default']);
  });
});

describe('config reset clears every local auth store (F016)', () => {
  let home: string;
  let stderr: string;

  beforeEach(async () => {
    home = await mkdtemp(join(tmpdir(), 'dc3-cli-reset-'));
    osStub.home = home;
    keychainMock.available = false;
    keychainMock.saved = [];
    keychainMock.deleted = [];
    vi.resetModules();
    stderr = '';
    vi.spyOn(console, 'error').mockImplementation((...args: unknown[]) => {
      stderr += args.join(' ');
    });
  });

  afterEach(async () => {
    vi.restoreAllMocks();
    osStub.home = '';
    await rm(home, { recursive: true, force: true });
  });

  function fakeTokenState() {
    return {
      token: 'tok-reset-test',
      salt: 'salt',
      tenant: 'tenantA',
      username: 'admin',
      issuedAt: 1,
      expiresAt: 2_000_000_000,
    };
  }

  it('resetAllLocalState wipes config, tokens, and stored passwords in one call', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'config.json'),
      JSON.stringify({
        version: 1,
        current_profile: 'default',
        settings: {},
        profiles: {
          default: { username: 'admin', gateway: 'http://gw', tenant: 'tenantA' },
        },
      }),
      'utf8',
    );
    await writeFile(join(home, '.dc3', 'tokens.json'), JSON.stringify({ default: fakeTokenState() }), 'utf8');
    const encrypted = new (await import('../src/core/credential-encrypted')).EncryptedFileStore();
    await encrypted.savePassword('admin@tenantA', 'pw');
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(true);

    const { resetAllLocalState } = await import('../src/core/credential-store');
    await resetAllLocalState();

    expect(existsSync(join(home, '.dc3', 'tokens.json'))).toBe(false);
    expect(existsSync(join(home, '.dc3', 'credentials.enc'))).toBe(false);
    const config = JSON.parse(await readFile(join(home, '.dc3', 'config.json'), 'utf8'));
    expect(config.profiles).toEqual({});
    expect(config.current_profile).toBe('default');

    const { tokenManager } = await import('../src/core/token-manager');
    expect(await tokenManager.getAllStates()).toEqual({});
  });

  it('reset also scrubs identifiers whose token is already gone (encrypted enumeration)', async () => {
    const encrypted = new (await import('../src/core/credential-encrypted')).EncryptedFileStore();
    await encrypted.savePassword('ghost@tenantB', 'pw');

    const { resetAllLocalState } = await import('../src/core/credential-store');
    await resetAllLocalState();
    expect(await encrypted.getPassword('ghost@tenantB')).toBeNull();
  });

  it('a failing clearer warns loudly naming what survived; the config reset still lands', async () => {
    await mkdir(join(home, '.dc3'), { recursive: true });
    const { configManager } = await import('../src/core/config-manager');
    await configManager.setProfile('default', { username: 'admin' });

    await configManager.reset({
      clearTokens: () => {
        throw new Error('tokens locked');
      },
    });

    expect(stderr).toContain('token state');
    expect(stderr).toContain('tokens locked');
    const config = JSON.parse(await readFile(join(home, '.dc3', 'config.json'), 'utf8'));
    expect(config.profiles).toEqual({});
  });

  it('keychain entries are deleted through the reset path when the keychain is available', async () => {
    keychainMock.available = true;
    await mkdir(join(home, '.dc3'), { recursive: true });
    await writeFile(
      join(home, '.dc3', 'tokens.json'),
      JSON.stringify({ default: fakeTokenState() }),
      'utf8',
    );
    const { resetAllLocalState } = await import('../src/core/credential-store');
    await resetAllLocalState();
    expect(keychainMock.deleted).toContain('admin@tenantA');
  });
});
