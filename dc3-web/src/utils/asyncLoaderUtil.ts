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
  run: <T>(task: () => Promise<T>, options?: {rethrow?: boolean}) => Promise<T | undefined>;
} => {
  const loading = ref(false);

  const run = async <T>(task: () => Promise<T>, options?: {rethrow?: boolean}): Promise<T | undefined> => {
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
