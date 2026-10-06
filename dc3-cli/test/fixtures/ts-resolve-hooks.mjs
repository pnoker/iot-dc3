// Module-resolution hook for child-process fixtures: TypeScript sources import
// compiled specifiers ending in .js, but only the .ts files exist in the
// working tree. Retry a failed .js resolution against the sibling .ts file
// (used together with Node's type stripping). Package specifiers resolve
// normally and never reach the retry.
export async function resolve(specifier, context, next) {
  try {
    return await next(specifier, context);
  } catch (error) {
    if (specifier.endsWith('.js')) {
      return next(specifier.replace(/\.js$/u, '.ts'), context);
    }
    throw error;
  }
}
