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

import {mount, type VueWrapper} from '@vue/test-utils';
import {createPinia, setActivePinia} from 'pinia';
import type {Component, ComponentOptions} from 'vue';
import {createMemoryHistory, createRouter, type RouteRecordRaw} from 'vue-router';

import i18n from '@/config/i18n';

import {createElButtonStub, createElFormStub, createElPaginationStub, layoutStubs} from '../setup/stubs/element-plus'; // Shared scaffolding for `tests/views/*.test.ts`. Most list pages share the

// Shared scaffolding for `tests/views/*.test.ts`. Most list pages share the
// same Element Plus surface (Card / Tool toolbar / Pagination / Tag) and the
// same lifecycle (mount → list API fires once → row renders). This helper
// folds that boilerplate into one call so each view test can stay focused
// on its own assertions.

export interface MountListPageOptions {
  /** Component under test. */
  component: Component;
  /** Props passed to the component. */
  props?: Record<string, unknown>;
  /** Slots passed to the component. */
  slots?: Record<string, string>;
  /** Initial route query. The page's setup() typically reads from useRoute(). */
  routeQuery?: Record<string, string>;
  /** Initial route name to push the test router to. Defaults to a noop /test route. */
  routePath?: string;
  /** Extra named routes the page may navigate to (router.push({ name }) calls). */
  extraRoutes?: RouteRecordRaw[];
  /** Additional stubs merged on top of layoutStubs + button/form/pagination. */
  stubs?: Record<string, ComponentOptions | unknown>;
  /** Skip the default ElForm stub when the component renders no form. */
  skipForm?: boolean;
}

export async function mountListPage(opts: MountListPageOptions): Promise<VueWrapper> {
  const noop = {template: '<div />'};

  // Some pages (Resource, agentic) reach for Pinia stores during setup.
  // Establish a fresh active pinia per mount so each test starts clean.
  setActivePinia(createPinia());

  const routes: RouteRecordRaw[] = [{name: 'test', path: '/test', component: noop}, ...(opts.extraRoutes ?? [])];

  const router = createRouter({
    history: createMemoryHistory(),
    routes,
  });

  const targetPath = opts.routePath ?? '/test';
  await router.push({path: targetPath, query: opts.routeQuery ?? {}});
  await router.isReady();

  const {ElForm} = createElFormStub();

  const wrapper = mount(opts.component, {
    props: opts.props,
    slots: opts.slots,
    global: {
      plugins: [i18n, router],
      // v-loading directive is registered by Element Plus at runtime but
      // not present under the unplugin auto-import we use in tests; the
      // strict warning handler in vitest.setup would promote a missing
      // directive to a thrown error. Stub it as a no-op here.
      directives: {loading: () => undefined},
      stubs: {
        ...layoutStubs,
        ElButton: createElButtonStub(),
        ElPagination: createElPaginationStub(),
        ...(opts.skipForm ? {} : {ElForm}),
        // Most list pages compose with these — provide light passthroughs
        // so dialog refs / popconfirms don't blow up the smoke path.
        BlankCard: {template: '<section class="blank-card-stub"><slot /></section>'},
        SkeletonCard: {template: '<div class="skeleton-card-stub"><slot /></div>'},
        ToolCard: {
          props: ['formModel', 'page', 'hideSort', 'hidePagination'],
          emits: ['search', 'reset', 'refresh', 'sort', 'size-change', 'current-change'],
          // Mirror the real ToolCard filters-slot contract (`:search`, `:form-data`)
          // so engine-driven pages that destructure `{ search }` don't hit an
          // undefined handler on `@keyup.enter`.
          template:
            '<section class="tool-card-stub"><slot name="filters" :form-data="formModel" :search="() => $emit(\'search\', formModel)" /><slot name="actions" /></section>',
        },
        ...(opts.stubs ?? {}),
      },
    },
  });

  return wrapper;
}
