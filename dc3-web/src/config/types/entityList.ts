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

import type {FormItemRule} from 'element-plus';
import type {PageQuery} from '@/config/types';

export type EntityMode = 'page' | 'tree';
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
  props?: {label?: string; value?: string; children?: string; disabled?: string};
  checkStrictly?: boolean;
  /** Synchronous reactive filter/shaping using the live form model. Called on every render. */
  transform?: (rows: any[], form: Record<string, any>) => unknown[];
  /** el-tree-select node-key (default 'id') */
  nodeKey?: string;
}

export interface EntityFieldConfig {
  prop: string;
  label: string;
  kind?: EntityFieldKind; // 默认 'input'
  options?: EntityOption[]; // select
  tree?: EntityTreeSource; // treeSelect
  placeholder?: string;
  required?: boolean;
  rules?: FormItemRule[]; // 追加校验（与 required/json 合并）
  span?: number; // 栅格，默认 12
  rows?: number; // textarea/json
  precision?: number; // number
  maxlength?: number;
  disabledOnEdit?: boolean; // 编辑态禁用（如 userName）
}

export interface EntityColumnContext {
  t: (key: string) => string;
  relations: Record<string, Record<string, string>>;
}

export interface EntityColumnConfig {
  prop: string; // 支持点路径，如 'menuExt.content.url'
  label: string;
  kind?: EntityColumnKind; // 默认 'text'
  width?: number | string;
  minWidth?: number | string;
  fixed?: boolean | 'left' | 'right';
  overflow?: boolean; // 默认 true
  options?: EntityOption[]; // tag/text 的值→标签映射来源
  formatter?: (row: Record<string, any>, ctx: EntityColumnContext) => string;
  onClick?: (row: Record<string, any>) => void; // link
  linkable?: (row: Record<string, any>) => boolean; // link，缺省视为可点击
}

export interface EntitySearchFieldConfig {
  prop: string;
  label: string;
  kind: EntitySearchKind;
  options?: EntityOption[];
  placeholder?: string;
  multiple?: boolean; // select 多选
  includeAll?: boolean; // enableFlag segmented
}

export interface EntityRowAction {
  key: string;
  label: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  onClick: (row: Record<string, any>) => void;
}

/** 工具栏级自定义按钮（与内置 Add 并列），用于无法走通用增删改的动作，如刷新目录、注册、新增连接。 */
export interface EntityToolbarAction {
  key: string;
  label: string;
  icon?: string; // 图标名，经 resolveIcon 解析
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  loading?: () => boolean; // 响应式 loading（闭包读取页面 ref）
  onClick: () => void;
}

export interface EntityRelation {
  key: string; // ctx.relations[key]
  load: (rows: any[]) => Promise<Record<string, string>>;
}

export interface EntityListConfig {
  name: string; // 调试/组件名
  title?: string; // 对话框标题用的本地化实体名；缺省回退 name
  mode?: EntityMode; // 默认 'page'
  editable: boolean;
  rowKey?: string; // tree 模式必填，默认 'id'
  defaultExpandAll?: boolean; // tree
  pageSize?: number; // page，默认 12
  defaultOrderColumn?: string; // page 排序列，默认 'create_time'

  searchFields: EntitySearchFieldConfig[];
  columns: EntityColumnConfig[];
  fields: EntityFieldConfig[];
  defaultForm: () => Record<string, unknown>;
  relations?: EntityRelation[];

  /** 编辑态打开时注入的派生/额外表单值（覆盖默认的按字段映射值）。 */
  fromRow?: (row: Record<string, any>) => Record<string, unknown>;
  /** 由表单构建提交载荷（存在时替代默认的载荷组装）。 */
  toPayload?: (form: Record<string, any>) => Record<string, unknown>;

  list: (query: PageQuery) => Promise<R>;
  add?: (payload: Record<string, unknown>) => Promise<R>;
  update?: (payload: Record<string, unknown>) => Promise<R>;
  remove?: (id: string) => Promise<R>;

  detail?: {routeName: string}; // 跳转详情；缺省则无 detail 按钮
  extraActions?: EntityRowAction[];
  toolbarActions?: EntityToolbarAction[]; // 工具栏自定义按钮
  operationWidth?: number; // 覆盖操作列宽（自动估算不适配长标签时）
  rowEditable?: (row: Record<string, any>) => boolean; // 行级编辑可用（resource 分组节点禁）
  rowDeletable?: (row: Record<string, any>) => boolean;

  dialogWidth?: string; // 默认 '720px'
  confirmDeleteText?: string;
  emptyText?: string;
}
