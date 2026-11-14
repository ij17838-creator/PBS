package io.github.ij17838creator.pbs;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 앱 밖 버튼(알림창·홈 화면 위젯·빠른 설정 타일)이 누른 기록을 기기에 적어 두는 곳.
 * 앱 화면이 꺼져 있거나 앱이 종료돼 있어도, 재부팅한 뒤에도 여기에 남아 있다가 앱을 다시 열면 앱 자료로 옮겨진다.
 *
 * 두 가지로 쓴다.
 * 1) 회기 지정: 앱의 회기 화면에서 '알림창 ＋1 켜기'를 누르면 그 회기(key)에 바로 쌓는다.
 * 2) 측정 대상(앱 밖 측정): 학생·기초선/중재·측정 방법을 미리 정해 두면, 앱을 열지 않아도
 *    첫 버튼에서 새 회기가 시작된다(key = 프로젝트|단계|new:시작시각). 앱을 열면 그 시각으로 회기가 만들어진다.
 *
 * 기록 한 줄(events): {t:누른 시각(ms), key, d:+1/-1} 또는 {t, key, k:'b'(회기 시작)|'ds'(행동 시작)|'de'(행동 끝)}
 *   'de'에 f:'a'가 붙으면 '응답 없어 자동으로 끝냄'(끝 시각 = 마지막으로 진행 중이라고 확인된 시각).
 *   {t, key, k:'adj', s:새 시작 시각(ms)} = 늦은 시작 보정(−10초 등으로 지금 진행 중인 행동의 시작을 당김).
 *
 * 끝 확인(끝 깜빡 방지): 행동 진행 중이면 chkSec초마다 '아직 행동 중인가요?' 알림을 띄운다(DurWatch가 시간을 잰다).
 *   conf = 마지막으로 '진행 중'이 확인된 시각(행동 시작·계속·보정 버튼을 누른 때). miss = 그 뒤 응답 없이 띄운 확인 수.
 *   응답 없는 확인이 chkMiss번 쌓인 뒤 다음 확인 때가 되면 conf 시각으로 행동을 끝낸다(chkMiss 0 = 자동으로 끝내지 않음).
 */
final class CounterStore {
    private static final String PREFS = "pbs_counter";
    static final String FREQ = "freq";
    static final String DUR = "dur";
    /** 끝 확인 알림 방식 */
    static final String CHK_VIB = "vib", CHK_SOUND = "sound", CHK_BOTH = "both", CHK_QUIET = "quiet", CHK_OFF = "off";

    private CounterStore() {}

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String today(long t) {
        return new SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(new Date(t));
    }

    static String hm(long t) {
        return new SimpleDateFormat("HH:mm", Locale.KOREA).format(new Date(t));
    }

    /* ---------- 읽기 ---------- */
    static boolean active(Context c) { return prefs(c).getBoolean("active", false); }
    static String key(Context c) { return prefs(c).getString("key", ""); }
    static String title(Context c) { return prefs(c).getString("title", "PBS 기록"); }
    static int count(Context c) { return prefs(c).getInt("count", 0); }
    static long last(Context c) { return prefs(c).getLong("last", 0L); }
    static String mode(Context c) { return prefs(c).getString("mode", FREQ); }
    static boolean auto(Context c) { return prefs(c).getBoolean("auto", false); }
    static boolean running(Context c) { return prefs(c).getBoolean("running", false); }
    static long runStart(Context c) { return prefs(c).getLong("runStart", 0L); }
    /** 이번 회기에서 끝난 행동들의 시간 합(ms). 지금 진행 중인 행동은 빠져 있다 */
    static long durMs(Context c) { return prefs(c).getLong("durMs", 0L); }
    static boolean hasTarget(Context c) { return prefs(c).getBoolean("tOn", false); }
    static String tTitle(Context c) { return prefs(c).getString("tTitle", "PBS 기록"); }
    static String tMode(Context c) { return prefs(c).getString("tMode", FREQ); }
    static String tPid(Context c) { return prefs(c).getString("tPid", ""); }
    static String tPh(Context c) { return prefs(c).getString("tPh", ""); }
    static long conf(Context c) { return prefs(c).getLong("conf", 0L); }
    static int miss(Context c) { return prefs(c).getInt("miss", 0); }
    /** 끝 확인 간격(초), 기본 30 */
    static int chkSec(Context c) { return Math.max(10, prefs(c).getInt("chkSec", 30)); }
    static String chkMode(Context c) { return prefs(c).getString("chkMode", CHK_VIB); }
    /** 응답 없는 확인이 몇 번 쌓이면 자동으로 끝낼지 (0 = 끝내지 않음), 기본 3 */
    static int chkMiss(Context c) { return Math.max(0, prefs(c).getInt("chkMiss", 3)); }
    /** 늦은 시작 보정 버튼(−10초·−30초·−1분)을 보일지, 기본 켬 */
    static boolean adjOn(Context c) { return prefs(c).getBoolean("adjOn", true); }
    /** 끝 확인이 필요한지: 지속시간 행동이 진행 중이고 알림 방식이 '끄기'가 아닐 때 */
    static boolean watching(Context c) {
        return active(c) && running(c) && DUR.equals(mode(c)) && !CHK_OFF.equals(chkMode(c));
    }
    /** 다음 끝 확인 시각(ms) */
    static long nextCheck(Context c) {
        SharedPreferences p = prefs(c);
        long base = Math.max(p.getLong("conf", 0L), p.getLong("runStart", 0L));
        return base + (long) (p.getInt("miss", 0) + 1) * chkSec(c) * 1000L;
    }

