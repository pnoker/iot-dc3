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
