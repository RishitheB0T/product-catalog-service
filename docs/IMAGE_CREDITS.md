<!--
BUILD FLOW 12.40 — docs/IMAGE_CREDITS.md
First introduced: Phase 12
Purpose: Where every product photo comes from, under which license, what was changed, and the rules used to choose
         them. The storefront shows the same credits in its footer (frontend/src/lib/photoCredits.js).
Depends on: The photos in frontend/public/images/products/ (12.33–12.39), all from Wikimedia Commons.
Read by: Anyone adding or replacing a product photo, and anyone checking that the images may be used.
Runtime role: None. Documentation only.
-->

# Image credits

Seven of the 24 demo products have a photo. The other 17 show a designed placeholder (the product's initials on the
same mat), because no suitable free photo was found for them. The demo data stores each photo as a path on the
storefront's own site, for example `/images/products/frm-kit-002.jpg`.

## How the photos were chosen

All photos come from [Wikimedia Commons](https://commons.wikimedia.org/). Commons' API reports each file's license
as structured data, so every license below comes from Commons itself and was not typed in by hand. A photo was used
only if it met all four rules:

1. **A free license** that allows commercial use and changes: CC0, public domain, CC BY, or CC BY-SA.
2. **No visible brand or logo.** A fictional shop must not present another company's product as its own. Several
   good photos were rejected for this reason. For example, a canvas sneaker uploaded by its own manufacturer, a sling
   bag with a visible logo, and a tote with a maker's label.
3. **It shows what the product's name and description say**, closely enough not to mislead.
4. **It is a photo of the object**, not a person wearing it, a museum scan, or a scene.

Openverse, a search engine for openly licensed images with more modern stock photography, was tried first but did
not respond on 2026-10-30. Replacing the placeholders with better photos is open for later.

## What was changed

Every photo was resized to at most 1,000 pixels on its longer side and saved as a JPEG (quality 82), which also
removes camera metadata. The vase photo was cropped to the vase. The storefront shows each photo whole, centred on a
tinted mat, rather than cropping it to a fixed shape. The changed copies of CC BY-SA photos are shared under the same
CC BY-SA license as their originals.

## The photos

| Product | File | Original on Commons | Author | License | Changes |
|---|---|---|---|---|---|
| Relaxed Twill Chinos | `frm-app-003.jpg` | [Chino pants](https://commons.wikimedia.org/wiki/File:Chino_pants.jpg) | Kuha455405 | [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/) | Resized |
| Merino Crewneck Sweater | `frm-app-004.jpg` | [Swiss Army Wool Sweater](https://commons.wikimedia.org/wiki/File:Swiss_Army_Wool_Sweater_(15695462777).jpg) | Andrew Toskin | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | Resized |
| Suede Desert Boot | `frm-ftw-003.jpg` | [Chukka boot](https://commons.wikimedia.org/wiki/File:Chukka_boot.jpg) | Reinthal | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | Resized |
| Ceramic Bud Vase | `frm-hom-002.jpg` | [Naqshli sopol guldon](https://commons.wikimedia.org/wiki/File:Naqshli_sopol_guldon.png) | AIDEPCUL | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) | Cropped and resized |
| Waffle Cotton Bath Towel | `frm-hom-004.jpg` | [Zusammengelegte Handtücher](https://commons.wikimedia.org/wiki/File:Zusammengelegte_Handt%C3%BCcher.jpg) | Mattes | Public domain | Resized |
| Cast Iron Skillet 26 cm | `frm-kit-002.jpg` | [Griswold cast iron skillet](https://commons.wikimedia.org/wiki/File:Griswold_cast_iron_skillet.jpg) | NMorales (WMF) | [CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/) | Resized |
| Undated Weekly Planner | `frm-sta-004.jpg` | [Personal organizer with metallic ring binder](https://commons.wikimedia.org/wiki/File:Personal_organizer_with_metallic_ring_binder.jpg) | Old Photo Profile | [CC BY 2.0](https://creativecommons.org/licenses/by/2.0/) | Resized |

The skillet's file name mentions its maker, but no name or logo is visible in the photo. The towel photo needs no
credit (public domain), but it is listed like the others.

## Adding a photo

1. Check the four rules above, and record the source page, author, and license.
2. Save the file as `frontend/public/images/products/<sku in lower case>.jpg`, at most 1,000 pixels on its longer side.
3. Add it to the table above and to `frontend/src/lib/photoCredits.js`, so the footer credits it.
4. Point the product's `imageUrl` at `/images/products/<file>`. The API accepts such paths, and web addresses, since
   Phase 12.
5. Add the file to `docs/FILE_GUIDE.md` as a **(binary)** row.
