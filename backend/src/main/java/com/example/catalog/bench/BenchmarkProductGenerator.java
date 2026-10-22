/*
 * BUILD FLOW 08.04 — BenchmarkProductGenerator.java
 * First introduced: Phase 08
 * Purpose: Generates realistic, reproducible products for measurements: the same seed always produces the same
 *          products in the same order, on any machine.
 * Depends on: Category (04.02) and java.util.Random.
 * Used by: BenchmarkDataSeeder (08.05); BenchmarkProductGeneratorTest (08.06).
 * Runtime role: Runs only while benchmark data is being loaded.
 */
package com.example.catalog.bench;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import com.example.catalog.product.Category;

// BUILD FLOW 08.04.01 — Reproducible randomness
// java.util.Random started from a fixed seed produces the same sequence of numbers every time, and its algorithm is
// part of the Java specification, so the sequence is the same on every JVM. Only nextInt(bound) is used, whose exact
// algorithm is documented too, and there is no floating-point maths, whose last digits could differ between
// platforms. Time works the same way: creation times count back from a fixed instant, never from "now".
public final class BenchmarkProductGenerator {

    // One generated product: exactly the columns the seeder inserts.
    public record GeneratedProduct(String sku, String name, String description, Category category, BigDecimal price,
            int stockQuantity, Instant createdAt) {
    }

    // BUILD FLOW 08.04.02 — What each category looks like
    // A share of the catalog in percent, a price range in rupees, and words for names such as "Relaxed Linen Shirt 214".
    // The shares are uneven on purpose, like a real shop, so that filters differ in how many rows they match.
    private record Vocabulary(String code, int sharePercent, int minPrice, int maxPrice, List<String> styles,
            List<String> materials, List<String> items) {
    }

    private static final Map<Category, Vocabulary> VOCABULARY = Map.of(
            Category.APPAREL, new Vocabulary("APP", 24, 600, 9_000,
                    List.of("Relaxed", "Classic", "Everyday", "Cropped", "Oversized", "Slim", "Boxy", "Washed"),
                    List.of("Linen", "Organic Cotton", "Merino", "Denim", "Twill", "Jersey", "Corduroy", "Flannel"),
                    List.of("Shirt", "Tee", "Chinos", "Sweater", "Jacket", "Shorts", "Overshirt", "Hoodie", "Trousers",
                            "Polo")),
            Category.FOOTWEAR, new Vocabulary("FTW", 15, 1_000, 13_000,
                    List.of("Everyday", "Classic", "Lightweight", "Weekend", "Trail", "City"),
                    List.of("Canvas", "Leather", "Suede", "Recycled Knit", "Cork", "Rubber"),
                    List.of("Sneaker", "Sandal", "Boot", "Runner", "Loafer", "Slide", "Espadrille")),
            Category.BAGS, new Vocabulary("BAG", 13, 800, 15_000,
                    List.of("Everyday", "Weekend", "Compact", "Roll-top", "Utility", "Classic"),
                    List.of("Waxed Canvas", "Leather", "Recycled Nylon", "Cotton Canvas", "Jute", "Ripstop"),
                    List.of("Tote", "Backpack", "Sling", "Duffel", "Pouch", "Messenger", "Crossbody")),
            Category.HOME, new Vocabulary("HOM", 20, 300, 19_000,
                    List.of("Stonewashed", "Hand-thrown", "Woven", "Minimal", "Textured", "Classic"),
                    List.of("Linen", "Stoneware", "Oak", "Cotton", "Wool", "Rattan", "Glass"),
                    List.of("Throw", "Vase", "Lamp", "Towel", "Cushion", "Basket", "Rug", "Planter")),
            Category.KITCHEN, new Vocabulary("KIT", 16, 200, 10_000,
                    List.of("Everyday", "Speckled", "Hand-glazed", "Classic", "Pre-seasoned", "Minimal"),
                    List.of("Enamel", "Cast Iron", "Stoneware", "Olive Wood", "Copper", "Glass"),
                    List.of("Kettle", "Skillet", "Mug Set", "Serving Board", "Bowl", "Jug", "Pan")),
            Category.STATIONERY, new Vocabulary("STA", 12, 100, 3_000,
                    List.of("Pocket", "Desk", "Undated", "Classic", "Travel", "Minimal"),
                    List.of("Dot Grid", "Brass", "Linen", "Recycled Paper", "Leather"),
                    List.of("Notebook", "Pen", "Organiser", "Planner", "Journal", "Pencil Case", "Desk Tray")));

    private static final Instant NEWEST = Instant.parse("2026-10-01T00:00:00Z");
    private static final int TWO_YEARS_IN_SECONDS = 2 * 365 * 24 * 60 * 60;
    private static final int SOLD_OUT_PERCENT = 7;
    private static final int MAX_STOCK = 250;

    private final Random random;
    private int number;

    public BenchmarkProductGenerator(long seed) {
        this.random = new Random(seed);
    }

    // BUILD FLOW 08.04.03 — One product
    // The order of the random draws below is part of the dataset: changing it, or any word list, produces different
    // products. BenchmarkProductGeneratorTest pins the default dataset with a fingerprint, so such a change is noticed.
    // Prices are whole rupees ending in 90 (1,290.00), the way the demo catalog prices products.
    public GeneratedProduct next() {
        number++;
        Category category = pickCategory();
        Vocabulary words = VOCABULARY.get(category);
        String name = pick(words.styles()) + " " + pick(words.materials()) + " " + pick(words.items()) + " "
                + (100 + random.nextInt(900));
        int hundreds = words.minPrice() / 100 + random.nextInt(words.maxPrice() / 100 - words.minPrice() / 100 + 1);
        BigDecimal price = BigDecimal.valueOf(hundreds * 100L - 10).setScale(2);
        int stock = random.nextInt(100) < SOLD_OUT_PERCENT ? 0 : 1 + random.nextInt(MAX_STOCK);
        Instant createdAt = NEWEST.minusSeconds(random.nextInt(TWO_YEARS_IN_SECONDS))
                .minusNanos(random.nextInt(1_000_000) * 1_000L);
        // Locale.ROOT: some locales format digits differently, which would change the SKUs from machine to machine.
        String sku = "BEN-" + words.code() + "-" + String.format(Locale.ROOT, "%06d", number);
        String description = name + ". A generated product for search measurements, number " + number + ".";
        return new GeneratedProduct(sku, name, description, category, price, stock, createdAt);
    }

    // Walks the categories in the enum's fixed order (never the map's, whose order is not defined) and returns the one
    // whose share contains the roll: rolls 0-23 give APPAREL, 24-38 FOOTWEAR, and so on.
    private Category pickCategory() {
        int roll = random.nextInt(100);
        int upTo = 0;
        for (Category category : Category.values()) {
            upTo += VOCABULARY.get(category).sharePercent();
            if (roll < upTo) {
                return category;
            }
        }
        throw new IllegalStateException("The category shares must add up to 100");
    }

    private String pick(List<String> words) {
        return words.get(random.nextInt(words.size()));
    }
}
