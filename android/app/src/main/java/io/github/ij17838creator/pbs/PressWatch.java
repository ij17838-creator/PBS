package io.github.ij17838creator.pbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;

/**
 * '음량 버튼 기록' — 회기가 열려 있고 화면이 꺼져 있을 때 음량 버튼(올림·내림 아무거나)을 정한 횟수(1~3번, 기본 1번)만큼 빠르게 누르면
 * 그 방법의 기본 버튼을 누른 것으로 기록한다: 빈도 ＋1, 지속시간 행동 시작/끝, 지연시간 지시/반응, 간격 기록 신호 때 '있음'.
 * 강도는 정한 횟수와 상관없이 이어 누른 수(1~3번)가 단계: 마지막 누름 뒤 GAP 동안 더 누르지 않으면 기록(4번째부터는 3으로 봄).
 * 2026-11-24: '회기 시작도'(btnStart, 기본 켬)면 열린 회기가 없을 때 정한 횟수만큼 누르면 회기를 연다. (화면 켜짐에서 받기 btnScr는 실기기에서 안 돼 폐기, 늘 끔)
 *   (배터리 때문에 회기가 열려 있거나, 회기 시작도를 켠 측정 대상이 있을 때만 돈다. 화면이 꺼져 있을 때만 받는다. 다른 앱이 소리를 내면 세지 않는다.)
 *
 * 받는 방법: 갤럭시는 화면이 꺼지면 음량 키를 앱에 주지 않는다(2026-11-17 실기기 진단). 그래서 화면이 꺼져 있는 동안
 * '소리 없는 재생'(0으로 채운 AudioTrack)을 틀어 두면 음량 키가 미디어 음량을 바꾸므로, 음량 바뀜 방송(VOLUME_CHANGED_ACTION)으로
 * 누름을 세고 곧바로 원래 칸(base)으로 되돌린다. 맨 위·맨 아래 칸에선 안 바뀌니 감시 중엔 한 칸 안쪽으로 옮기고, 화면이 켜지면 원래대로.
 * 음악·영상 재생 중(다른 앱 재생이 시작되면 재생 목록 수가 늘어남)이거나 통화 중이면 세지 않고 소리 없는 재생도 멈춘다.
 * 2·3번일 때: 마지막 누름 뒤 GRACE 동안 한 번 더 누르면 취소. 1번일 때: 누를 때마다 바로 기록(취소 대기 없음, 잘못 누르면 알림창 −1). 그 사이 기기가 잠들지 않게 깨움 잠금을 건다.
 * 진단: 최근 처리 결과를 btnLog에 남겨 앱의 '음량 버튼 기록' 칸에서 보여 준다. 전원·이어폰 버튼은 쓰지 않는다.
 */
final class PressWatch {
    /** 누름 사이 최대 간격, 정한 횟수 전체 최대 길이, 마지막 누름 뒤 취소를 기다리는 시간(ms) */
    static final long GAP = 1500L, SPAN = 4000L, GRACE = 1200L;
    /** 꾹 누를 때 반복되는 신호 간격보다 짧으면 '꾹 누름'으로 보고 세지 않는다 */
    static final long VOL_REPEAT = 120L;

    static final int FB_START = 1, FB_TICK = 2, FB_END = 3, FB_CANCEL = 4, FB_NONE = 5, FB_TEST = 6;

    private final Context c;
    private final Handler h = new Handler(Looper.getMainLooper());
    private BroadcastReceiver scr, volRx;
    private android.hardware.display.DisplayManager.DisplayListener disp;
    private AudioManager.AudioPlaybackCallback playCb;
    private PowerManager.WakeLock wl;
    private AudioTrack silent;
    private int base = -1, orig = -1, cfgBase = -1;
    private boolean others, started;
    private final long[] ts = new long[3]; // 정한 횟수(1~3)까지
    private int n, lvN;
    private long lastPress, lastVolRaw, ignoreUntil;
    private boolean pending;
    private final Runnable commit = this::commit;
    private final Runnable idle = this::onIdle;
    private final Runnable recheck = this::update;

    PressWatch(Context c) { this.c = c.getApplicationContext(); }

    private AudioManager am() { return (AudioManager) c.getSystemService(Context.AUDIO_SERVICE); }

