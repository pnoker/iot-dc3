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
import {ref} from 'vue';

/**
 * The `loading.value = true; try { ... } catch { /* handled globally *\/ }
 * finally { loading.value = false; }` boilerplate was repeating 30+ times
 * across dashboard cards. This composable folds it into one call:
 *
 * <pre>
 *   const { loading, run } = useAsyncLoader();
 *   const load = () => run(async () => {
 *     const res = await someApi();
 *     rows.value = res.data;
 *   });
 * </pre>
 *
 * Errors are swallowed by default — the axios response interceptor already
 * surfaces toast / 401 redirect, so catching here again would double-report.
 * Pass {@code rethrow: true} if a caller genuinely needs to handle errors.
 */
export const useAsyncLoader = (): {
  loading: Ref<boolean>;
  run: <T>(task: () => Promise<T>, options?: { rethrow?: boolean }) => Promise<T | undefined>;
} => {
  const loading = ref(false);

  const run = async <T>(task: () => Promise<T>, options?: { rethrow?: boolean }): Promise<T | undefined> => {
    loading.value = true;
    try {
      return await task();
    } catch (err) {
      if (options?.rethrow) throw err;
      return undefined;
    } finally {
      loading.value = false;
    }
  };

  return {loading, run};
};
