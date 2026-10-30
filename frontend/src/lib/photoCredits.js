// BUILD FLOW 12.11 — frontend/src/lib/photoCredits.js
// First introduced: Phase 12
// Purpose: Who took each product photo, where it comes from, and under which license, for the credits the licenses
//          require. The same list, with how the photos were chosen, is in docs/IMAGE_CREDITS.md (12.40).
// Depends on: The photos in public/images/products/ (12.33–12.39), all from Wikimedia Commons.
// Used by: SiteFooter (12.28), which shows the credits on every page.
// Runtime role: A constant list; nothing is fetched.

// BUILD FLOW 12.11.01 — One entry per photo
// CC BY and CC BY-SA require the title, the author, the source, the license, and a note of any changes. The
// resized (and once cropped) copies of CC BY-SA photos are shared under that same license.
export const PHOTO_CREDITS = [
  {
    product: 'Relaxed Twill Chinos', title: 'Chino pants', author: 'Kuha455405',
    license: 'CC BY-SA 3.0', licenseUrl: 'https://creativecommons.org/licenses/by-sa/3.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Chino_pants.jpg', changes: 'resized',
  },
  {
    product: 'Merino Crewneck Sweater', title: 'Swiss Army Wool Sweater', author: 'Andrew Toskin',
    license: 'CC BY 2.0', licenseUrl: 'https://creativecommons.org/licenses/by/2.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Swiss_Army_Wool_Sweater_(15695462777).jpg', changes: 'resized',
  },
  {
    product: 'Suede Desert Boot', title: 'Chukka boot', author: 'Reinthal',
    license: 'CC BY-SA 4.0', licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Chukka_boot.jpg', changes: 'resized',
  },
  {
    product: 'Ceramic Bud Vase', title: 'Naqshli sopol guldon', author: 'AIDEPCUL',
    license: 'CC BY 4.0', licenseUrl: 'https://creativecommons.org/licenses/by/4.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Naqshli_sopol_guldon.png', changes: 'cropped and resized',
  },
  {
    product: 'Waffle Cotton Bath Towel', title: 'Zusammengelegte Handtücher', author: 'Mattes',
    license: 'Public domain', licenseUrl: null,
    source: 'https://commons.wikimedia.org/wiki/File:Zusammengelegte_Handt%C3%BCcher.jpg', changes: 'resized',
  },
  {
    product: 'Cast Iron Skillet 26 cm', title: 'Griswold cast iron skillet', author: 'NMorales (WMF)',
    license: 'CC BY-SA 4.0', licenseUrl: 'https://creativecommons.org/licenses/by-sa/4.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Griswold_cast_iron_skillet.jpg', changes: 'resized',
  },
  {
    product: 'Undated Weekly Planner', title: 'Personal organizer with metallic ring binder', author: 'Old Photo Profile',
    license: 'CC BY 2.0', licenseUrl: 'https://creativecommons.org/licenses/by/2.0/',
    source: 'https://commons.wikimedia.org/wiki/File:Personal_organizer_with_metallic_ring_binder.jpg', changes: 'resized',
  },
]