    /** 켜기 (여러 번 불러도 된다) */
    void start() {
        if (!started) {
            started = true;
            scr = new BroadcastReceiver() {
                @Override public void onReceive(Context x, Intent i) { h.postDelayed(recheck, 300L); }
            };
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_ON);
            f.addAction(Intent.ACTION_SCREEN_OFF);
            try { ContextCompat.registerReceiver(c, scr, f, ContextCompat.RECEIVER_EXPORTED); } catch (Exception e) { scr = null; }
            try {
                android.hardware.display.DisplayManager dm = (android.hardware.display.DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
                disp = new android.hardware.display.DisplayManager.DisplayListener() {
                    @Override public void onDisplayAdded(int id) {}
                    @Override public void onDisplayRemoved(int id) {}
                    @Override public void onDisplayChanged(int id) { if (id == android.view.Display.DEFAULT_DISPLAY) update(); }
                };
                dm.registerDisplayListener(disp, h);
            } catch (Exception e) { disp = null; }
            volRx = new BroadcastReceiver() {
                @Override public void onReceive(Context x, Intent i) {
                    if (silent == null || base < 0) return;
                    if (i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1) != AudioManager.STREAM_MUSIC) return;
                    int v = i.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", base);
                    if (v == base) return; // 되돌린 것
                    try { am().setStreamVolume(AudioManager.STREAM_MUSIC, base, 0); } catch (Exception ignored) {}
                    volPress();
                }
            };
            try { ContextCompat.registerReceiver(c, volRx, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"), ContextCompat.RECEIVER_EXPORTED); }
            catch (Exception e) { volRx = null; }
            if (Build.VERSION.SDK_INT >= 26) {
                try {
                    playCb = new AudioManager.AudioPlaybackCallback() {
                        @Override public void onPlaybackConfigChanged(java.util.List<android.media.AudioPlaybackConfiguration> l) {
                            int sz = l == null ? 0 : l.size();
                            if (silent != null) {
                                if (cfgBase < 0) cfgBase = sz; // 처음 바뀐 것은 내 소리 없는 재생
                                else if (sz > cfgBase) { others = true; stopSilent(); log(c, "다른 앱 재생 시작 · 음량 받기 멈춤"); }
                            }
                            h.removeCallbacks(recheck);
                            h.postDelayed(recheck, 400L);
                        }
                    };
                    am().registerAudioPlaybackCallback(playCb, h);
                } catch (Exception e) { playCb = null; }
            }
            log(c, "감시 시작(회기 열림)");
        }
        update();
    }

    void stop() {
        if (!started) return;
        started = false;
        h.removeCallbacks(commit);
        h.removeCallbacks(idle);
        h.removeCallbacks(recheck);
        pending = false;
        n = 0;
        if (scr != null) { try { c.unregisterReceiver(scr); } catch (Exception ignored) {} scr = null; }
        if (volRx != null) { try { c.unregisterReceiver(volRx); } catch (Exception ignored) {} volRx = null; }
        if (disp != null) {
            try { ((android.hardware.display.DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE)).unregisterDisplayListener(disp); } catch (Exception ignored) {}
            disp = null;
        }
        if (playCb != null && Build.VERSION.SDK_INT >= 26) { try { am().unregisterAudioPlaybackCallback(playCb); } catch (Exception ignored) {} playCb = null; }
        stopSilent();
        unlock();
        log(c, "감시 멈춤");
    }

    private boolean interactive() {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm == null || pm.isInteractive();
        } catch (Exception e) { return true; }
    }

    /** 다른 앱 음악·영상 재생 중이거나 통화 중이면 그 이유, 아니면 "" */
    private String busyWhy() {
        try {
            AudioManager am = am();
            if (silent != null ? others : am.isMusicActive()) return "소리 재생 중";
            int m = am.getMode();
            return m == AudioManager.MODE_NORMAL ? "" : "통화 상태(" + m + ")";
        } catch (Exception e) { return ""; }
    }

    /** 화면이 꺼져 있거나('화면 켜져 있어도'를 켰거나, 누르는 도중) 재생·통화 중이 아니면 소리 없는 재생을 켠다 */
    private void update() {
        if (!started) return;
        boolean scrOn = interactive();
        boolean want = (!scrOn || CounterStore.btnScr(c) || n > 0 || pending) && busyWhy().isEmpty();
        if (want == (silent != null)) return;
        if (want) {
            startSilent();
            if (silent != null) log(c, (scrOn ? "화면 켜짐" : "화면 꺼짐") + " · 음량 받기 켬");
        } else {
            String why = busyWhy();
            stopSilent();
            log(c, "음량 받기 끔" + (why.isEmpty() ? "" : "(" + why + ")"));
        }
    }