    /** 지금 버튼이 다룰 측정 방법 (진행 중인 회기 우선, 없으면 측정 대상) */
    static String uiMode(Context c) { return active(c) ? mode(c) : tMode(c); }

    /** 버튼을 보여 줄 게 있는지 */
    static boolean anything(Context c) { return active(c) || hasTarget(c); }

    /* ---------- 내부 도우미 ---------- */
    private static JSONArray events(SharedPreferences p) {
        try { return new JSONArray(p.getString("events", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }

    private static JSONObject push(JSONArray ev, long t, String key, String k, int d) {
        JSONObject o = new JSONObject();
        try {
            o.put("t", t);
            o.put("key", key);
            if (k != null) o.put("k", k); else o.put("d", d);
            ev.put(o);
        } catch (JSONException ignored) {}
        return o;
    }

    /** 진행 중인 행동을 t 시각으로 끝낸다. flag가 있으면 기록에 남긴다('a' = 자동 끝) */
    private static void endRunLocked(SharedPreferences p, SharedPreferences.Editor e, JSONArray ev, long t, String flag) {
        long st = p.getLong("runStart", t);
        t = Math.max(t, st);
        JSONObject o = push(ev, t, p.getString("key", ""), "de", 0);
        if (flag != null) { try { o.put("f", flag); } catch (JSONException ignored) {} }
        e.putBoolean("running", false).putLong("durMs", p.getLong("durMs", 0L) + Math.max(0L, t - st))
            .putInt("count", p.getInt("count", 0) + 1).putLong("last", t).putLong("lastEnd", t).putInt("miss", 0);
    }

    /** 진행 중인 회기를 닫는다(행동이 진행 중이면 '행동 끝'을 남긴다) */
    private static void endLocked(SharedPreferences p, SharedPreferences.Editor e, JSONArray ev, long now) {
        if (!p.getBoolean("active", false)) return;
        if (p.getBoolean("running", false)) push(ev, now, p.getString("key", ""), "de", 0);
        e.putBoolean("active", false).putBoolean("running", false).putBoolean("auto", false);
    }

    /** 측정 대상으로 새 회기를 연다. 측정 대상이 없으면 false */
    private static boolean beginLocked(SharedPreferences p, SharedPreferences.Editor e, JSONArray ev, long now) {
        if (!p.getBoolean("tOn", false)) return false;
        String key = p.getString("tPid", "") + "|" + p.getString("tPh", "") + "|new:" + now;
        e.putBoolean("active", true)
            .putBoolean("auto", true)
            .putString("key", key)
            .putString("title", p.getString("tTitle", "PBS 기록") + " · " + hm(now) + " 시작")
            .putString("mode", p.getString("tMode", FREQ))
            .putString("day", today(now))
            .putInt("count", 0)
            .putLong("last", 0L)
            .putBoolean("running", false)
            .putLong("runStart", 0L)
            .putLong("lastEnd", 0L)
            .putLong("durMs", 0L);
        push(ev, now, key, "b", 0);
        return true;
    }

    /** 자동으로 연 회기가 어제 것이면 닫는다(다음 버튼부터 새 회기) */
    private static boolean staleLocked(SharedPreferences p, long now) {
        return p.getBoolean("active", false) && p.getBoolean("auto", false)
            && !today(now).equals(p.getString("day", ""));
    }

    /* ---------- 앱에서 부르는 것 ---------- */

    /** 앱의 회기 화면에서 '알림창 ＋1 켜기' (특정 회기에 바로 쌓기, 빈도만) */
    static synchronized void start(Context c, String key, String title, int count) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        long now = System.currentTimeMillis();
        SharedPreferences.Editor e = p.edit();
        endLocked(p, e, ev, now);
        e.putBoolean("active", true)
            .putBoolean("auto", false)
            .putString("key", key)
            .putString("title", title)
            .putString("mode", FREQ)
            .putInt("count", Math.max(0, count))
            .putLong("last", 0L)
            .putBoolean("running", false)
            .putLong("durMs", 0L)
            .putString("events", ev.toString())
            .commit();
    }

    /** 회기 끝 (앱의 '알림창 버튼 끄기'와 알림창·위젯의 '회기 끝') */
    static synchronized void stop(Context c) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        endLocked(p, e, ev, System.currentTimeMillis());
        e.putString("events", ev.toString()).commit();
    }

    static synchronized void setCount(Context c, String key, int count) {
        SharedPreferences p = prefs(c);
        if (!key.equals(p.getString("key", ""))) return;
        p.edit().putInt("count", Math.max(0, count)).apply();
    }

    /** 앱 밖 측정 대상 정하기. 다른 대상의 자동 회기가 열려 있으면 닫는다 */
    static synchronized void setTarget(Context c, String pid, String ph, String title, String mode) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        String m = DUR.equals(mode) ? DUR : FREQ;
        boolean same = pid.equals(p.getString("tPid", "")) && ph.equals(p.getString("tPh", "")) && m.equals(p.getString("tMode", ""));
        if (!same && p.getBoolean("auto", false)) endLocked(p, e, ev, System.currentTimeMillis());
        e.putBoolean("tOn", true).putString("tPid", pid).putString("tPh", ph).putString("tTitle", title).putString("tMode", m)
            .putString("events", ev.toString()).commit();
    }

