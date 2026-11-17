package io.github.ij17838creator.pbs;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.media.VolumeProvider;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.content.ContextCompat;

/**
 * '버튼 3번' — 기기를 주머니에 넣고 화면이 꺼진 채로 음량 버튼(올림·내림 아무거나)을 3번 빠르게 누르면 측정을 시작한다.
 * DurWatch(포그라운드 서비스) 안에서 돈다. 켜 두면 측정 알림이 있는 동안 계속 돈다.
 *
 * 음량 버튼: 화면이 꺼져 있는 동안에만 '원격 음량' 미디어 세션을 켜서 받는다(화면이 켜져 있으면 음량은 평소처럼).
 *   화면 꺼짐 상태에서 음량 버튼이 앱에 오는지는 기기·안드로이드 버전마다 다르다[추정].
 *   화면 상태는 DisplayListener와 화면 켜짐/꺼짐 방송으로 받아 세션을 켜고 끄는 데만 쓴다.
 * 전원 버튼은 세지 않는다(2026-11-17 실기기: 갤럭시 탭이 빠른 전원 누름을 5초에 한 번만 화면에 반영해 셀 수 없었음).
 * 기록 대기(GRACE) 동안 기기가 잠들지 않게 잠깐 깨움 잠금(WakeLock)을 건다.
 * 진단: 최근 누름과 처리 결과를 btnLog에 남겨 앱의 '버튼 3번' 칸에서 보여 준다.
 *
 * 음악·영상(유튜브 등)이 재생 중이거나 통화·영상통화 중이면 세지 않는다. 이때 음량 세션도 끄므로 음량 버튼은 평소처럼 음량을 바꾼다.
 *
 * 3번째 누름 뒤 GRACE 동안 한 번 더 누르면 취소(짧게 2번 떨림). 아니면 기록하고 떨림으로 알린다.
 *   회기가 없으면 '회기 시작'(길게 1번). 회기가 열려 있으면 그 방법의 기본 버튼:
 *   빈도 ＋1(짧게 1번), 지속시간·지연시간 시작(길게 1번)/끝(길게 2번). 강도·간격 기록은 회기 시작만(이미 열려 있으면 짧게 3번 = 기록 안 함).
 */
final class PressWatch {
    /** 누름 사이 최대 간격, 3번 전체 최대 길이, 3번째 뒤 취소를 기다리는 시간(ms) */
    static final long GAP = 1500L, SPAN = 4000L, GRACE = 1200L;
    /** 음량 버튼을 꾹 누를 때 반복되는 신호 간격보다 짧으면 '꾹 누름'으로 보고 세지 않는다 */
    static final long VOL_REPEAT = 120L;

    static final int FB_START = 1, FB_TICK = 2, FB_END = 3, FB_CANCEL = 4, FB_NONE = 5, FB_TEST = 6;

    private final Context c;
    private final Handler h = new Handler(Looper.getMainLooper());
    private BroadcastReceiver scr;
    private android.hardware.display.DisplayManager.DisplayListener disp;
    private PowerManager.WakeLock wl;
    private AudioManager.AudioPlaybackCallback playCb;
    private MediaSession ms;
    private boolean msOn;
    private boolean vol;
    private Boolean lastScreen;
    private final long[] ts = new long[3];
    private int n;
    private long lastPress, lastVolRaw, ignoreUntil;
    private boolean pending;
    private final Runnable commit = this::commit;
    private final Runnable idle = this::onIdle;

    PressWatch(Context c) { this.c = c.getApplicationContext(); }

