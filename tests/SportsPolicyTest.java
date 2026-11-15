package io.freemytv;

public final class SportsPolicyTest {
    static void check(boolean value, String why) { if (!value) throw new AssertionError(why); }
    public static void main(String[] args) {
        long now = SportsPolicy.utc("2026-11-08T14:00:00Z");
        check(now > 0 && now == SportsPolicy.utc("2026-11-08 14:00:00"), "timestamps are UTC");
        check(SportsPolicy.utc("2026-02-30 12:00:00") == 0, "reject impossible date");
        check(SportsPolicy.utc("2026-11-08 14:00:00garbage") == 0, "reject trailing content");
        check(SportsPolicy.provider("India", "1168", "Sony LIV") != null, "SonyLIV India mapping");
        check(SportsPolicy.provider("India", "582", "Disney+ Hotstar") != null, "India Hotstar alias");
        check(SportsPolicy.provider("UK", "1168", "Sony LIV") == null, "exclude foreign availability");
        check(SportsPolicy.provider("India", "111", "Sony LIV") == null, "exclude unverified channel ids");
        check(SportsPolicy.provider("India", "1168", "Sony Sports 1") == null, "TV channel is not streaming-app proof");
        check(SportsPolicy.visible(now + SportsPolicy.HOUR, "NS", false, now, now), "upcoming event");
        check(!SportsPolicy.live(now - 1000, "NS", false, now, now), "past start time alone cannot mean live");
        check(SportsPolicy.live(now - SportsPolicy.MINUTE, "1H", false, now, now), "fresh confirmed live status");
        check(!SportsPolicy.live(now - SportsPolicy.HOUR, "2H", false, now - 16 * SportsPolicy.MINUTE, now), "stale live status expires");
        check(!SportsPolicy.visible(now - SportsPolicy.HOUR, "FT", false, now, now), "finished hidden");
        check(!SportsPolicy.visible(now + SportsPolicy.HOUR, "NS", true, now, now), "postponed hidden");
        check(!SportsPolicy.visible(now + SportsPolicy.HOUR, "NS", false, now - 7 * SportsPolicy.HOUR, now), "stale schedule expires");
        check(!SportsPolicy.visible(now + 8 * 24 * SportsPolicy.HOUR, "NS", false, now, now), "limit upcoming horizon");
        check(!SportsPolicy.visible(0, "LIVE", false, now, now), "invalid date hidden");
        System.out.println("Sports policy: India app mapping, strict UTC, status freshness, expiry, postponements and terminal states passed.");
    }
}
