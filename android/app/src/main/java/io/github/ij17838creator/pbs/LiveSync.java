package io.github.ij17838creator.pbs;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 칠판·PC와 함께 세기(앱 화면이 꺼져 있어도).
 * 공유 프로젝트(측정 방법 빈도)의 회기가 열려 있는 동안 포그라운드 서비스로 돌며 2초마다
 *  - 서버 칸 '~live/host'에 회기 상태(열림·횟수·받은 번호)를 쓰고
 *  - 다른 기기가 쓴 '~live/tap/<기기>'를 읽어 새 ＋1·−1을 이 기기 회기(CounterStore)에 넣는다(앱이 열리면 natDrain이 회기 기록으로 옮김).
 * 서버는 Firestore REST + 구글 로그인 토큰(앱 화면이 setLive로 넘겨 줌, 만료되면 refresh 토큰으로 새로 받음).
 * 웹 화면(index.html) 쪽 같은 기능(liveHostTick)은 이 앱(ver 10)에서는 쓰지 않는다.
 */
public class LiveSync extends Service {
    private static final int NOTI_ID = 7301;
    private static volatile LiveSync inst;
    private HandlerThread ht;
    private Handler h;
    private final Runnable loop = this::step;

    private static SharedPreferences prefs(Context c) { return CounterStore.prefs(c); }

    /** 앱 화면이 넘겨 주는 설정: {on, apiKey, project, uid, name, dev, idToken, refresh, map:{기기 프로젝트 id: 공유 id}} */
    static void config(Context c, JSONObject o) {
        SharedPreferences.Editor e = prefs(c).edit();
        boolean on = o.optBoolean("on", false);
        e.putBoolean("lvOn", on);
        if (on) {
            e.putString("lvKey", o.optString("apiKey", ""))
                .putString("lvProj", o.optString("project", ""))
                .putString("lvUid", o.optString("uid", ""))
                .putString("lvName", o.optString("name", ""))
                .putString("lvDev", o.optString("dev", ""))
                .putString("lvMap", o.optJSONObject("map") != null ? o.optJSONObject("map").toString() : "{}");
            String rt = o.optString("refresh", "");
            if (!rt.isEmpty()) e.putString("lvRt", rt);
            String it = o.optString("idToken", "");
            if (!it.isEmpty()) e.putString("lvTok", it).putLong("lvExp", System.currentTimeMillis() + 50 * 60000L);
        }
        e.commit();
    }

    static boolean cfgOn(Context c) {
        SharedPreferences p = prefs(c);
        return p.getBoolean("lvOn", false) && !p.getString("lvUid", "").isEmpty() && !p.getString("lvRt", "").isEmpty()
            && !p.getString("lvProj", "").isEmpty() && !p.getString("lvDev", "").isEmpty();
    }

    private static String sharedPid(Context c, String localPid) {
        try { return new JSONObject(prefs(c).getString("lvMap", "{}")).optString(localPid, ""); }
        catch (Exception e) { return ""; }
    }

    /** 지금 칠판으로 보낼 회기 key(없으면 "") */
    static String want(Context c) {
        if (!cfgOn(c) || !CounterStore.active(c) || !CounterStore.FREQ.equals(CounterStore.mode(c))) return "";
        String key = CounterStore.key(c);
        if (key.startsWith("live|")) return "";
        String[] a = key.split("\\|", 3);
        if (a.length < 3 || !("bl".equals(a[1]) || "iv".equals(a[1]))) return "";
        return sharedPid(c, a[0]).isEmpty() ? "" : key;
    }

    /** 진단용 상태(앱 화면에 보임) */
    static JSONObject status(Context c) {
        SharedPreferences p = prefs(c);
        JSONObject o = new JSONObject();
        try {
            o.put("on", cfgOn(c));
            o.put("host", p.getString("lvHost", ""));
            o.put("err", p.getString("lvErr", ""));
            o.put("ok", p.getLong("lvOk", 0L));
            o.put("alive", inst != null);
        } catch (Exception ignored) {}
        return o;
    }

