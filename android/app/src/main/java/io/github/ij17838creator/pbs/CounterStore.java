package io.github.ij17838creator.pbs;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 알림창 ＋1 버튼이 누른 기록을 기기에 적어 두는 곳.
 * 앱 화면이 꺼져 있거나 앱이 종료돼 있어도 여기에 쌓였다가, 앱을 다시 열면 앱 자료로 옮겨진다.
 */
final class CounterStore {
    private static final String PREFS = "pbs_counter";

    private CounterStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static synchronized void start(Context c, String key, String title, int count) {
        prefs(c).edit()
            .putBoolean("active", true)
            .putString("key", key)
            .putString("title", title)
            .putInt("count", Math.max(0, count))
            .putLong("last", 0L)
            .apply();
    }

    static synchronized void stop(Context c) {
        prefs(c).edit().putBoolean("active", false).apply();
    }

    static synchronized void setCount(Context c, String key, int count) {
        SharedPreferences p = prefs(c);
        if (!key.equals(p.getString("key", ""))) return;
        p.edit().putInt("count", Math.max(0, count)).apply();
    }

    static boolean active(Context c) { return prefs(c).getBoolean("active", false); }
    static String key(Context c) { return prefs(c).getString("key", ""); }
    static String title(Context c) { return prefs(c).getString("title", "PBS 기록"); }
    static int count(Context c) { return prefs(c).getInt("count", 0); }
    static long last(Context c) { return prefs(c).getLong("last", 0L); }

    /** 버튼 한 번 = 기록 한 줄. d는 +1 또는 -1. 반환값: 바뀐 뒤 횟수 */
    static synchronized int add(Context c, int d) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false)) return p.getInt("count", 0);
        int count = p.getInt("count", 0);
        if (d < 0 && count <= 0) return 0;
        count = Math.max(0, count + d);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        try {
            JSONObject o = new JSONObject();
            o.put("t", now);
            o.put("d", d);
            o.put("key", p.getString("key", ""));
            ev.put(o);
        } catch (JSONException ignored) {}
        SharedPreferences.Editor e = p.edit().putInt("count", count).putString("events", ev.toString());
        if (d > 0) e.putLong("last", now);
        e.commit();
        return count;
    }

    /** 쌓인 기록을 모두 꺼내고 비운다 */
    static synchronized JSONArray drain(Context c) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        p.edit().putString("events", "[]").commit();
        return ev;
    }

    private static JSONArray events(SharedPreferences p) {
        try { return new JSONArray(p.getString("events", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
}
