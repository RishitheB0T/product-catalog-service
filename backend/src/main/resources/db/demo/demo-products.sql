-- BUILD FLOW 04.05 — demo-products.sql
-- First introduced: Phase 04
-- Purpose: A small, realistic demo catalog for local development: 24 products, four in each of the six
--          categories, prices in rupees, two products out of stock (for the in-stock filter).
-- Depends on: The product table from V1__create_product_table.sql (04.01).
-- Used by: DemoDataLoader (04.06), only with the "dev" profile and only when the table is empty.
--          Not a Flyway migration on purpose: Flyway tracks the schema; this is optional sample data.
-- Runtime role: Development only. Large benchmark data is generated separately (Phase 08).
--
-- created_at, updated_at, and version are left out, so the column defaults from V1 fill them (UTC now, 0).
-- image_url stays NULL until Phase 12 adds licensed product photos; the storefront shows a fallback until then.

-- BUILD FLOW 04.05.01 — The demo products
INSERT INTO product (sku, name, description, category, price, stock_quantity, image_url) VALUES
    ('FRM-APP-001', 'Organic Cotton Crew Tee', 'Heavyweight organic cotton tee with a relaxed fit and a ribbed crew neck. Pre-washed so it keeps its shape.', 'APPAREL', 1290.00, 48, NULL),
    ('FRM-APP-002', 'Linen Camp-Collar Shirt', 'Breathable European linen shirt with a camp collar and corozo buttons, cut for warm days.', 'APPAREL', 3490.00, 22, NULL),
    ('FRM-APP-003', 'Relaxed Twill Chinos', 'Mid-rise chinos in brushed cotton twill with a tapered leg and a hidden coin pocket.', 'APPAREL', 2990.00, 31, NULL),
    ('FRM-APP-004', 'Merino Crewneck Sweater', 'Fine-gauge merino wool sweater that regulates temperature and resists odour. Machine washable on a wool cycle.', 'APPAREL', 5490.00, 0, NULL),
    ('FRM-FTW-001', 'Canvas Low-Top Sneaker', 'Organic canvas upper on a vulcanised rubber sole, with a cushioned cork footbed.', 'FOOTWEAR', 3290.00, 26, NULL),
    ('FRM-FTW-002', 'Leather Slide Sandal', 'Vegetable-tanned leather strap on a contoured cork and latex footbed.', 'FOOTWEAR', 2490.00, 18, NULL),
    ('FRM-FTW-003', 'Suede Desert Boot', 'Unlined suede upper on a natural crepe sole that softens with wear.', 'FOOTWEAR', 6990.00, 9, NULL),
    ('FRM-FTW-004', 'Knit Everyday Runner', 'Lightweight recycled-knit upper with a responsive foam midsole for long days on foot.', 'FOOTWEAR', 4590.00, 14, NULL),
    ('FRM-BAG-001', 'Waxed Canvas Tote', 'Water-resistant waxed canvas tote with leather handles and an inside zip pocket.', 'BAGS', 2790.00, 35, NULL),
    ('FRM-BAG-002', 'Everyday Backpack 20L', 'Roll-top backpack with a padded laptop sleeve and a quick-access side pocket.', 'BAGS', 4990.00, 17, NULL),
    ('FRM-BAG-003', 'Crossbody Sling Bag', 'Compact sling in recycled nylon with an adjustable strap and two zip compartments.', 'BAGS', 1990.00, 40, NULL),
    ('FRM-BAG-004', 'Weekender Duffel', 'Forty-litre duffel in heavy canvas with a shoe compartment and a detachable shoulder strap.', 'BAGS', 6490.00, 7, NULL),
    ('FRM-HOM-001', 'Stonewashed Linen Throw', 'Soft stonewashed linen throw, 130 by 170 cm, finished with a hand-knotted fringe.', 'HOME', 3990.00, 12, NULL),
    ('FRM-HOM-002', 'Ceramic Bud Vase', 'Hand-thrown stoneware vase with a matte glaze. Each piece varies slightly.', 'HOME', 1490.00, 25, NULL),
    ('FRM-HOM-003', 'Oak Table Lamp', 'Solid oak base with a linen shade and a warm LED bulb included.', 'HOME', 5990.00, 6, NULL),
    ('FRM-HOM-004', 'Waffle Cotton Bath Towel', 'Quick-drying waffle-weave cotton towel that gets softer with every wash.', 'HOME', 1190.00, 60, NULL),
    ('FRM-KIT-001', 'Enamel Pour-Over Kettle', 'Gooseneck enamel kettle for precise pouring, suitable for gas and induction hobs.', 'KITCHEN', 2890.00, 15, NULL),
    ('FRM-KIT-002', 'Cast Iron Skillet 26 cm', 'Pre-seasoned cast iron skillet that goes from hob to oven to table.', 'KITCHEN', 3590.00, 20, NULL),
    ('FRM-KIT-003', 'Stoneware Mug Set of 2', 'Two stoneware mugs with a speckled glaze, safe for the dishwasher and the microwave.', 'KITCHEN', 1290.00, 44, NULL),
    ('FRM-KIT-004', 'Olive Wood Serving Board', 'Serving board cut from a single piece of olive wood and finished with food-safe oil.', 'KITCHEN', 2190.00, 11, NULL),
    ('FRM-STA-001', 'Dot Grid Notebook A5', 'Numbered pages of thick, smooth paper with a lay-flat binding and two ribbon markers.', 'STATIONERY', 890.00, 80, NULL),
    ('FRM-STA-002', 'Brass Ballpoint Pen', 'Solid brass pen that develops a warm patina over time; takes standard refills.', 'STATIONERY', 1690.00, 30, NULL),
    ('FRM-STA-003', 'Linen Desk Organiser', 'Linen-wrapped organiser with three compartments for pens, cards, and cables.', 'STATIONERY', 1390.00, 0, NULL),
    ('FRM-STA-004', 'Undated Weekly Planner', 'Undated weekly planner with monthly overviews, habit trackers, and a back pocket for receipts.', 'STATIONERY', 990.00, 50, NULL);
