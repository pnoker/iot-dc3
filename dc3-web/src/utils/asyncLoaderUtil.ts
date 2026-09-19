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

import type {Ref} from 'vue';
import {getCurrentInstance, onUnmounted, ref} from 'vue';

/**
 * The `loading.value = true; try { ... } catch { /* handled globally *\/ }
 * finally { loading.value = false; }` boilerplate was repeating 30+ times
 * across dashboard cards. This composable folds it into one call:
 *
 * <pre>
 *   const { loading, run } = useAsyncLoader();
 *   const load = () => run(
 *     () => someApi(),
 *     { apply: (rows) => { data.value = rows; } }
 *   );
 * </pre>
 *
 * Errors are swallowed by default — the axios response interceptor already
 * surfaces toast / 401 redirect, so catching here again would double-report.
 * Pass `rethrow: true` if a caller genuinely needs to handle errors.
 * @returns the composable handle
 */
export const useAsyncLoader = (): {
  loading: Ref<boolean>;
  error: Ref<unknown | null>;
  status: Ref<'idle' | 'loading' | 'success' | 'error'>;
  run: <T>(
    task: () => Promise<T>,
    options?: { apply?: (result: T) => void; rethrow?: boolean }
  ) => Promise<T | undefined>;
  /** Invalidates an in-flight task so it cannot commit after its owner closes. */
  invalidate: () => void;
} => {
  const loading = ref(false);
  const error = ref<unknown | null>(null);
  const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle');
  let latestRunId = 0;

  const invalidate = () => {
    latestRunId += 1;
    loading.value = false;
  };

  // A composable is also used directly by utility tests and non-component
  // callers, so only register the lifecycle hook when a Vue instance exists.
  if (getCurrentInstance()) onUnmounted(invalidate);

  const run = async <T>(
    task: () => Promise<T>,
    options?: { apply?: (result: T) => void; rethrow?: boolean }
  ): Promise<T | undefined> => {
    const runId = ++latestRunId;
    loading.value = true;
    error.value = null;
    status.value = 'loading';
    try {
      const result = await task();
      if (runId !== latestRunId) return undefined;
      options?.apply?.(result);
      status.value = 'success';
      return result;
    } catch (runError) {
      if (runId === latestRunId) {
        error.value = runError;
        status.value = 'error';
      }
      if (options?.rethrow) throw runError;
      return undefined;
    } finally {
      if (runId === latestRunId) loading.value = false;
    }
  };

  return {loading, error, status, run, invalidate};
};