    /** 켜기·설정 바꾸기 (여러 번 불러도 된다) */
    void start(boolean volOn) {
        vol = volOn;
        if (scr == null) {
            scr = new BroadcastReceiver() {
                @Override public void onReceive(Context x, Intent i) {
                    String a = i.getAction();
                    final boolean on = Intent.ACTION_SCREEN_ON.equals(a);
                    if (!on && !Intent.ACTION_SCREEN_OFF.equals(a)) return;
                    // 화면 상태 신호가 놓쳤을 때만 보탠다(늦게 온 지난 방송은 지금 상태와 달라서 버린다)
                    h.postDelayed(() -> {
                        if (interactive() == on && (lastScreen == null || lastScreen != on)) screen(on, "방송");
                        else updateSession();
                    }, 700L);
                }
            };
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_SCREEN_ON);
            f.addAction(Intent.ACTION_SCREEN_OFF);
            try { ContextCompat.registerReceiver(c, scr, f, ContextCompat.RECEIVER_EXPORTED); }
            catch (Exception e) { scr = null; }
            lastScreen = interactive();
            try {
                final android.hardware.display.DisplayManager dm = (android.hardware.display.DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE);
                disp = new android.hardware.display.DisplayManager.DisplayListener() {
                    @Override public void onDisplayAdded(int id) {}
                    @Override public void onDisplayRemoved(int id) {}
                    @Override public void onDisplayChanged(int id) {
                        if (id != android.view.Display.DEFAULT_DISPLAY) return;
                        android.view.Display d = dm.getDisplay(id);
                        if (d == null) return;
                        int st = d.getState();
                        screen(st == android.view.Display.STATE_ON || st == android.view.Display.STATE_VR, "화면");
                    }
                };
                dm.registerDisplayListener(disp, h);
            } catch (Exception e) { disp = null; }
            log(c, "감시 시작" + (disp == null ? " · 화면신호 없음" : ""));
        }
        if (playCb == null && Build.VERSION.SDK_INT >= 26) { // 다른 앱이 재생을 시작·멈추면 음량 세션을 다시 정한다
            try {
                AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
                playCb = new AudioManager.AudioPlaybackCallback() {
                    @Override public void onPlaybackConfigChanged(java.util.List<android.media.AudioPlaybackConfiguration> l) {
                        h.postDelayed(PressWatch.this::onIdle, 300L);
                    }
                };
                if (am != null) am.registerAudioPlaybackCallback(playCb, h); else playCb = null;
            } catch (Exception e) { playCb = null; }
        }
        updateSession();
    }

    void stop() {
        h.removeCallbacks(commit);
        h.removeCallbacks(idle);
        pending = false;
        n = 0;
        boolean was = scr != null;
        if (scr != null) { try { c.unregisterReceiver(scr); } catch (Exception ignored) {} scr = null; }
        if (disp != null) {
            try { ((android.hardware.display.DisplayManager) c.getSystemService(Context.DISPLAY_SERVICE)).unregisterDisplayListener(disp); } catch (Exception ignored) {}
            disp = null;
        }
        unlock();
        if (was) log(c, "감시 멈춤");
        if (playCb != null && Build.VERSION.SDK_INT >= 26) {
            try { ((AudioManager) c.getSystemService(Context.AUDIO_SERVICE)).unregisterAudioPlaybackCallback(playCb); } catch (Exception ignored) {}
            playCb = null;
        }
        if (ms != null) { try { ms.setActive(false); ms.release(); } catch (Exception ignored) {} ms = null; msOn = false; }
    }

    /** 화면 상태가 바뀜 = 전원 누름 한 번 (같은 상태가 또 오면 무시) */
    private void screen(boolean on, String src) {
        lastScreen = on;
        updateSession(); // 전원 누름은 세지 않고 음량 세션만 켜고 끈다
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

    private String busyWhy() {
        try {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return "";
            if (am.isMusicActive()) return "소리 재생 중";
            int m = am.getMode();
            return m == AudioManager.MODE_NORMAL ? "" : "통화 상태(" + m + ")";
        } catch (Exception e) { return ""; }
    }

    private boolean interactive() {
        try {
            PowerManager pm = (PowerManager) c.getSystemService(Context.POWER_SERVICE);
            return pm == null || pm.isInteractive();
        } catch (Exception e) { return true; }
    }

    /** 음악·영상 재생 중이거나 통화(일반·인터넷 전화·영상통화) 중·벨 울리는 중인지 */
    private boolean mediaBusy() {
        try {
            AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return false;
            if (am.isMusicActive()) return true;
            return am.getMode() != AudioManager.MODE_NORMAL;
        } catch (Exception e) { return false; }
    }

    /** 음량 세션은 화면이 꺼져 있을 때와 누르는 도중에만 켠다(화면이 켜져 있으면 음량 버튼은 평소처럼) */
    private void updateSession() {
        boolean want = !mediaBusy() && (!interactive() || n > 0 || pending);
        if (want == msOn) return;
        try {
            if (want) {
                if (ms == null) {
                    ms = new MediaSession(c, "pbs-press");
                    ms.setPlaybackToRemote(new VolumeProvider(VolumeProvider.VOLUME_CONTROL_RELATIVE, 10, 5) {
                        @Override public void onAdjustVolume(int direction) {
                            if (direction == 0) return;
                            h.post(PressWatch.this::volPress);
                        }
                    });
                    ms.setPlaybackState(new PlaybackState.Builder().setState(PlaybackState.STATE_PLAYING, 0, 1f).build());
                }
                ms.setActive(true);
            } else if (ms != null) {
                ms.setActive(false);
            }
            msOn = want;
            log(c, want ? "화면 꺼짐 · 음량 받기 켬" : "음량 받기 끔" + (mediaBusy() ? "(재생·통화 중)" : ""));
        } catch (Exception e) {
            msOn = false;
            log(c, "음량 받기 실패: " + e.getClass().getSimpleName());
        }
    }

    private void volPress() {
        long now = SystemClock.elapsedRealtime();
        long gap = now - lastVolRaw;
        lastVolRaw = now;
        if (gap < VOL_REPEAT) { // 꾹 누르는 중: 지금까지 센 것도 버린다
            if (pending) { h.removeCallbacks(commit); pending = false; }
            n = 0;
            ignoreUntil = now + GAP;
            log(c, "음량 꾹 누름 · 무시");
            return;
        }
        press("음량");
    }

    /** 누름 한 번 (전원 = 화면 상태 바뀜, 음량 = 버튼 신호) */
    private void press(String src) {
        long now = SystemClock.elapsedRealtime();
        String why = busyWhy();
        if (!why.isEmpty()) { // 음악·영상·통화 중엔 세지 않는다
            if (pending) { pending = false; h.removeCallbacks(commit); unlock(); }
            n = 0;
            updateSession();
            log(c, src + " · " + why + "이라 안 셈");
            return;
        }
        if (n > 0 && now - lastPress > GAP) n = 0;
        lastPress = now;
        if (pending) { // 3번 뒤 한 번 더 = 취소
            pending = false;
            h.removeCallbacks(commit);
            n = 0;
            ignoreUntil = now + GAP;
            unlock();
            log(c, src + " · 4번째 = 취소");
            feedback(c, FB_CANCEL);
            scheduleIdle();
            return;
        }
        if (now < ignoreUntil) { ignoreUntil = now + GAP; scheduleIdle(); log(c, src + " · 이어진 누름 무시"); return; } // 취소 뒤 이어진 누름(5번 등)은 무시
        if (n < 3) ts[n++] = now;
        String res = n + "번째";
        if (n >= 3) {
            if (now - ts[0] <= SPAN) {
                pending = true;
                n = 0;
                lock();
                h.postDelayed(commit, GRACE);
                res = "3번째 · 기록 대기";
            } else { // 너무 느렸다: 앞의 것을 버리고 뒤 두 번만 남긴다
                ts[0] = ts[1]; ts[1] = ts[2]; n = 2;
                res = "느려서 다시 2번째";
            }
        }
        log(c, src + " · " + res);
        if (CounterStore.btnTest(c) && !pending) feedback(c, FB_TEST);
        updateSession();
        scheduleIdle();
    }

    private void scheduleIdle() {
        h.removeCallbacks(idle);
        h.postDelayed(idle, GAP + GRACE + 100L);
    }

    /** 누름이 멈췄으면 센 것을 비우고, 화면이 켜져 있으면 음량 세션을 끈다 */
    private void onIdle() {
        if (!pending && SystemClock.elapsedRealtime() - lastPress > GAP) n = 0;
        updateSession();
    }

    private void commit() {
        pending = false;
        n = 0;
        if (mediaBusy()) { updateSession(); unlock(); log(c, "재생·통화가 시작돼 기록 안 함"); return; } // 기다리는 사이 재생·통화가 시작됐으면 기록하지 않는다
        ignoreUntil = SystemClock.elapsedRealtime() + GAP;
        int fb = act(c);
        log(c, "→ " + (fb == FB_START ? "시작" : fb == FB_TICK ? "＋1" : fb == FB_END ? "끝" : "기록 안 됨" + (CounterStore.anything(c) ? "(이 방법은 시작만)" : "(측정 대상 없음)")));
        feedback(c, fb);
        unlock();
        CounterNotifier.refreshAll(c);
        PbsCounterPlugin.fire();
        updateSession();
    }

    /** 3번 눌렸을 때 할 일. 반환값: 떨림 종류 */
    static int act(Context c) {
        if (!CounterStore.anything(c)) return FB_NONE;
        if (CounterStore.begin(c)) return FB_START; // 열린 회기가 없으면(어제 것 포함) 측정 대상으로 새 회기
        if (!CounterStore.active(c)) return FB_NONE;
        switch (CounterStore.mode(c)) {
            case CounterStore.FREQ: return CounterStore.add(c, 1) > 0 ? FB_TICK : FB_NONE;
            case CounterStore.DUR: return CounterStore.durToggle(c) ? FB_START : FB_END;
            case CounterStore.LAT: return CounterStore.latToggle(c) ? FB_START : FB_END;
            default: return FB_NONE; // 강도(단계를 골라야 함)·간격 기록(신호에 답함)은 알림창에서
        }
    }

    /** 주머니 속에서도 알 수 있게 떨림으로 알린다. 떨림이 없는 기기(태블릿 등)는 짧은 소리 */
    @SuppressWarnings("deprecation")
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

    /** 진단 기록: 최근 20줄만 남긴다 (앱의 '버튼 3번' 칸에 보임) */
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
