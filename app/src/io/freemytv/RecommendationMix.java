package io.freemytv;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Selects a rotating row, preferring two local titles for every international one. No Android or network work. */
final class RecommendationMix {
    interface Item {
        boolean local();
        boolean plex();
        String franchise();
        default boolean fresh() { return true; }
    }

    static <T extends Item> List<T> select(List<T> all, int cards, long hour, boolean preferLocal) {
        List<T> out = new ArrayList<>();
        if (cards <= 0 || all.isEmpty()) return out;
        Set<String> seen = new HashSet<>();
        if (preferLocal) {
            List<T> local = new ArrayList<>(), international = new ArrayList<>();
            for (T item : all) (item.local() ? local : international).add(item);
            int localCards = cards - cards / 3;
            List<T> localFresh = age(local, true), localOld = age(local, false);
            List<T> worldFresh = age(international, true), worldOld = age(international, false);
            add(out, seen, localFresh, localCards, hour, localCards);
            add(out, seen, localOld, localCards, hour, Math.max(1, localCards - out.size()));
            add(out, seen, worldFresh, cards, hour, Math.max(1, cards - out.size()));
            add(out, seen, worldOld, cards, hour, Math.max(1, cards - out.size()));
            // A small catalog or duplicate franchises must not leave otherwise playable slots empty.
            add(out, seen, localFresh, cards, hour, localCards);
            add(out, seen, localOld, cards, hour, Math.max(1, cards - out.size()));
        } else {
            add(out, seen, all, cards, hour, cards);
        }
        return out;
    }

    private static <T extends Item> List<T> age(List<T> pool, boolean fresh) {
        List<T> out = new ArrayList<>();
        for (T item : pool) if (item.fresh() == fresh) out.add(item);
        return out;
    }

    private static <T extends Item> void add(List<T> out, Set<String> seen, List<T> pool,
                                            int limit, long hour, int stride) {
        if (pool.isEmpty()) return;
        boolean plex = false;
        for (T item : out) plex |= item.plex();
        int start = (int) Math.floorMod(hour * stride, (long) pool.size());
        for (int i = 0; i < pool.size() && out.size() < limit; i++) {
            T item = pool.get((start + i) % pool.size());
            if (item.plex() && plex) continue;
            if (seen.add(item.franchise())) {
                out.add(item);
                plex |= item.plex();
            }
        }
    }
}