    /** 상태가 바뀔 때마다 부른다(CounterNotifier.refreshAll) */
    static void sync(Context c) {
        final Context a = c.getApplicationContext();
        SharedPreferences p = prefs(a);
        String w = want(a);
        String cur = p.getString("lvHost", "");
        if (!cur.isEmpty() && !cur.equals(w)) { // 회기 끝 또는 다른 회기로 바뀜
            final String old = cur;
            p.edit().putString("lvHost", "").putString("lvSig", "").commit();
            bg(() -> sendEnd(a, old));
        }
        if (w.isEmpty()) {
            if (inst != null) { try { a.stopService(new Intent(a, LiveSync.class)); } catch (Exception ignored) {} }
            return;
        }
        if (!w.equals(cur)) {
            p.edit().putString("lvHost", w).putString("lvSig", "").putString("lvSeen", "{}").commit();
        }
        LiveSync s = inst;
        if (s != null && s.h != null) s.h.post(s.loop);
        else {
            try { ContextCompat.startForegroundService(a, new Intent(a, LiveSync.class)); }
            catch (Exception e) { bg(() -> { try { pushHost(a, w, true); } catch (Exception x) { err(a, x); } }); }
        }
    }

    private static void bg(Runnable r) { new Thread(r, "pbs-live").start(); }