    private void startSilent() {
        if (silent != null) return;
        try {
            AudioManager am = am();
            int rate = 8000, frames = rate; // 1초짜리 0을 되풀이
            AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
            AudioFormat af = new AudioFormat.Builder().setSampleRate(rate)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build();
            AudioTrack t = new AudioTrack(aa, af, frames * 2, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
            t.write(new short[frames], 0, frames);
            t.setLoopPoints(0, frames, -1);
            cfgBase = -1;
            others = false;
            t.play();
            silent = t;
            orig = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            base = Math.max(1, Math.min(max - 1, orig));
            if (base != orig) am.setStreamVolume(AudioManager.STREAM_MUSIC, base, 0);
            h.postDelayed(() -> {
                if (silent != null && cfgBase < 0 && Build.VERSION.SDK_INT >= 26) {
                    try { cfgBase = am.getActivePlaybackConfigurations().size(); } catch (Exception ignored) {}
                }
            }, 800L);
        } catch (Exception e) {
            silent = null;
            log(c, "소리 없는 재생 실패: " + e.getClass().getSimpleName());
        }
    }

    private void stopSilent() {
        if (silent == null) return;
        try { silent.stop(); } catch (Exception ignored) {}
        try { silent.release(); } catch (Exception ignored) {}
        silent = null;
        try {
            AudioManager am = am();
            if (orig >= 0 && am.getStreamVolume(AudioManager.STREAM_MUSIC) == base) am.setStreamVolume(AudioManager.STREAM_MUSIC, orig, 0);
        } catch (Exception ignored) {}
        base = -1;
        orig = -1;
    }

    private void lock() {
        try {
            if (wl == null) {
                PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
                wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "pbs:press");
                wl.setReferenceCounted(false);
            }
            wl.acquire(GRACE + 3000L);
        } catch (Exception ignored) {}
    }

    private void unlock() {
        try { if (wl != null && wl.isHeld()) wl.release(); } catch (Exception ignored) {}
    }

    private void volPress() {
        long now = SystemClock.elapsedRealtime();
        long gap = now - lastVolRaw;
        lastVolRaw = now;
        if (gap < VOL_REPEAT) { // 꾹 누르는 중: 지금까지 센 것도 버린다
            if (pending) { h.removeCallbacks(commit); pending = false; unlock(); }
            n = 0;
            ignoreUntil = now + GAP;
            log(c, "음량 꾹 누름 · 무시");
            return;
        }
        press();
    }

    private void press() {
        long now = SystemClock.elapsedRealtime();
        String why = busyWhy();
        if (!why.isEmpty()) {
            if (pending) { pending = false; h.removeCallbacks(commit); unlock(); }
            n = 0;
            log(c, "음량 · " + why + "이라 안 셈");
            update();
            return;
        }
        if (n > 0 && now - lastPress > GAP) n = 0;
        if (CounterStore.LVL.equals(CounterStore.mode(c)) && CounterStore.active(c)) { // 강도: 이어 누른 수 = 단계
            if (!pending) lvN = 0;
            lvN = Math.min(3, lvN + 1);
            lastPress = now;
            pending = true;
            lock();
            h.removeCallbacks(commit);
            h.postDelayed(commit, GAP);
            log(c, "음량 · 강도 " + lvN + " 대기");
            if (CounterStore.btnTest(c)) feedback(c, FB_TEST);
            scheduleIdle();
            return;
        }
        lastPress = now;
        int need = CounterStore.btnCount(c);
        if (need <= 1) { // 1번: 누를 때마다 바로 기록(취소 대기 없음 = 빠르게 이어 눌러도 다 셈, 잘못 누르면 알림창 −1)
            n = 0;
            log(c, "음량 · 1번");
            commitNow();
            return;
        }
        if (pending) { // 정한 횟수 뒤 한 번 더 = 취소
            pending = false;
            h.removeCallbacks(commit);
            n = 0;
            ignoreUntil = now + GAP;
            unlock();
            log(c, "음량 · " + (need + 1) + "번째 = 취소");
            feedback(c, FB_CANCEL);
            scheduleIdle();
            return;
        }
        if (now < ignoreUntil) { ignoreUntil = now + GAP; scheduleIdle(); log(c, "음량 · 이어진 누름 무시"); return; }
        if (n < need) ts[n++] = now;
        String res = n + "번째";
        if (n >= need) {
            if (now - ts[0] <= SPAN) {
                pending = true;
                n = 0;
                lock();
                h.postDelayed(commit, GRACE);
                res = need + "번째 · 기록 대기";
            } else { // 너무 느렸다: 맨 앞 것을 버린다
                for (int k = 1; k < need; k++) ts[k - 1] = ts[k];
                n = need - 1;
                res = "느려서 다시 " + n + "번째";
            }
        }
        log(c, "음량 · " + res);
        if (CounterStore.btnTest(c) && !pending) feedback(c, FB_TEST);
        scheduleIdle();
    }

