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

import type {FormItemRule} from 'element-plus';
import type {PageQuery} from '@/config/types';

/**
 * Minimal translation contract for semantic-layer configs (A1/A5 boundary):
 * a function from an i18n key to a localized string. Vue's ComposerTranslation
 * satisfies it structurally, so configs no longer import vue-i18n directly —
 * they stay consumable by a future framework-agnostic client SDK.
 */
export type Translator = (key: string, params?: Record<string, unknown>) => string;

export type EntityMode = 'page' | 'tree';
export type EntityPagination = 'offset' | 'cursor';
export type EntityFieldKind =
  'input' | 'number' | 'select' | 'enableFlag' | 'textarea' | 'json' | 'color' | 'treeSelect';
export type EntityColumnKind = 'text' | 'tag' | 'code' | 'time' | 'enable' | 'color' | 'icon' | 'link';
export type EntitySearchKind = 'input' | 'select' | 'enableFlag';

export interface EntityOption {
  label: string;
  value: string | number;
}

export interface EntityTreeSource {
  load: () => Promise<unknown[]>;
  props?: { label?: string; value?: string; children?: string; disabled?: string };
  checkStrictly?: boolean;
  /** Synchronous reactive filter/shaping using the live form model. Called on every render. */
  transform?: (rows: any[], form: Record<string, any>) => unknown[];
  /** el-tree-select node-key (default 'id') */
  nodeKey?: string;
}

export interface EntityFieldConfig {
  prop: string;
  label: string;
  kind?: EntityFieldKind; // Defaults to 'input'.
  options?: EntityOption[]; // select
  tree?: EntityTreeSource; // treeSelect
  placeholder?: string;
  required?: boolean;
  rules?: FormItemRule[]; // Additional rules merged with required/JSON validation.
  span?: number; // Grid span; defaults to 12.
  rows?: number; // textarea/json
  precision?: number; // number
  maxlength?: number;
  disabledOnEdit?: boolean; // Disable immutable fields such as userName while editing.
}

export interface EntityColumnContext {
  t: (key: string) => string;
  relations: Record<string, Record<string, string>>;
}

export interface EntityColumnConfig {
  prop: string; // Supports paths such as 'menuExt.content.url'.
  label: string;
  kind?: EntityColumnKind; // Defaults to 'text'.
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  overflow?: boolean; // Defaults to true.
  options?: EntityOption[]; // Value-to-label mapping for tag/text columns.
  formatter?: (row: Record<string, any>, ctx: EntityColumnContext) => string;
  onClick?: (row: Record<string, any>) => void; // link
  linkable?: (row: Record<string, any>) => boolean; // Link columns are clickable by default.
}

export interface EntitySearchFieldConfig {
  prop: string;
  label: string;
  kind: EntitySearchKind;
  options?: EntityOption[];
  placeholder?: string;
  multiple?: boolean; // Enables multi-select.
  includeAll?: boolean; // enableFlag segmented
}

export interface EntityRowAction {
  key: string;
  label: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  /** When set, the action renders inside el-popconfirm; onClick runs only after confirmation. */
  popconfirmTitle?: string;
  onClick: (row: Record<string, any>) => void;
}

/** Toolbar action for operations outside generic CRUD, such as refresh, registration, or connection creation. */
export interface EntityToolbarAction {
  key: string;
  label: string;
  icon?: string; // Icon name resolved through resolveIcon.
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  loading?: () => boolean; // Reactive loading state read from the owning page.
  onClick: () => void;
}

export interface EntityRelation {
  key: string; // ctx.relations[key]
  load: (rows: any[]) => Promise<Record<string, string>>;
}

export interface EntityListConfig {
  name: string; // Diagnostic and component name.
  title?: string; // Localized dialog entity name; falls back to name.
  mode?: EntityMode; // Defaults to 'page'.
  pagination?: EntityPagination; // Defaults to offset; cursor is for history/high-volume lists.
  editable: boolean;
  rowKey?: string; // Required in tree mode; defaults to 'id'.
  defaultExpandAll?: boolean; // tree
  pageSize?: number; // Page mode size; defaults to 12.
  defaultOrderColumn?: string; // Page sort column; defaults to 'create_time'.

  searchFields: EntitySearchFieldConfig[];
  columns: EntityColumnConfig[];
  fields: EntityFieldConfig[];
  defaultForm: () => Record<string, unknown>;
  relations?: EntityRelation[];

  /** Supplies derived form values when editing, overriding the default field mapping. */
  fromRow?: (row: Record<string, any>) => Record<string, unknown>;
  /** Builds the submitted payload instead of using the default field assembly. */
  toPayload?: (form: Record<string, any>) => Record<string, unknown>;

  list: (query: PageQuery) => Promise<any>;
  add?: (payload: Record<string, unknown>) => Promise<unknown>;
  update?: (payload: Record<string, unknown>) => Promise<unknown>;
  remove?: (id: string) => Promise<unknown>;

  detail?: { routeName: string }; // Detail route; omit to hide the detail action.
  extraActions?: EntityRowAction[];
  toolbarActions?: EntityToolbarAction[]; // Custom toolbar actions.
  operationWidth?: number; // Override when automatic sizing cannot fit long labels.
  rowEditable?: (row: Record<string, any>) => boolean; // Controls editability for individual rows.
  rowDeletable?: (row: Record<string, any>) => boolean;

  dialogWidth?: string; // Defaults to '720px'.
  confirmDeleteText?: string;
  emptyText?: string;
}
