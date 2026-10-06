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
/**
 * Atomic filesystem primitives shared by every persisted-state writer
 * (config.json, tokens.json, credentials.enc, credentials.key).
 *
 * Concurrency contract: state files are mutated by independent CLI processes
 * doing read-modify-write cycles. `writeFileAtomic` removes torn writes
 * (readers see either the old or the new file, never a half-written one);
 * `withLock` serializes the read-modify-write sections so concurrent writers
 * cannot silently drop each other's updates.
 */
import { execFile } from 'node:child_process';
import { chmod, mkdir, open, readFile, readdir, rename, rm, stat } from 'node:fs/promises';
import { randomBytes } from 'node:crypto';
import { userInfo } from 'node:os';
import { basename, dirname, join } from 'node:path';
import { promisify } from 'node:util';

import { TimeoutError } from './errors.js';

const execFileAsync = promisify(execFile);

/** A lockfile older than this (holder crashed or hung) is considered stale. */
const LOCK_STALE_AGE_MS = 30_000;
/** Give up acquiring a lock after this long instead of hanging forever. */
const LOCK_ACQUIRE_TIMEOUT_MS = 10_000;
/** Poll interval while waiting for a lock held by another process. */
const LOCK_POLL_INTERVAL_MS = 25;
/** Quarantined copies kept per state file before the oldest are pruned. */
const QUARANTINE_KEEP = 5;

/** Options controlling the final file's permissions in {@link writeFileAtomic}. */
export interface AtomicWriteOptions {
  /** POSIX file mode applied to the final file (default 0o644; ignored on win32). */
  mode?: number;
  /**
   * Restrict the file to the current user: POSIX chmod 0600, win32 explicit ACL
   * (icacls /inheritance:r /grant <current user>:F) because mode bits cannot
   * express per-principal restrictions there.
   */
  restrictToOwner?: boolean;
}

/**
 * Restrict an existing file to the current user only.
 * @param path - file to restrict
 * @param posixMode - mode applied verbatim on POSIX platforms
 */
export async function restrictToFileOwner(path: string, posixMode = 0o600): Promise<void> {
  if (process.platform === 'win32') {
    const account =
      process.env.USERDOMAIN && process.env.USERNAME
        ? `${process.env.USERDOMAIN}\\${process.env.USERNAME}`
        : userInfo().username;
    try {
      await execFileAsync('icacls', [path, '/inheritance:r', '/grant', `${account}:F`], {
        timeout: 5000,
        windowsHide: true,
      });
    } catch (error) {
      // Warn instead of failing the write: the file exists, but the security
      // promise is broken, and the user must know.
      const reason = error instanceof Error ? error.message : String(error);
      console.error(
        `Warning: could not restrict permissions on ${path} to the current user (${reason});` +
          ` other local accounts may be able to read it.`,
      );
    }
  } else {
    await chmod(path, posixMode);
  }
}

/**
 * Atomically replace `path` with `data`: write `<path>.tmp-<pid>-<rand>`,
 * fsync, then rename over the target. Readers never observe a partial file.
 * @param path - destination file to create or replace
 * @param data - full file contents
 * @param options - mode / owner-restriction options for the final file
 */
export async function writeFileAtomic(
  path: string,
  data: string,
  options: AtomicWriteOptions = {},
): Promise<void> {
  await mkdir(dirname(path), { recursive: true });
  const tmp = `${path}.tmp-${process.pid}-${randomBytes(6).toString('hex')}`;
  const handle = await open(tmp, 'w');
  try {
    try {
      await handle.writeFile(data, 'utf8');
      await handle.sync();
    } finally {
      await handle.close();
    }
  } catch (error) {
    // The write was killed mid-flight: the target still holds its previous
    // bytes and the half-written temp file must not linger either.
    await rm(tmp, { force: true });
    throw error;
  }
  try {
    if (process.platform !== 'win32') {
      await chmod(tmp, options.restrictToOwner ? 0o600 : (options.mode ?? 0o644));
    }
    await rename(tmp, path);
  } catch (error) {
    await rm(tmp, { force: true });
    throw error;
  }
  // ACLs must be applied to the final path after the rename on win32: the
  // temp file was created with inherited permissions, and rename carries them.
  if (process.platform === 'win32' && options.restrictToOwner) {
    await restrictToFileOwner(path);
  }
}

/**
 * Whether a process with the given pid exists (EPERM counts as alive).
 * @param pid - process id to probe with signal 0
 * @returns true when the process exists
 */
function processAlive(pid: number): boolean {
  try {
    process.kill(pid, 0);
    return true;
  } catch (error) {
    return (error as NodeJS.ErrnoException).code === 'EPERM';
  }
}

/**
 * Whether the lockfile at `lockPath` may be stolen: its recorded pid is dead,
 * or it is older than the stale age regardless of the holder.
 * @param lockPath - lockfile to inspect
 * @returns true when the lock is stale and can be removed
 */
