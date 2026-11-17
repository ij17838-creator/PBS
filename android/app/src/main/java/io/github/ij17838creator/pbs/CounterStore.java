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
 *
 * 지연시간(lat): 같은 버튼으로 '지시'(k:'ls') → '반응 시작'(k:'le'). 최대 대기(latMax초)가 지나면 저절로 끝(f:'m', 반응 시각 = 지시+최대),
 *   '중단'(문제행동·중재로 그 시도를 못 잼)은 f:'x'.
 * 강도(lvl): 단계 버튼 한 번 = 행동 1건 {k:'lv', v:1~3}, 되돌리기 {k:'lu'}.
 * 간격 기록(int, 순간 표집): 회기가 열려 있는 동안 ivSec초마다(정각 맞춤이면 벽시계 기준) 신호 알림을 띄우고,
 *   교사가 '있음/없음'을 누르면 그 신호 시각으로 {k:'iv', v:'1'|'0'}. 다음 신호까지 답이 없으면 v:'?'(빈칸).
 *   응답 없는 신호가 10분 넘게 이어지면 회기를 저절로 닫는다(회기 끝을 깜빡해도 신호가 끝없이 울리지 않게).
 */
final class CounterStore {
    private static final String PREFS = "pbs_counter";
    static final String FREQ = "freq";
    static final String DUR = "dur";
    static final String LAT = "lat", LVL = "lvl", INT = "int";
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
    /** 지연시간 최대 대기(초), 기본 60 */
    static int latMax(Context c) { return Math.max(5, Math.min(600, prefs(c).getInt("latMax", 60))); }
    /** 간격 기록 신호 간격(초), 기본 120 */
    static int ivSec(Context c) { return ivSecP(prefs(c)); }
    static boolean ivAlign(Context c) { return prefs(c).getBoolean("ivAlign", true); }
    /** 간격 신호 알림 방식 (vib·sound·both·quiet) */
    static String ivMode(Context c) { return prefs(c).getString("ivMode", CHK_VIB); }
    static long ivNext(Context c) { return prefs(c).getLong("ivNext", 0L); }
    /** 답을 기다리는 신호 시각(ms), 없으면 0 */
    static long ivPend(Context c) { return prefs(c).getLong("ivPend", 0L); }
    static int ivYes(Context c) { return prefs(c).getInt("ivYes", 0); }
    static int ivTot(Context c) { return prefs(c).getInt("ivTot", 0); }
    static int lastLvl(Context c) { return prefs(c).getInt("lastLvl", 0); }

    /** 측정 시간(분, 0 = 제한 없음), 끝나면 자동 갱신할지, 갱신 때 횟수를 0으로(새 회기) 할지 */
    static int sesMin(Context c) { return Math.max(0, Math.min(240, prefs(c).getInt("sesMin", 0))); }
    static boolean sesRenew(Context c) { return prefs(c).getBoolean("sesRenew", false); }
    static boolean sesReset(Context c) { return prefs(c).getBoolean("sesReset", true); }
    /** 지금 버튼이 ABC 관찰을 다루는지 (ABC는 앱의 관찰 간격·시작/종료 시간 규칙을 따른다) */
    static boolean isAbc(Context c) {
        SharedPreferences p = prefs(c);
        if (p.getBoolean("active", false)) return p.getString("key", "").contains("|abc|");
        return "abc".equals(p.getString("tPh", ""));
    }

    /** 이번 측정 시간이 끝나는 시각(ms), 측정 시간이 없으면 0. ABC 관찰에는 쓰지 않는다 */
    static long sesEnd(Context c) {
        SharedPreferences p = prefs(c);
        if (isAbc(c)) return 0L;
        int m = sesMin(c);
        long st = p.getLong("sesStart", 0L);
        if (!p.getBoolean("active", false) || m <= 0 || st <= 0) return 0L;
        return st + m * 60000L;
    }
    static void setTimer(Context c, int min, boolean renew, boolean reset) {
        SharedPreferences p = prefs(c);
        SharedPreferences.Editor e = p.edit().putInt("sesMin", Math.max(0, Math.min(240, min))).putBoolean("sesRenew", renew).putBoolean("sesReset", reset);
        if (p.getBoolean("active", false) && p.getLong("sesStart", 0L) <= 0) e.putLong("sesStart", System.currentTimeMillis());
        e.commit();
    }

    /** 시계가 필요한지: 측정 시간 / 지속시간 끝 확인 / 지연시간 최대 대기 / 간격 기록 신호 */
    static boolean watching(Context c) {
        return sesEnd(c) > 0 || modeWatching(c);
    }