    @Override
    public void onCreate() {
        super.onCreate();
        ht = new HandlerThread("pbs-live");
        ht.start();
        h = new Handler(ht.getLooper());
        inst = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTI_ID, note(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(NOTI_ID, note());
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (want(this).isEmpty()) { stopSelf(); return START_NOT_STICKY; }
        h.post(loop);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (inst == this) inst = null;
        if (h != null) h.removeCallbacksAndMessages(null);
        if (ht != null) ht.quitSafely();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private Notification note() {
        Intent open = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 62, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        CounterNotifier.ensureChannel(this);
        return new NotificationCompat.Builder(this, CounterNotifier.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pbs)
            .setContentTitle("칠판·PC와 함께 세는 중")
            .setContentText("회기가 끝나면 저절로 꺼져요")
            .setOngoing(true).setSilent(true).setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pi)
            .build();
    }

    /** 2초마다: 상태 쓰기 + 다른 기기 ＋1 받기 */
    private void step() {
        h.removeCallbacks(loop);
        Context c = this;
        String w = want(c);
        if (w.isEmpty() || !w.equals(prefs(c).getString("lvHost", ""))) { stopSelf(); return; }
        try {
            int got = pollTaps(c, w);
            pushHost(c, w, got > 0);
            ok(c);
        } catch (Exception e) {
            err(c, e);
        }
        h.postDelayed(loop, 2000);
    }

    private static void ok(Context c) {
        prefs(c).edit().putString("lvErr", "").putLong("lvOk", System.currentTimeMillis()).apply();
    }

    private static void err(Context c, Exception e) {
        String m = e.getMessage();
        if (m == null) m = e.getClass().getSimpleName();
        if (m.length() > 80) m = m.substring(0, 80);
        prefs(c).edit().putString("lvErr", m).apply();
    }

    private static String liveId(String key) {
        String[] a = key.split("\\|", 3);
        return a.length < 3 ? key : a[2];
    }

    private static String pidOf(Context c, String key) {
        return sharedPid(c, key.split("\\|", 3)[0]);
    }

    /** 회기 상태 쓰기. 바뀐 게 없으면 1분마다만(살아 있음) */
    static void pushHost(Context c, String key, boolean force) throws Exception {
        SharedPreferences p = prefs(c);
        String pid = pidOf(c, key);
        if (pid.isEmpty()) return;
        JSONObject d = new JSONObject();
        d.put("on", true);
        d.put("id", liveId(key));
        d.put("st", CounterStore.hm(p.getLong("sesStart", System.currentTimeMillis())));
        d.put("title", CounterStore.title(c));
        d.put("n", CounterStore.count(c));
        d.put("seen", new JSONObject(p.getString("lvSeen", "{}")));
        d.put("dev", p.getString("lvDev", ""));
        String sig = d.toString();
        long now = System.currentTimeMillis();
        if (!force && sig.equals(p.getString("lvSig", "")) && now - p.getLong("lvBeat", 0L) < 60000L) return;
        d.put("at", now);
        writeCell(c, pid, "~live/host", d.toString());
        p.edit().putString("lvSig", sig).putLong("lvBeat", now).apply();
    }

    static void sendEnd(Context c, String key) {
        try {
            String pid = pidOf(c, key);
            if (pid.isEmpty()) return;
            JSONObject d = new JSONObject();
            d.put("on", false);
            d.put("id", liveId(key));
            d.put("dev", prefs(c).getString("lvDev", ""));
            d.put("at", System.currentTimeMillis());
            writeCell(c, pid, "~live/host", d.toString());
            ok(c);
        } catch (Exception e) {
            err(c, e);
        }
    }

    /** 다른 기기가 누른 ＋1·−1을 번호로 한 번씩만 넣는다. 반환값: 넣은 개수 */
    private static int pollTaps(Context c, String key) throws Exception {
        SharedPreferences p = prefs(c);
        String pid = pidOf(c, key);
        if (pid.isEmpty()) return 0;
        String id = liveId(key);
        String me = p.getString("lvDev", "");
        JSONObject q = new JSONObject("{\"structuredQuery\":{\"from\":[{\"collectionId\":\"cells\"}],\"where\":{\"compositeFilter\":{\"op\":\"AND\",\"filters\":["
            + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"path\"},\"op\":\"GREATER_THAN_OR_EQUAL\",\"value\":{\"stringValue\":\"~live/tap/\"}}},"
            + "{\"fieldFilter\":{\"field\":{\"fieldPath\":\"path\"},\"op\":\"LESS_THAN\",\"value\":{\"stringValue\":\"~live/tap/\"}}}]}}}}");
        String res = http(c, "POST", base(c) + "/projects/" + seg(pid) + ":runQuery", q.toString());
        JSONArray arr = new JSONArray(res);
        JSONObject seen = new JSONObject(p.getString("lvSeen", "{}"));
        int n = 0;
        for (int i = 0; i < arr.length(); i++) {
            JSONObject doc = arr.getJSONObject(i).optJSONObject("document");
            if (doc == null) continue;
            JSONObject f = doc.optJSONObject("fields");
            if (f == null || f.optJSONObject("value") == null) continue;
            JSONObject v;
            try { v = new JSONObject(f.getJSONObject("value").optString("stringValue", "")); } catch (Exception e) { continue; }
            String dev = v.optString("dev", "");
            if (!id.equals(v.optString("id")) || dev.isEmpty() || dev.equals(me)) continue;
            JSONArray evs = v.optJSONArray("evs");
            if (evs == null) continue;
            long top = seen.optLong(dev, 0L);
            final long had = top;
            for (int k = 0; k < evs.length(); k++) {
                JSONObject e = evs.optJSONObject(k);
                if (e == null) continue;
                long no = e.optLong("i", 0L);
                if (no <= had) continue;
                top = Math.max(top, no);
                if (CounterStore.liveTap(c, key, e.optInt("d", 1) > 0 ? 1 : -1, e.optLong("t", System.currentTimeMillis()))) n++;
            }
            seen.put(dev, top);
        }
        p.edit().putString("lvSeen", seen.toString()).commit();
        if (n > 0) {
            final Context a = c.getApplicationContext();
            new Handler(Looper.getMainLooper()).post(() -> { CounterNotifier.refreshAll(a); PbsCounterPlugin.fire(); });
        }
        return n;
    }

    /* ---------- Firestore REST ---------- */
    private static String base(Context c) {
        return "https://firestore.googleapis.com/v1/projects/" + prefs(c).getString("lvProj", "") + "/databases/(default)/documents";
    }

    private static String seg(String s) {
        try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); } catch (Exception e) { return s; }
    }

    /** 웹 화면의 칸 문서 이름과 같게: encodeURIComponent → '.'은 %2E → '%'는 '~' */
    static String docKey(String path) {
        StringBuilder b = new StringBuilder();
        byte[] bs = path.getBytes(StandardCharsets.UTF_8);
        for (byte x : bs) {
            int ch = x & 0xff;
            boolean keep = (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9') || "-_!~*'()".indexOf(ch) >= 0;
            if (keep) b.append((char) ch);
            else b.append('~').append(String.format(Locale.ROOT, "%02X", ch));
        }
        return b.toString();
    }

    private static void writeCell(Context c, String pid, String path, String value) throws Exception {
        SharedPreferences p = prefs(c);
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ROOT);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        JSONObject fields = new JSONObject();
        fields.put("path", new JSONObject().put("stringValue", path));
        fields.put("value", new JSONObject().put("stringValue", value));
        fields.put("deleted", new JSONObject().put("booleanValue", false));
        fields.put("rev", new JSONObject().put("integerValue", "0"));
        fields.put("baseRev", new JSONObject().put("integerValue", "0"));
        fields.put("by", new JSONObject().put("stringValue", p.getString("lvUid", "")));
        fields.put("byName", new JSONObject().put("stringValue", p.getString("lvName", "")));
        fields.put("at", new JSONObject().put("timestampValue", f.format(new Date())));
        String proj = p.getString("lvProj", "");
        String name = "projects/" + proj + "/databases/(default)/documents/projects/" + pid + "/cells/" + docKey(path);
        JSONObject w = new JSONObject().put("update", new JSONObject().put("name", name).put("fields", fields));
        http(c, "POST", base(c) + ":commit", new JSONObject().put("writes", new JSONArray().put(w)).toString());
    }

    /** 로그인 토큰: 만료 5분 전이면 refresh 토큰으로 새로 받는다 */
    private static synchronized String token(Context c, boolean force) throws Exception {
        SharedPreferences p = prefs(c);
        String t = p.getString("lvTok", "");
        if (!force && !t.isEmpty() && System.currentTimeMillis() < p.getLong("lvExp", 0L) - 5 * 60000L) return t;
        String body = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(p.getString("lvRt", ""), "UTF-8");
        String res = raw("POST", "https://securetoken.googleapis.com/v1/token?key=" + URLEncoder.encode(p.getString("lvKey", ""), "UTF-8"),
            body, null, "application/x-www-form-urlencoded");
        JSONObject o = new JSONObject(res);
        t = o.optString("id_token", "");
        if (t.isEmpty()) throw new Exception("로그인 토큰 없음");
        long exp = System.currentTimeMillis() + Math.max(300L, o.optLong("expires_in", 3600L)) * 1000L;
        SharedPreferences.Editor e = p.edit().putString("lvTok", t).putLong("lvExp", exp);
        String rt = o.optString("refresh_token", "");
        if (!rt.isEmpty()) e.putString("lvRt", rt);
        e.commit();
        return t;
    }

    private static String http(Context c, String method, String url, String body) throws Exception {
        try {
            return raw(method, url, body, token(c, false), "application/json");
        } catch (HttpErr e) {
            if (e.code != 401 && e.code != 403) throw e;
            return raw(method, url, body, token(c, true), "application/json"); // 토큰이 막 만료된 경우 한 번 더
        }
    }

    static class HttpErr extends Exception {
        final int code;
        HttpErr(int code, String m) { super("서버 " + code + (m.isEmpty() ? "" : " " + m)); this.code = code; }
    }

    private static String raw(String method, String url, String body, String bearer, String type) throws Exception {
        HttpURLConnection k = (HttpURLConnection) new URL(url).openConnection();
        try {
            k.setRequestMethod(method);
            k.setConnectTimeout(8000);
            k.setReadTimeout(8000);
            k.setRequestProperty("Content-Type", type);
            k.setRequestProperty("Referer", "https://localhost/");
            if (bearer != null) k.setRequestProperty("Authorization", "Bearer " + bearer);
            if (body != null) {
                k.setDoOutput(true);
                try (OutputStream os = k.getOutputStream()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            }
            int code = k.getResponseCode();
            InputStream is = code >= 400 ? k.getErrorStream() : k.getInputStream();
            String res = "";
            if (is != null) {
                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int r;
                while ((r = is.read(buf)) > 0) bo.write(buf, 0, r);
                is.close();
                res = bo.toString("UTF-8");
            }
            if (code >= 400) {
                String m = "";
                try {
                    JSONObject e = new JSONObject(res).optJSONObject("error");
                    if (e != null) m = e.optString("status", e.optString("message", ""));
                } catch (Exception ignored) {}
                throw new HttpErr(code, m);
            }
            return res;
        } finally {
            k.disconnect();
        }
    }
}