async function lockIsStale(lockPath: string): Promise<boolean> {
  let pid: number | null = null;
  try {
    pid = Number.parseInt((await readFile(lockPath, 'utf8')).trim(), 10);
  } catch {
    // Missing or non-numeric pid content: leave pid null.
  }
  if (pid !== null && Number.isInteger(pid) && pid > 0 && !processAlive(pid)) {
    return true;
  }
  try {
    const info = await stat(lockPath);
    return Date.now() - info.mtimeMs > LOCK_STALE_AGE_MS;
  } catch {
    // The lock vanished between EEXIST and now: treat as stale so the caller retries.
    return true;
  }
}

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Serialize a critical section across processes using an O_EXCL lockfile next
 * to `path`, with stale recovery: a lock held by a dead process or older than
 * 30s is removed and retried. Use for load-modify-save sections on state files.
 * Acquisition gives up after 10s with a typed TimeoutError (kind timeout) so a
 * permanently held lock surfaces as a deadline failure, never a hang.
 * @param path - state file the critical section owns (lock lives at `<path>.lock`)
 * @param fn - critical section; runs exactly once while the lock is held
 * @returns whatever `fn` resolves to
 */
export async function withLock<T>(path: string, fn: () => Promise<T>): Promise<T> {
  const lockPath = `${path}.lock`;
  await mkdir(dirname(lockPath), { recursive: true });
  const deadline = Date.now() + LOCK_ACQUIRE_TIMEOUT_MS;
  for (;;) {
    let handle;
    try {
      handle = await open(lockPath, 'wx');
    } catch (error) {
      if ((error as NodeJS.ErrnoException).code === 'EEXIST') {
        if (await lockIsStale(lockPath)) {
          await rm(lockPath, { force: true });
          continue;
        }
        if (Date.now() >= deadline) {
          // Typed taxonomy failure (kind timeout / code TIMEOUT): a plain Error
          // here would surface at the chokepoint as kind api / code INTERNAL,
          // misreporting a deadline as an unclassified failure.
          throw new TimeoutError(
            `timed out waiting for the lock on ${lockPath} (held by another dc3 process)`,
            { cause: error },
          );
        }
        await sleep(LOCK_POLL_INTERVAL_MS);
        continue;
      }
      throw error;
    }
    try {
      await handle.writeFile(String(process.pid), 'utf8');
      break;
    } finally {
      await handle.close();
    }
  }
  try {
    return await fn();
  } finally {
    await rm(lockPath, { force: true });
  }
}

/**
 * Move a corrupt/unreadable state file aside so later writes can never destroy
 * the bytes: the original path is freed for a fresh write while the corrupt
 * content survives under a timestamped sibling for manual recovery. Older
 * quarantine copies beyond {@link QUARANTINE_KEEP} are pruned so repeated
 * corruptions cannot accumulate unboundedly; pruning is best-effort and never
 * fails the quarantine itself.
 * @param path - file to quarantine
 * @returns the quarantine path, or null when the file does not exist
 */
export async function quarantineFile(path: string): Promise<string | null> {
  const stamp = new Date().toISOString().replace(/[:.]/gu, '-');
  const target = `${path}.corrupt-${stamp}-${randomBytes(3).toString('hex')}`;
  try {
    await rename(path, target);
  } catch (error) {
    if ((error as NodeJS.ErrnoException).code === 'ENOENT') {
      return null;
    }
    throw error;
  }
  await pruneQuarantineCopies(path);
  return target;
}

/**
 * Delete the oldest `<basename>.corrupt-*` siblings beyond the retention
 * count. ISO timestamps make lexicographic order chronological. Every failure
 * (listing or unlinking) only warns: the quarantine already succeeded and the
 * corrupt bytes are preserved, so a prunable leftover must not break the
 * recovery path that relies on this function.
 * @param path - state file whose quarantine siblings are pruned
 */
async function pruneQuarantineCopies(path: string): Promise<void> {
  const dir = dirname(path);
  const prefix = `${basename(path)}.corrupt-`;
  let names: string[];
  try {
    names = (await readdir(dir)).filter((name) => name.startsWith(prefix));
  } catch (error) {
    const reason = error instanceof Error ? error.message : String(error);
    console.error(`Warning: could not list old quarantine copies in ${dir} (${reason}).`);
    return;
  }
  names.sort();
  const excess = names.slice(0, Math.max(0, names.length - QUARANTINE_KEEP));
  for (const name of excess) {
    const victim = join(dir, name);
    try {
      await rm(victim, { force: true });
    } catch (error) {
      const reason = error instanceof Error ? error.message : String(error);
      console.error(`Warning: could not prune old quarantine copy ${victim} (${reason}).`);
    }
  }
}