    private static boolean modeWatching(Context c) {
        if (!active(c)) return false;
        String m = mode(c);
        if (DUR.equals(m)) return running(c) && !CHK_OFF.equals(chkMode(c));
        if (LAT.equals(m)) return running(c);
        return INT.equals(m);
    }
    /** 다음에 시계가 깨어날 시각(ms): 측정 시간 끝과 방법별 확인 중 빠른 것 */
    static long nextCheck(Context c) {
        long se = sesEnd(c);
        if (!modeWatching(c)) return se;
        long m = modeNext(c);
        return se > 0 ? Math.min(se, m) : m;
    }

    private static long modeNext(Context c) {
        SharedPreferences p = prefs(c);
        String m = p.getString("mode", FREQ);
        if (LAT.equals(m)) return p.getLong("runStart", 0L) + latMax(c) * 1000L;
        if (INT.equals(m)) return p.getLong("ivNext", 0L);
        long base = Math.max(p.getLong("conf", 0L), p.getLong("runStart", 0L));
        return base + (long) (p.getInt("miss", 0) + 1) * chkSec(c) * 1000L;
    }

    /** 지금 버튼이 다룰 측정 방법 (진행 중인 회기 우선, 없으면 측정 대상) */
    static String uiMode(Context c) { return active(c) ? mode(c) : tMode(c); }

    /** 버튼을 보여 줄 게 있는지 */
    static boolean anything(Context c) { return active(c) || hasTarget(c); }

