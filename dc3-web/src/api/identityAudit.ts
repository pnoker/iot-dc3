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

import {httpPost} from '@/api/common';
import {API_IDENTITY_AUDIT_BASE} from '@/config/constant/api';
import type {IdentityAuditRecord} from '@/config/types/auth';

export const listIdentityAudit = (
  params: {
    principalId?: string;
    action?: string;
    resourceType?: string;
    resourceId?: string;
    status?: string;
    limit?: number;
  } = {}
) =>
  httpPost<R<IdentityAuditRecord[]>>(`${API_IDENTITY_AUDIT_BASE}/list`, undefined, {
    params: {
      principal_id: params.principalId,
      action: params.action,
      resource_type: params.resourceType,
      resource_id: params.resourceId,
      status: params.status,
      limit: params.limit,
    },
  });
