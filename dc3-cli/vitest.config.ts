import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    globals: true,
    environment: 'node',
    include: ['test/**/*.test.ts'],
    // The credential-store tests derive AES keys and round-trip files through
    // the real tmpdir filesystem; on Windows (and on slower CI runners) those
    // sit close to vitest's 5s default under full-suite worker parallelism.
    // Budget real work, not assertions — the default starves legitimate I/O.
    testTimeout: 15_000,
    coverage: {
      provider: 'v8',
      include: ['src/**/*.ts'],
      // Ratchet floor for the coverage numbers reached by the hardening
      // campaign (90.14% statements / 77.06% branches / 95.34% functions /
      // ~90.16% lines), set one notch under so normal per-run jitter cannot
      // redden CI. `pnpm test:coverage` exits non-zero the moment any metric
      // drops below its floor, so the numbers are enforced, not decorative.
      thresholds: {
        statements: 89,
        branches: 76,
        functions: 94,
        lines: 88,
      },
    },
  },
});