    /** 앱 밖 측정 끄기. 자동으로 연 회기도 닫는다 */
    static synchronized void clearTarget(Context c) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        if (p.getBoolean("auto", false)) endLocked(p, e, ev, System.currentTimeMillis());
        e.putBoolean("tOn", false).putString("events", ev.toString()).commit();
    }

    /* ---------- 버튼에서 부르는 것 ---------- */

    private static void save(SharedPreferences.Editor e, JSONArray ev) {
        e.putString("events", ev.toString()).commit();
    }

    /** 어제 열린 자동 회기는 닫고, 열린 회기가 없으면 (open이 참일 때) 측정 대상으로 새 회기를 연다. 반환값: 지금 열린 회기가 있는지 */
    private static boolean prepare(SharedPreferences p, JSONArray ev, long now, boolean open) {
        if (staleLocked(p, now)) {
            SharedPreferences.Editor e = p.edit();
            endLocked(p, e, ev, now);
            save(e, ev);
        }
        if (p.getBoolean("active", false)) return true;
        if (!open) return false;
        SharedPreferences.Editor e = p.edit();
        if (!beginLocked(p, e, ev, now)) return false;
        save(e, ev);
        return true;
    }

    /** '회기 시작': 측정 대상으로 새 회기만 연다(아직 행동은 없음 = 0회도 기록됨) */
    static synchronized boolean begin(Context c) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        if (p.getBoolean("active", false) && !staleLocked(p, now)) return false;
        return prepare(p, ev, now, true);
    }

    /** 빈도 버튼 한 번 = 기록 한 줄. d는 +1 또는 -1. 열린 회기가 없으면 +1에서 새 회기를 연다. 반환값: 바뀐 뒤 횟수 */
    static synchronized int add(Context c, int d) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        boolean canOpen = d > 0 && FREQ.equals(p.getString("tMode", FREQ));
        if (!prepare(p, ev, now, canOpen)) return 0;
        if (!FREQ.equals(p.getString("mode", FREQ))) return p.getInt("count", 0);
        int count = p.getInt("count", 0);
        if (d < 0 && count <= 0) return 0;
        count = Math.max(0, count + d);
        push(ev, now, p.getString("key", ""), null, d);
        SharedPreferences.Editor e = p.edit().putInt("count", count);
        if (d > 0) e.putLong("last", now);
        save(e, ev);
        return count;
    }

    /** 지속시간 '행동 시작/행동 끝'. 열린 회기가 없으면 새 회기를 열고 바로 시작한다. 반환값: 지금 진행 중인지 */
    static synchronized boolean durToggle(Context c) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        boolean canOpen = DUR.equals(p.getString("tMode", FREQ));
        if (!prepare(p, ev, now, canOpen)) return false;
        if (!DUR.equals(p.getString("mode", FREQ))) return false;
        String key = p.getString("key", "");
        boolean run = p.getBoolean("running", false);
        SharedPreferences.Editor e = p.edit();
        if (run) {
            endRunLocked(p, e, ev, now, null);
        } else {
            push(ev, now, key, "ds", 0);
            e.putBoolean("running", true).putLong("runStart", now).putLong("conf", now).putInt("miss", 0);
        }
        save(e, ev);
        return !run;
    }

    /** '행동 끝'만 (끝 확인 알림의 버튼: 이미 끝났으면 아무것도 안 함 = 실수로 새 행동이 시작되지 않게) */
    static synchronized boolean durEnd(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false) || !p.getBoolean("running", false)) return false;
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        endRunLocked(p, e, ev, System.currentTimeMillis(), null);
        save(e, ev);
        return true;
    }

    /** 끝 확인 '계속': 지금도 진행 중이라고 확인 */
    static synchronized boolean confirm(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("running", false)) return false;
        p.edit().putLong("conf", System.currentTimeMillis()).putInt("miss", 0).commit();
        return true;
    }

    /** 늦은 시작 보정: 진행 중인 행동의 시작을 sec초 앞당긴다(앞 행동의 끝보다 앞으로는 못 감). 반환값: 실제로 당긴 초 */
    static synchronized long adjust(Context c, int sec) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false) || !p.getBoolean("running", false)) return 0;
        long now = System.currentTimeMillis();
        long st = p.getLong("runStart", now);
        long ns = Math.max(p.getLong("lastEnd", 0L), st - sec * 1000L);
        if (ns >= st) return 0;
        JSONArray ev = events(p);
        JSONObject o = push(ev, now, p.getString("key", ""), "adj", 0);
        try { o.put("s", ns); } catch (JSONException ignored) {}
        SharedPreferences.Editor e = p.edit().putLong("runStart", ns).putLong("conf", now).putInt("miss", 0);
        save(e, ev);
        return (st - ns) / 1000L;
    }

    /** 끝 확인 때가 됐을 때. 반환값: 0 = 아직 아님·해당 없음, 1 = 확인 알림을 띄울 것, 2 = 응답이 없어 자동으로 끝냄 */
    static synchronized int checkTick(Context c) {
        SharedPreferences p = prefs(c);
        if (!watching(c)) return 0;
        long now = System.currentTimeMillis();
        if (now < nextCheck(c) - 1500L) return 0;
        int miss = p.getInt("miss", 0);
        int lim = chkMiss(c);
        if (lim > 0 && miss >= lim) {
            JSONArray ev = events(p);
            SharedPreferences.Editor e = p.edit();
            endRunLocked(p, e, ev, Math.max(p.getLong("conf", 0L), p.getLong("runStart", 0L)), "a");
            save(e, ev);
            return 2;
        }
        p.edit().putInt("miss", miss + 1).commit();
        return 1;
    }

    /** 끝 확인 설정 (앱의 '끝 확인 설정') */
    static synchronized void setCheck(Context c, int sec, String mode, int miss, boolean adj) {
        String m = CHK_SOUND.equals(mode) || CHK_BOTH.equals(mode) || CHK_QUIET.equals(mode) || CHK_OFF.equals(mode) ? mode : CHK_VIB;
        prefs(c).edit().putInt("chkSec", Math.max(10, Math.min(600, sec))).putString("chkMode", m)
            .putInt("chkMiss", Math.max(0, Math.min(20, miss))).putBoolean("adjOn", adj).putInt("miss", 0)
            .putLong("conf", System.currentTimeMillis()).commit();
    }

    /** 쌓인 기록을 모두 꺼내고 비운다 */
    static synchronized JSONArray drain(Context c) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        p.edit().putString("events", "[]").commit();
        return ev;
    }
}
