# Brand assets

- `svg/` contains editable source artwork. Do not reference these files from Markdown.
- `png/` contains rendered assets for README files and other published uses.
- Run `node .github/brand/gen-banners.mjs` from the repository root after changing the banner template, localized text,
  or social preview source.

The generator reads `svg/banner.template.svg`, refreshes the localized SVG sources, and renders their PNG counterparts
into `png/`. It also renders `svg/social-preview.svg` to `png/social-preview.png`, keeping SVG files as editable sources
and PNG files as the published assets.