    private void scheduleIdle() {
        h.removeCallbacks(idle);
        h.postDelayed(idle, GAP + GRACE + 100L);
    }

    private void onIdle() {
        if (!pending && SystemClock.elapsedRealtime() - lastPress > GAP) n = 0;
        update();
    }

    private void commit() {
        pending = false;
        n = 0;
        ignoreUntil = SystemClock.elapsedRealtime() + GAP;
        commitNow();
    }

    private void commitNow() {
        String why = busyWhy();
        if (!why.isEmpty()) { unlock(); log(c, why + " · 기록 안 함"); update(); return; }
        int lv = lvN;
        lvN = 0;
        boolean wasOn = CounterStore.active(c);
        int fb = act(c, lv);
        log(c, "→ " + (fb == FB_START ? (wasOn ? "시작" : "회기 시작") : fb == FB_TICK ? (lv > 0 ? "강도 " + lv : CounterStore.INT.equals(CounterStore.mode(c)) ? "있음" : "＋1") : fb == FB_END ? "끝"
            : "기록 안 됨" + (CounterStore.active(c) ? "(이 방법은 앱·알림창에서)" : "(열린 회기 없음)")));
        feedback(c, fb);
        unlock();
        CounterNotifier.refreshAll(c);
        PbsCounterPlugin.fire();
        update();
    }

    static int act(Context c) { return act(c, 0); }
    static int act(Context c, int lv) {
        if (!CounterStore.active(c)) { // 열린 회기가 없으면 '회기 시작도'를 켰을 때만 회기 시작(횟수·행동은 아직 안 셈, 타일 첫 탭과 같음)
            if (!CounterStore.btnStart(c) || !CounterStore.hasTarget(c)) return FB_NONE;
            return CounterStore.begin(c) ? FB_START : FB_NONE;
        }
        switch (CounterStore.mode(c)) {
            case CounterStore.FREQ: return CounterStore.add(c, 1) > 0 ? FB_TICK : FB_NONE;
            case CounterStore.DUR: return CounterStore.durToggle(c) ? FB_START : FB_END;
            case CounterStore.LAT: return CounterStore.latToggle(c) ? FB_START : FB_END;
            case CounterStore.LVL: return lv > 0 && CounterStore.lvlAdd(c, lv) ? FB_TICK : FB_NONE;
            case CounterStore.INT: return CounterStore.ivAnswer(c, true) ? FB_TICK : FB_NONE; // 신호 때만 '있음'
            default: return FB_NONE;
        }
    }
    static void feedback(Context c, int kind) {
        long[] pat;
        switch (kind) {
            case FB_START: pat = new long[] { 0, 500 }; break;
            case FB_TICK: pat = new long[] { 0, 120 }; break;
            case FB_END: pat = new long[] { 0, 400, 200, 400 }; break;
            case FB_CANCEL: pat = new long[] { 0, 80, 120, 80 }; break;
            case FB_TEST: pat = new long[] { 0, 30 }; break;
            default: pat = new long[] { 0, 80, 120, 80, 120, 80 }; break;
        }
        try {
            Vibrator v = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= 26) {
                    AudioAttributes aa = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build();
                    v.vibrate(VibrationEffect.createWaveform(pat, -1), aa);
                } else {
                    v.vibrate(pat, -1);
                }
                return;
            }
        } catch (Exception ignored) {}
        try {
            final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50);
            int tone = kind == FB_START || kind == FB_TICK ? ToneGenerator.TONE_PROP_BEEP
                : kind == FB_END ? ToneGenerator.TONE_PROP_BEEP2 : ToneGenerator.TONE_PROP_NACK;
            tg.startTone(tone, 300);
            new Handler(Looper.getMainLooper()).postDelayed(tg::release, 600L);
        } catch (Exception ignored) {}
    }
    static synchronized void log(Context c, String msg) {
        try {
            android.content.SharedPreferences p = CounterStore.prefs(c);
            String t = new java.text.SimpleDateFormat("HH:mm:ss.S", java.util.Locale.KOREA).format(new java.util.Date());
            String old = p.getString("btnLog", "");
            String[] lines = old.isEmpty() ? new String[0] : old.split("\n");
            StringBuilder b = new StringBuilder(t).append(' ').append(msg);
            for (int k = 0; k < Math.min(lines.length, 19); k++) b.append('\n').append(lines[k]);
            p.edit().putString("btnLog", b.toString()).apply();
        } catch (Exception ignored) {}
    }
}
