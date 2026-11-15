package io.freemytv;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Conservative country, app, time, and live-status rules for public sports listings. */
final class SportsPolicy {
    static final long MINUTE = 60_000L, HOUR = 60 * MINUTE;
    static final class Provider {
        final String id, name, key, pkg;
        Provider(String id, String name, String key, String pkg) {
            this.id = id; this.name = name; this.key = key; this.pkg = pkg;
        }
    }
    static final Provider[] PROVIDERS = {
            new Provider("1168", "SonyLIV", "sonyliv", "com.sonyliv"),
            new Provider("582", "JioHotstar", "hotstar", "in.startv.hotstar")
    };

    static Provider provider(String country, String id, String channel) {
        if (!"India".equalsIgnoreCase(country)) return null;
        String name = channel.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        for (Provider p : PROVIDERS) {
            if (!p.id.equals(id)) continue;
            if (p.key.equals("sonyliv") && name.equals("sonyliv")) return p;
            if (p.key.equals("hotstar") && (name.equals("disneyhotstar") || name.equals("jiohotstar"))) return p;
        }
        return null;
    }

    static long utc(String timestamp) {
        if (timestamp == null) return 0;
        String text = timestamp.replace('T', ' ');
        if (text.endsWith("Z")) text = text.substring(0, text.length() - 1);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT);
        f.setLenient(false);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        ParsePosition pos = new ParsePosition(0);
        Date date = f.parse(text, pos);
        return date == null || pos.getIndex() != text.length() ? 0 : date.getTime();
    }

    static boolean terminal(String status, boolean postponed) {
        String s = status.toUpperCase(Locale.ROOT).replace(' ', '_');
        return postponed || s.equals("FT") || s.equals("AET") || s.equals("PEN") || s.equals("FINISHED")
                || s.equals("MATCH_FINISHED") || s.equals("CANC") || s.equals("CANCELLED") || s.equals("PST")
                || s.equals("POSTPONED") || s.equals("ABD") || s.equals("ABANDONED") || s.equals("AWD")
                || s.equals("AWARDED") || s.equals("WALKOVER");
    }

    static boolean live(long start, String status, boolean postponed, long checked, long now) {
        if (terminal(status, postponed) || start <= 0 || start > now || now - start > 12 * HOUR
                || checked > now || now - checked > 15 * MINUTE) return false;
        String s = status.toUpperCase(Locale.ROOT).replace(' ', '_');
        return s.equals("LIVE") || s.equals("IN_PROGRESS") || s.equals("IN_PLAY") || s.equals("1H")
                || s.equals("2H") || s.equals("HT") || s.equals("ET") || s.equals("BT")
                || s.equals("Q1") || s.equals("Q2") || s.equals("Q3") || s.equals("Q4")
                || s.equals("P1") || s.equals("P2") || s.equals("P3");
    }

    static boolean visible(long start, String status, boolean postponed, long checked, long now) {
        if (start <= 0 || checked <= 0 || checked > now || now - checked > 6 * HOUR || terminal(status, postponed)) return false;
        return live(start, status, postponed, checked, now) || start > now && start - now <= 7 * 24 * HOUR;
    }
}
