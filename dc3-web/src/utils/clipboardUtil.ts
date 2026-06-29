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

import {ElMessage} from 'element-plus';
import i18n from '@/config/i18n';

/**
 * Copy content to clipboard
 *
 * @param content Content to copy
 * @param detail Additional detail for message
 * @param message Custom message
 * @returns {Promise<boolean>} Whether copy was successful
 */
export const setCopyContent = async (content: unknown, detail?: boolean, message?: string): Promise<boolean> => {
  const textContent = String(content ?? '');

  // Try modern Clipboard API first (preferred, non-deprecated)
  if (navigator.clipboard && navigator.clipboard.writeText) {
    try {
      await navigator.clipboard.writeText(textContent);
      showSuccessMessage(textContent, detail, message);
      return true;
    } catch (error) {
      console.warn('Clipboard API failed, falling back to execCommand:', error);
      // Fall through to fallback method
    }
  }

  // Fallback to deprecated execCommand for older browsers or non-secure contexts
  return fallbackCopy(textContent, detail, message);
};

/**
 * Fallback copy method using execCommand
 */
const fallbackCopy = (content: string, detail?: boolean, message?: string): boolean => {
  const input = document.createElement('input');
  input.setAttribute('id', 'copy-id-input');
  input.setAttribute('value', content);
  document.body.appendChild(input);

  input.select();
  const success = document.execCommand('copy');

  if (success) {
    showSuccessMessage(content, detail, message);
  } else {
    ElMessage.error({message: i18n.global.t('common.clipboard.failed')});
  }

  // Safe DOM cleanup - check element exists before removal
  const inputElement = document.getElementById('copy-id-input');
  if (inputElement && inputElement.parentNode) {
    inputElement.parentNode.removeChild(inputElement);
  }

  return success;
};

/**
 * Show success message
 */
const showSuccessMessage = (content: string, detail?: boolean, message?: string): void => {
  let tip = i18n.global.t('common.clipboard.copied');
  if (detail) {
    tip = i18n.global.t('common.clipboard.copiedTarget', {target: message || content});
  }
  ElMessage.success({message: tip});
};
