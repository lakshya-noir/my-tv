package io.freemytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Small, cached India app schedules. No images, video, or foreground service; Home alone requests refreshes. */
final class Sports {
    private static Sports instance;
    static synchronized Sports get(Context c) {
        if (instance == null) instance = new Sports(c.getApplicationContext());
        return instance;
    }
    private static final String API = "https://www.thesportsdb.com/api/v1/json/123/";
    private final Context context;
    private final SharedPreferences cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Set<Recs.Listener> listeners = new HashSet<>();
    private volatile List<JSONObject> entries = Collections.emptyList();
    private volatile boolean fetching;
    private long askedAt;
    private String appsAt = "";

    private Sports(Context c) {
        context = c;
        cache = c.getSharedPreferences("sports", Context.MODE_PRIVATE);
        try {
            JSONArray saved = new JSONArray(cache.getString("entries", "[]"));
            List<JSONObject> restored = new ArrayList<>();
            for (int i = 0; i < saved.length(); i++) restored.add(saved.getJSONObject(i));
            entries = restored;
            askedAt = cache.getLong("checked", 0);
            appsAt = cache.getString("apps", "");
        } catch (Exception ignored) {}
    }

    void addListener(Recs.Listener listener) { listeners.add(listener); }
    void removeListener(Recs.Listener listener) { listeners.remove(listener); }
    private boolean enabled(SportsPolicy.Provider p) {
        return !Prefs.serviceOff(p.key) && HomeApps.launchIntent(context.getPackageManager(), p.pkg) != null;
    }

    void refreshIfStale() {
        if (!"IN".equals(Recs.REGION) || listeners.isEmpty() || fetching) return;
        StringBuilder apps = new StringBuilder();
        for (SportsPolicy.Provider p : SportsPolicy.PROVIDERS) if (enabled(p)) apps.append(p.id).append(',');
        long now = System.currentTimeMillis();
        if (apps.toString().equals(appsAt) && now >= askedAt && now - askedAt < 15 * SportsPolicy.MINUTE) return;
        appsAt = apps.toString();
        askedAt = now;
        if (apps.length() == 0) { entries = Collections.emptyList(); return; }
        String requested = appsAt;
        fetching = true;
        io.execute(() -> {
            List<JSONObject> found = new ArrayList<>();
            for (SportsPolicy.Provider p : SportsPolicy.PROVIDERS) {
                if (!requested.contains(p.id + ",")) continue;
                try {
                    JSONArray events = get("eventstv.php?id=" + p.id).optJSONArray("tvevents");
                    for (int i = 0; events != null && i < Math.min(6, events.length()); i++) {
                        JSONObject event = events.getJSONObject(i);
                        if (event.optString("strEvent").trim().isEmpty()) continue;
                        if (SportsPolicy.provider(event.optString("strCountry"), event.optString("idChannel"), event.optString("strChannel")) != p) continue;
                        String id = event.optString("idEvent");
                        if (!id.matches("[0-9]+")) continue;
                        JSONArray details = get("lookupevent.php?id=" + id).optJSONArray("events");
                        if (details == null || details.length() == 0) continue;
                        JSONObject detail = details.getJSONObject(0);
                        if (!id.equals(detail.optString("idEvent"))) continue;
                        event.put("status", detail.optString("strStatus"));
                        event.put("postponed", "yes".equalsIgnoreCase(detail.optString("strPostponed")));
                        event.put("checkedAt", System.currentTimeMillis());
                        found.add(event);
                    }
                } catch (Exception e) {
                    // Do not relabel cached live status as newly checked when a provider request fails.
                    for (JSONObject old : entries) if (p.id.equals(old.optString("idChannel"))) found.add(old);
                    AppLog.w("sports schedule: " + p.name + " " + e.getClass().getSimpleName());
                }
            }
            entries = found;
            JSONArray saved = new JSONArray();
            for (JSONObject event : found) saved.put(event);
            cache.edit().putString("entries", saved.toString()).putLong("checked", askedAt).putString("apps", requested).apply();
            fetching = false;
            main.post(() -> { for (Recs.Listener l : new ArrayList<>(listeners)) l.onRecs(); });
        });
    }

    List<Recs.Pick> now() {
        long now = System.currentTimeMillis();
        List<JSONObject> eligible = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (JSONObject event : entries) {
            SportsPolicy.Provider p = SportsPolicy.provider(event.optString("strCountry"), event.optString("idChannel"), event.optString("strChannel"));
            if (p == null || !enabled(p) || !seen.add(event.optString("idEvent") + ":" + p.id)) continue;
            if (SportsPolicy.visible(start(event), event.optString("status"), event.optBoolean("postponed"), event.optLong("checkedAt"), now)) eligible.add(event);
        }
        eligible.sort(Comparator.comparingLong(Sports::start));
        List<Recs.Pick> out = new ArrayList<>();
        for (JSONObject event : eligible) {
            SportsPolicy.Provider provider = SportsPolicy.provider(event.optString("strCountry"), event.optString("idChannel"), event.optString("strChannel"));
            Recs.Pick p = new Recs.Pick();
            p.id = event.optInt("idEvent");
            p.kind = "sport";
            p.title = event.optString("strEvent");
            p.sport = event.optString("strSport");
            p.app = provider.key;
            p.pkg = provider.pkg;
            p.detailed = true;
            p.sportsLive = SportsPolicy.live(start(event), event.optString("status"), event.optBoolean("postponed"), event.optLong("checkedAt"), now);
            SimpleDateFormat time = new SimpleDateFormat("EEE d MMM, h:mm a", Locale.ENGLISH);
            time.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata"));
            p.meta = (p.sportsLive ? Ui.tr("Live") : time.format(new Date(start(event))) + " IST") + " · " + provider.name;
            out.add(p);
            if (out.size() >= Recs.MAX_CARDS) break;
        }
        return out;
    }

    private static long start(JSONObject event) {
        return SportsPolicy.utc(event.optString("strTimeStamp", event.optString("dateEvent") + " " + event.optString("strTime")));
    }

    private static JSONObject get(String path) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(API + path).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(8000);
        c.setInstanceFollowRedirects(false);
        try {
            if (c.getResponseCode() != 200) throw new java.io.IOException("schedule unavailable");
            try (InputStream in = c.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] b = new byte[4096];
                for (int n; (n = in.read(b)) >= 0; ) {
                    if (out.size() + n > 256 * 1024) throw new java.io.IOException("schedule too large");
                    out.write(b, 0, n);
                }
                return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));
            }
        } finally { c.disconnect(); }
    }
}
