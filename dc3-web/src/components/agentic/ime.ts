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
 * True when a keydown event belongs to an IME composition session — most
 * importantly the Enter that confirms a candidate in Chinese/Japanese/Korean
 * input methods. Vue's `.enter` modifier does not filter these events out, so
 * every Enter-driven action on a text input must guard with this helper or
 * CJK users cannot confirm a candidate without accidentally submitting.
 *
 * Two signals are checked because engines disagree: `isComposing` is the
 * standard flag, while `keyCode === 229` is the legacy value several engines
 * (older Safari, some Android IMEs) report on the committing keydown with
 * `isComposing` already false.
 *
 * @param event - the keydown event to classify
 * @returns true when the event belongs to composition and must be ignored
 */
export const isImeCompositionKey = (event: KeyboardEvent): boolean => event.isComposing || event.keyCode === 229;