    /** 음량 3번을 켰는지 (btnVol은 쓰지 않음, 예전 값) */
    static boolean btnOn(Context c) { return prefs(c).getBoolean("btnOn", false); }
    static boolean btnVol(Context c) { return prefs(c).getBoolean("btnVol", true); }
    /** 시험용: 센 누름마다 아주 짧게 떨기 */
    static boolean btnTest(Context c) { return prefs(c).getBoolean("btnTest", false); }
    static String btnLog(Context c) { return prefs(c).getString("btnLog", ""); }
    /** 음량 3번 감시가 지금 돌아야 하는지: 켬 + 회기 열림 + 빈도·지속시간·지연시간 */
    static boolean pressOn(Context c) {
        if (!btnOn(c) || !active(c)) return false; // 배터리: 회기가 열려 있을 때만
        String m = mode(c);
        return FREQ.equals(m) || DUR.equals(m) || LAT.equals(m);
    }
    static void setBtn(Context c, boolean on, boolean vol, boolean test) {
        prefs(c).edit().putBoolean("btnOn", on).putBoolean("btnVol", vol).putBoolean("btnTest", test).commit();
    }

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
        String key = p.getString("key", "");
        String m = p.getString("mode", FREQ);
        if (p.getBoolean("running", false)) {
            if (LAT.equals(m)) flag(push(ev, now, key, "le", 0), "x"); // 반응 전에 회기를 닫으면 그 시도는 중단
            else push(ev, now, key, "de", 0);
        }
        if (INT.equals(m) && p.getLong("ivPend", 0L) > 0) pushIv(ev, p.getLong("ivPend", 0L), key, "?");
        e.putBoolean("active", false).putBoolean("running", false).putBoolean("auto", false).putLong("ivPend", 0L).putLong("ivNext", 0L);
    }

    private static JSONObject flag(JSONObject o, String f) {
        try { o.put("f", f); } catch (JSONException ignored) {}
        return o;
    }

    private static void pushIv(JSONArray ev, long t, String key, String v) {
        JSONObject o = push(ev, t, key, "iv", 0);
        try { o.put("v", v); } catch (JSONException ignored) {}
    }

    /** 간격 기록 첫 신호: 정각 맞춤이면 벽시계로 간격의 배수가 되는 다음 시각(예: 2분이면 10:02:00, 10:04:00 …) */
    static long firstSignal(long now, int sec, boolean align) {
        long per = sec * 1000L;
        if (!align) return now + per;
        long off = java.util.TimeZone.getDefault().getOffset(now);
        long local = now + off;
        return (local / per + 1) * per - off;
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
            .putLong("durMs", 0L)
            .putLong("ivPend", 0L).putInt("ivYes", 0).putInt("ivTot", 0).putInt("ivMissRun", 0).putInt("lastLvl", 0)
            .putLong("ivNext", INT.equals(p.getString("tMode", FREQ)) ? firstSignal(now, ivSecP(p), p.getBoolean("ivAlign", true)) : 0L);
        e.putLong("sesStart", now);
        push(ev, now, key, "b", 0);
        return true;
    }

    private static int ivSecP(SharedPreferences p) { return Math.max(10, Math.min(3600, p.getInt("ivSec", 120))); }

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
            .putLong("sesStart", now)
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
    static synchronized void setTarget(Context c, String pid, String ph, String title, String mode,
                                       int latMax, int ivSec, boolean ivAlign, String ivMode) {
        SharedPreferences p = prefs(c);
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        String m = DUR.equals(mode) || LAT.equals(mode) || LVL.equals(mode) || INT.equals(mode) ? mode : FREQ;
        String im = CHK_SOUND.equals(ivMode) || CHK_BOTH.equals(ivMode) || CHK_QUIET.equals(ivMode) ? ivMode : CHK_VIB;
        e.putInt("latMax", Math.max(5, Math.min(600, latMax))).putInt("ivSec", Math.max(10, Math.min(3600, ivSec)))
            .putBoolean("ivAlign", ivAlign).putString("ivMode", im);
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

    /* ---------- 지연시간 ---------- */

    /** 지연시간 시도 하나를 끝낸다. flag: null = 반응, "m" = 최대 대기 넘음, "x" = 중단(값에서 뺌) */
    private static void latEndLocked(SharedPreferences p, SharedPreferences.Editor e, JSONArray ev, long t, String flag) {
        long st = p.getLong("runStart", t);
        t = Math.max(t, st);
        JSONObject o = push(ev, t, p.getString("key", ""), "le", 0);
        if (flag != null) flag(o, flag);
        e.putBoolean("running", false).putLong("last", t);
        if (!"x".equals(flag)) e.putInt("count", p.getInt("count", 0) + 1).putLong("durMs", p.getLong("durMs", 0L) + (t - st));
    }

    /** 지연시간 '지시' / '반응 시작'(같은 버튼). 열린 회기가 없으면 새 회기를 열고 바로 지시. 반환값: 지금 반응을 기다리는지 */
    static synchronized boolean latToggle(Context c) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        if (!prepare(p, ev, now, LAT.equals(p.getString("tMode", FREQ)))) return false;
        if (!LAT.equals(p.getString("mode", FREQ))) return false;
        boolean run = p.getBoolean("running", false);
        SharedPreferences.Editor e = p.edit();
        if (run) {
            long st = p.getLong("runStart", now);
            long max = latMax(c) * 1000L;
            if (now - st >= max) latEndLocked(p, e, ev, st + max, "m");
            else latEndLocked(p, e, ev, now, null);
        } else {
            push(ev, now, p.getString("key", ""), "ls", 0);
            e.putBoolean("running", true).putLong("runStart", now);
        }
        save(e, ev);
        return !run;
    }

    /** 지연시간 '중단': 문제행동이 나와 중재하느라 이 시도를 못 잼 */
    static synchronized boolean latStop(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false) || !p.getBoolean("running", false) || !LAT.equals(p.getString("mode", FREQ))) return false;
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        latEndLocked(p, e, ev, System.currentTimeMillis(), "x");
        save(e, ev);
        return true;
    }

    /* ---------- 강도 ---------- */

    /** 단계 버튼(1~3) 한 번 = 행동 1건. 열린 회기가 없으면 새 회기를 연다 */
    static synchronized boolean lvlAdd(Context c, int lv) {
        SharedPreferences p = prefs(c);
        long now = System.currentTimeMillis();
        JSONArray ev = events(p);
        if (!prepare(p, ev, now, LVL.equals(p.getString("tMode", FREQ)))) return false;
        if (!LVL.equals(p.getString("mode", FREQ))) return false;
        JSONObject o = push(ev, now, p.getString("key", ""), "lv", 0);
        try { o.put("v", Math.max(1, Math.min(3, lv))); } catch (JSONException ignored) {}
        save(p.edit().putInt("count", p.getInt("count", 0) + 1).putLong("last", now).putInt("lastLvl", lv), ev);
        return true;
    }

    /** 강도 되돌리기: 마지막 1건 지우기 */
    static synchronized boolean lvlUndo(Context c) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false) || !LVL.equals(p.getString("mode", FREQ)) || p.getInt("count", 0) <= 0) return false;
        JSONArray ev = events(p);
        push(ev, System.currentTimeMillis(), p.getString("key", ""), "lu", 0);
        save(p.edit().putInt("count", p.getInt("count", 0) - 1).putInt("lastLvl", 0), ev);
        return true;
    }

    /* ---------- 간격 기록 (순간 표집) ---------- */

    /** 신호에 답하기. 답을 기다리는 신호가 없으면 false (신호 때가 아닌데 누른 것) */
    static synchronized boolean ivAnswer(Context c, boolean yes) {
        SharedPreferences p = prefs(c);
        if (!p.getBoolean("active", false) || !INT.equals(p.getString("mode", FREQ))) return false;
        long pend = p.getLong("ivPend", 0L);
        if (pend <= 0) return false;
        JSONArray ev = events(p);
        pushIv(ev, pend, p.getString("key", ""), yes ? "1" : "0");
        save(p.edit().putLong("ivPend", 0L).putInt("ivTot", p.getInt("ivTot", 0) + 1)
            .putInt("ivYes", p.getInt("ivYes", 0) + (yes ? 1 : 0)).putInt("ivMissRun", 0).putLong("last", System.currentTimeMillis()), ev);
        return true;
    }

    /** 간격 신호 때. 반환값: 0 = 아직 아님, 4 = 신호를 띄울 것, 5 = 응답이 오래 없어 회기를 닫음 */
    private static int ivTickLocked(Context c, SharedPreferences p, long now) {
        long nx = p.getLong("ivNext", 0L);
        if (nx <= 0 || now < nx - 1500L) return 0;
        long per = ivSec(c) * 1000L;
        String key = p.getString("key", "");
        JSONArray ev = events(p);
        int missRun = p.getInt("ivMissRun", 0);
        long pend = p.getLong("ivPend", 0L);
        if (pend > 0) { pushIv(ev, pend, key, "?"); missRun++; }
        long sig = nx;
        while (sig + per <= now + 1500L) { pushIv(ev, sig, key, "?"); missRun++; sig += per; } // 기기가 잠들어 놓친 신호
        SharedPreferences.Editor e = p.edit();
        if (missRun >= 3 && (long) missRun * per >= 10 * 60 * 1000L) {
            endLocked(p, e, ev, now);
            save(e.putInt("ivMissRun", 0), ev);
            return 5;
        }
        save(e.putLong("ivPend", sig).putLong("ivNext", sig + per).putInt("ivMissRun", missRun), ev);
        return 4;
    }

    /** 시계가 깨어났을 때. 반환값: 0 = 아직 아님·해당 없음, 1 = 끝 확인 알림을 띄울 것, 2 = 응답이 없어 자동으로 끝냄,
     *  3 = 지연시간 최대 대기 넘음(반응 없음으로 끝냄), 4 = 간격 신호, 5 = 간격 기록 응답이 오래 없어 회기를 닫음 */
    static synchronized int checkTick(Context c) {
        SharedPreferences p = prefs(c);
        if (!watching(c)) return 0;
        long now = System.currentTimeMillis();
        long se = sesEnd(c);
        if (se > 0 && now >= se - 1500L) return timerLocked(c, p, se);
        if (!modeWatching(c) || now < modeNext(c) - 1500L) return 0;
        String md = p.getString("mode", FREQ);
        if (INT.equals(md)) return ivTickLocked(c, p, now);
        if (LAT.equals(md)) {
            JSONArray ev = events(p);
            SharedPreferences.Editor e = p.edit();
            latEndLocked(p, e, ev, p.getLong("runStart", now) + latMax(c) * 1000L, "m");
            save(e, ev);
            return 3;
        }
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

    /** 측정 시간이 끝남. 반환값: 6 = 회기를 닫음, 7 = 새 회기로 갱신(횟수 0), 8 = 같은 회기에서 시간만 다시 */
    private static int timerLocked(Context c, SharedPreferences p, long se) {
        if (sesRenew(c) && !sesReset(c)) {
            p.edit().putLong("sesStart", se).commit();
            return 8;
        }
        JSONArray ev = events(p);
        SharedPreferences.Editor e = p.edit();
        boolean durRun = DUR.equals(p.getString("mode", FREQ)) && p.getBoolean("running", false);
        endLocked(p, e, ev, se);
        save(e, ev);
        if (!sesRenew(c)) return 6;
        e = p.edit();
        if (!beginLocked(p, e, ev, se)) { save(e, ev); return 6; } // 측정 대상이 없으면(알림창 ＋1 회기) 닫기만
        if (durRun) { // 진행 중이던 행동은 새 회기에서 이어서
            e.commit();
            push(ev, se, p.getString("key", ""), "ds", 0);
            e = p.edit().putBoolean("running", true).putLong("runStart", se).putLong("conf", se).putInt("miss", 0);
        }
        save(e, ev);
        return 7;
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
