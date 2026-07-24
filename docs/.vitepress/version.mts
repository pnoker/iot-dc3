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

import {readFileSync} from 'node:fs'
import {fileURLToPath} from 'node:url'
import {resolve} from 'node:path'

export interface VersionInfo {
    /** Display version, e.g. "2026.5" (trimmed from POM patch version) */
    version: string
    /** Full version including patch, e.g. "2026.5.22" */
    fullVersion: string
    /** Whether this build is the latest (main/release branch). Archived builds set DC3_DOCS_IS_LATEST=false */
    isLatest: boolean
}

const POM_PATH = resolve(fileURLToPath(new URL('../../pom.xml', import.meta.url)))

/**
 * Resolve the documentation version at build time.
 *
 * Resolution order:
 *   1. DC3_DOCS_VERSION env var (set by CI for versioned/archived builds)
 *   2. Parse <version> from the parent pom.xml (e.g. "2026.5.22")
 *   3. Fallback to "dev"
 *
 * The display version is trimmed to YYYY.M (patch releases share the same docs).
 */
export function resolveVersion(): VersionInfo {
    const envVersion = process.env.DC3_DOCS_VERSION

    if (envVersion) {
        const parts = envVersion.split('.')
        const display = parts.length >= 2 ? parts.slice(0, 2).join('.') : envVersion
        return {
            version: display,
            fullVersion: envVersion,
            isLatest: process.env.DC3_DOCS_IS_LATEST !== 'false'
        }
    }

    try {
        const pom = readFileSync(POM_PATH, 'utf8')
        const match = pom.match(/<version>(\d{4}\.\d+\.\d+)<\/version>/)
        if (match) {
            const full = match[1]
            const parts = full.split('.')
            return {
                version: `${parts[0]}.${parts[1]}`,
                fullVersion: full,
                isLatest: true
            }
        }
    } catch (_) {
        // POM not found — standalone docs deployment
    }

    return {version: 'dev', fullVersion: 'dev', isLatest: true}
}
