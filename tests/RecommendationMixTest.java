package io.freemytv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Run with tools/test-recommendation-mix.sh; uses no Android runtime or external API. */
public final class RecommendationMixTest {
    static final class Title implements RecommendationMix.Item {
        final String name;
        final boolean local, plex, fresh;
        Title(String name, boolean local) { this(name, local, false); }
        Title(String name, boolean local, boolean plex) { this(name, local, plex, true); }
        Title(String name, boolean local, boolean plex, boolean fresh) { this.name = name; this.local = local; this.plex = plex; this.fresh = fresh; }
        public boolean local() { return local; }
        public boolean plex() { return plex; }
        public String franchise() { return name; }
        public boolean fresh() { return fresh; }
    }

    static void check(boolean ok, String reason) {
        if (!ok) throw new AssertionError(reason);
    }

    static void valid(List<Title> out, int size) {
        check(out.size() == size, "expected " + size + " cards, got " + out.size());
        Set<String> names = new HashSet<>();
        int plex = 0;
        for (Title t : out) {
            check(names.add(t.name), "duplicate franchise " + t.name);
            if (t.plex) plex++;
        }
        check(plex <= 1, "multiple Plex titles in one tab");
    }

    public static void main(String[] args) {
        List<Title> catalog = new ArrayList<>();
        for (int i = 0; i < 8; i++) catalog.add(new Title("Indian " + i, true));
        for (int i = 0; i < 8; i++) catalog.add(new Title("International " + i, false));
        Set<String> rotated = new HashSet<>();
        for (long hour = 0; hour < 48; hour++) {
            List<Title> row = RecommendationMix.select(catalog, 3, hour, true);
            valid(row, 3);
            check(row.get(0).local && row.get(1).local && !row.get(2).local, "two Indian titles then one international");
            for (Title t : row) rotated.add(t.name);
            List<Title> six = RecommendationMix.select(catalog, 6, hour, true);
            valid(six, 6);
            check(six.stream().filter(t -> t.local).count() == 4, "six cards use a 4:2 mix");
        }
        check(rotated.size() == 16, "rotation reaches both full catalogs");
        // Pool lengths divisible by the row size must not permanently skip every third Indian title.
        List<Title> divisible = new ArrayList<>(catalog.subList(0, 6));
        divisible.addAll(catalog.subList(8, 11));
        Set<String> reached = new HashSet<>();
        for (int h = 0; h < 24; h++) for (Title t : RecommendationMix.select(divisible, 3, h, true)) reached.add(t.name);
        check(reached.size() == 9, "rotation reaches every title with six local and three international candidates");
        valid(RecommendationMix.select(Arrays.asList(new Title("Only local", true),
                new Title("Foreign A", false), new Title("Foreign B", false)), 3, 7, true), 3);
        List<Title> localOnly = catalog.subList(0, 8);
        valid(RecommendationMix.select(localOnly, 3, 4, true), 3);
        valid(RecommendationMix.select(catalog.subList(8, 16), 3, 4, true), 3);
        valid(RecommendationMix.select(Arrays.asList(new Title("Same", true), new Title("Same", true),
                new Title("Other", true), new Title("Foreign", false)), 3, 0, true), 3);
        List<Title> withPlex = Arrays.asList(new Title("Plex A", false, true), new Title("Plex B", false, true),
                new Title("Indian A", true), new Title("Indian B", true), new Title("Foreign", false));
        for (int h = 0; h < 24; h++) valid(RecommendationMix.select(withPlex, 3, h, true), 3);
        List<Title> original = RecommendationMix.select(catalog, 3, 1, false);
        check(original.get(0) == catalog.get(3) && original.get(2) == catalog.get(5), "ordinary rotation unchanged");
        check(RecommendationMix.select(catalog, 1, 0, true).get(0).local, "single card prefers Indian content");
        valid(RecommendationMix.select(catalog, 3, -1, true), 3);
        valid(RecommendationMix.select(new ArrayList<Title>(), 3, 0, true), 0);
        valid(RecommendationMix.select(catalog, 0, 0, true), 0);
        List<Title> mixedAge = Arrays.asList(new Title("Recent Indian A", true), new Title("Recent Indian B", true),
                new Title("Old Indian", true, false, false), new Title("Recent international", false),
                new Title("Old international", false, false, false));
        for (int h = 0; h < 48; h++) {
            List<Title> row = RecommendationMix.select(mixedAge, 3, h, true);
            valid(row, 3);
            check(row.stream().allMatch(t -> t.fresh), "old titles cannot displace enough recent matches");
        }
        valid(RecommendationMix.select(Arrays.asList(new Title("Recent", true),
                new Title("Older fallback", true, false, false), new Title("International", false)), 3, 0, true), 3);
        List<Title> scarce = new ArrayList<>();
        scarce.add(new Title("Only fresh Indian", true));
        for (int i = 0; i < 5; i++) scarce.add(new Title("Old fallback " + i, true, false, false));
        scarce.add(new Title("Fresh international", false));
        Set<String> fallbackRotation = new HashSet<>();
        for (int h = 0; h < 30; h++) {
            List<Title> row = RecommendationMix.select(scarce, 3, h, true);
            valid(row, 3);
            check(row.get(0).name.equals("Only fresh Indian"), "scarce fresh title must still come before older fallback");
            check(row.get(2).name.equals("Fresh international"), "fresh international title retained");
            fallbackRotation.add(row.get(1).name);
        }
        check(fallbackRotation.size() == 5, "all older fallback titles can rotate when needed");
        System.out.println("Recommendation mix: 48-hour rotation, 2:1 and 4:2 ratios, catalog fallbacks, franchise and Plex limits passed.");
    }
}
